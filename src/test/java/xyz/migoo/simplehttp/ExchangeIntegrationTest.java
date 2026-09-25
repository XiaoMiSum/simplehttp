package xyz.migoo.simplehttp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * 真实链路集成测试：本地起 HTTP 服务端，验证「完整且真实的 HTTP 请求响应信息」是否可获取。
 * <p>
 * 覆盖：连接池复用、无实体响应（HEAD）、gzip 原始/解码双视图、重定向链与最终 URI、
 * 线上请求头与请求体还原、大响应与耗时口径、失败上下文、字符集、chunked、
 * 逐请求代理、关闭后执行、监听器、query 不回写、落盘与采集上限、流式请求体。
 *
 * @author xiaomi
 */
public class ExchangeIntegrationTest {

    private static final String PLAIN_TEXT = "0123456789012345678901234567890123456789";
    private static final String GBK_TEXT = "中文编码测试";
    private static final byte[] BIG_BODY = new byte[2 * 1024 * 1024];

    static {
        for (int i = 0; i < BIG_BODY.length; i++) {
            BIG_BODY[i] = (byte) (i % 251);
        }
    }

    private HttpServer server;
    private ExecutorService executor;
    private SimpleHttp client;
    private String baseUrl;

    /**
     * 服务端收到的最后一个请求（用于与采集到的线上请求做 diff）
     */
    private volatile Map<String, List<String>> lastRequestHeaders;
    private volatile byte[] lastRequestBody;

    private final List<Exchange> completed = new CopyOnWriteArrayList<>();
    private final List<Exchange> failed = new CopyOnWriteArrayList<>();

    @BeforeClass
    public void setUp() throws IOException {
        executor = Executors.newFixedThreadPool(8);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(executor);

        server.createContext("/hello", exchange -> {
            var body = "{\"msg\":\"hi\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().add("X-Server", "demo");
            record(exchange, body);
            respond(exchange, 200, body);
        });
        server.createContext("/echo", exchange -> {
            var body = exchange.getRequestBody().readAllBytes();
            lastRequestBody = body;
            lastRequestHeaders = Map.copyOf(exchange.getRequestHeaders());
            respond(exchange, 200, "ok".getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/no-content", exchange -> {
            record(exchange, new byte[0]);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/utf8", exchange -> {
            var body = "中文测试".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            record(exchange, body);
            respond(exchange, 200, body);
        });
        server.createContext("/gbk", exchange -> {
            var body = GBK_TEXT.getBytes(Charset.forName("GBK"));
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=GBK");
            record(exchange, body);
            respond(exchange, 200, body);
        });
        server.createContext("/gzip", exchange -> {
            var buffer = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(buffer)) {
                gzip.write(PLAIN_TEXT.getBytes(StandardCharsets.UTF_8));
            }
            var body = buffer.toByteArray();
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            record(exchange, body);
            respond(exchange, 200, body);
        });
        server.createContext("/redirect1", exchange -> {
            exchange.getResponseHeaders().add("Location", "/redirect2");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirect2", exchange -> {
            exchange.getResponseHeaders().add("Location", "/hello");
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });
        server.createContext("/big", exchange -> {
            record(exchange, BIG_BODY);
            respond(exchange, 200, BIG_BODY);
        });
        server.createContext("/chunked", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            try (var os = exchange.getResponseBody()) {
                for (int i = 0; i < 5; i++) {
                    os.write(("chunk-" + i + ";").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }
        });
        server.createContext("/error", exchange -> {
            record(exchange, new byte[0]);
            respond(exchange, 500, "boom".getBytes(StandardCharsets.UTF_8));
        });
        server.start();

        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = SimpleHttp.builder()
                .exchangeListener(new ExchangeListener() {
                    @Override
                    public void onComplete(Exchange exchange) {
                        completed.add(exchange);
                    }

                    @Override
                    public void onFailure(Exchange exchange, Throwable cause) {
                        failed.add(exchange);
                    }
                })
                .build();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    // ------------------------------------------------------------------ T1 可用性

    /**
     * 同一客户端可连续、并发复用（连接池不会被单次请求关闭）
     */
    @Test
    public void testSequentialAndConcurrentReuse() throws Exception {
        for (int i = 0; i < 100; i++) {
            var response = client.execute(Request.get(baseUrl + "/hello"));
            Assert.assertEquals(response.statusCode(), 200, "第 " + i + " 次请求失败");
        }
        var pool = client.connectionManager().getTotalStats();
        Assert.assertTrue(pool.getMax() > 0, "连接池应存活（未被单次请求关闭）");

        var threads = Executors.newFixedThreadPool(8);
        var errors = new CopyOnWriteArrayList<Throwable>();
        try {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 80; i++) {
                futures.add(threads.submit(() -> {
                    try {
                        var response = client.execute(Request.get(baseUrl + "/hello"));
                        if (response.statusCode() != 200) {
                            throw new IllegalStateException("status=" + response.statusCode());
                        }
                    } catch (Throwable t) {
                        errors.add(t);
                    }
                }));
            }
            for (var future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            threads.shutdownNow();
        }
        Assert.assertTrue(errors.isEmpty(), "并发请求不应失败：" + errors);
    }

    /**
     * 默认单例客户端也可连续执行
     */
    @Test
    public void testDefaultClientReusable() throws Exception {
        Assert.assertEquals(Request.get(baseUrl + "/hello").execute().statusCode(), 200);
        Assert.assertEquals(Request.get(baseUrl + "/hello").execute().statusCode(), 200);
    }

    // ------------------------------------------------------------------ T2 无实体响应

    /**
     * HEAD 等无实体响应不再抛 NPE
     */
    @Test
    public void testHeadWithoutEntity() throws Exception {
        var response = client.execute(Request.head(baseUrl + "/no-content"));
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertNotNull(response.bytes());
        Assert.assertEquals(response.bytes().length, 0);
        Assert.assertEquals(response.rawBytes().length, 0);
    }

    // ------------------------------------------------------------------ T3 gzip 真实性

    /**
     * gzip：线上原始头与原始字节保持原样，同时提供解压后的结果视图
     */
    @Test
    public void testGzipRawAndDecodedViews() throws Exception {
        var response = client.execute(Request.get(baseUrl + "/gzip"));

        Assert.assertNull(response.header("Content-Encoding"), "解压后的结果视图不应再有 Content-Encoding");
        Assert.assertEquals(headerValue(response.rawHeaders(), "Content-Encoding"), "gzip");
        var rawContentLength = headerValue(response.rawHeaders(), "Content-Length");
        Assert.assertNotNull(rawContentLength, "线上原样响应头必须保留 Content-Length");
        Assert.assertEquals(response.contentLength(), Long.parseLong(rawContentLength));
        Assert.assertEquals(response.rawBytes().length, Long.parseLong(rawContentLength),
                "线上原始字节长度应等于线上 Content-Length");

        Assert.assertEquals(response.text(), PLAIN_TEXT, "解码视图应为解压后文本");
        Assert.assertEquals(response.bytes().length, PLAIN_TEXT.length());
        Assert.assertNotEquals(response.rawBytes().length, response.bytes().length, "原始字节应为压缩态");
    }

    // ------------------------------------------------------------------ T4 重定向链

    /**
     * 重定向：最终 URI、重定向链、逐跳状态码均可获取
     */
    @Test
    public void testRedirectChainAndFinalUri() throws Exception {
        var request = Request.get(baseUrl + "/redirect1");
        var response = client.execute(request);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.exchange().finalUri().toString().endsWith("/hello"), "最终 URI 应为 /hello");
        Assert.assertEquals(response.exchange().redirects().size(), 2, "应记录 2 跳重定向");
        Assert.assertEquals(response.exchange().attempts().size(), 3, "应记录 3 跳（2 次 3xx + 1 次 200）");

        var attempts = response.exchange().attempts();
        Assert.assertEquals(attempts.get(0).statusCode(), 302);
        Assert.assertEquals(attempts.get(0).location(), "/redirect2");
        Assert.assertEquals(attempts.get(1).statusCode(), 301);
        Assert.assertEquals(attempts.get(1).location(), "/hello");
        Assert.assertEquals(attempts.get(2).statusCode(), 200);

        Assert.assertEquals(request.uri(), baseUrl + "/redirect1", "原始 Request 的 URI 不应被回写");
    }

    // ------------------------------------------------------------------ T5 请求头真实性

    /**
     * 线上真实请求头 = 服务端实际收到的请求头（含客户端自动补全的头）
     */
    @Test
    public void testWireRequestHeadersMatchServerReceived() throws Exception {
        var request = Request.post(baseUrl + "/echo")
                .addHeader("X-Custom", "custom-value")
                .body(RequestEntity.json("{\"k\":1}"));
        var response = client.execute(request);

        Assert.assertEquals(response.statusCode(), 200);
        var attempt = response.exchange().lastAttempt();
        var wireHeaders = attempt.wireHeaders();
        Assert.assertTrue(wireHeaders.length > 1, "应采集到完整请求头");

        // 客户端自动补全的头必须在采集结果里
        Assert.assertNotNull(headerValue(wireHeaders, "Host"));
        Assert.assertNotNull(headerValue(wireHeaders, "Content-Length"));
        Assert.assertNotNull(headerValue(wireHeaders, "User-Agent"));
        Assert.assertNotNull(headerValue(wireHeaders, "Accept-Encoding"));
        Assert.assertEquals(headerValue(wireHeaders, "X-Custom"), "custom-value");

        // 与服务端实际收到的逐项比对
        for (var header : wireHeaders) {
            var name = header.getName();
            if (name.equalsIgnoreCase("Connection") || name.equalsIgnoreCase("Proxy-Connection")) {
                // 中间可能被服务端改写，不参与比对
                continue;
            }
            var received = serverValues(name);
            Assert.assertNotNull(received, "服务端应收到请求头 " + name);
            Assert.assertTrue(received.contains(header.getValue()),
                    name + " 应一致：采集=" + header.getValue() + " 服务端=" + received);
        }
        Assert.assertEquals(headerValue(wireHeaders, "Host"), firstOf(serverValues("Host")));
        Assert.assertEquals(new String(attempt.wireBody(), StandardCharsets.UTF_8), "{\"k\":1}");
        Assert.assertEquals(attempt.requestBodyState(), Attempt.BodyState.CAPTURED);
    }

    // ------------------------------------------------------------------ T6 multipart 真实体

    /**
     * multipart 上传：采集到的请求体 = 服务端实际收到的字节
     */
    @Test
    public void testMultipartWireBodyMatchesServer() throws Exception {
        var file = Files.createTempFile("simplehttp-upload", ".txt");
        Files.writeString(file, "file-content-here");
        try {
            var response = client.execute(Request.post(baseUrl + "/echo")
                    .body(RequestEntity.binary(
                            List.of(new org.apache.hc.core5.http.message.BasicNameValuePair("file", file.toString())),
                            Map.of("field", "value"))));

            Assert.assertEquals(response.statusCode(), 200);
            var attempt = response.exchange().lastAttempt();
            Assert.assertNotNull(attempt.wireBody(), "应采集到线上真实请求体");
            Assert.assertEquals(attempt.wireBody(), lastRequestBody,
                    "采集到的请求体必须与服务端收到的字节完全一致");
            Assert.assertTrue(headerValue(attempt.wireHeaders(), "Content-Type").startsWith("multipart/form-data"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    // ------------------------------------------------------------------ T7 大响应与耗时

    /**
     * 大响应：字节数一致，耗时口径正确（总耗时 >= TTFB >= 0，body 耗时 >= 0）
     */
    @Test
    public void testLargeResponseAndTimings() throws Exception {
        var response = client.execute(Request.get(baseUrl + "/big"));

        Assert.assertEquals(response.bytes().length, BIG_BODY.length);
        Assert.assertEquals(response.exchange().lastAttempt().rawBodySize(), BIG_BODY.length);
        Assert.assertEquals(response.rawBytes().length, BIG_BODY.length);

        var timings = response.exchange().timings();
        Assert.assertNotNull(timings);
        Assert.assertTrue(timings.ttfbMillis() >= 0, "TTFB 应已采集");
        Assert.assertTrue(timings.bodyMillis() >= 0, "响应体耗时应已采集");
        Assert.assertTrue(timings.totalMillis() >= timings.bodyMillis(), "总耗时应包含响应体下载");
        Assert.assertTrue(timings.totalMillis() >= timings.ttfbMillis(), "总耗时应不小于 TTFB");
        Assert.assertTrue(response.duration() >= timings.ttfbMillis(), "duration() 应为完整耗时");
    }

    /**
     * chunked 响应：线上无 Content-Length 时如实返回 -1
     */
    @Test
    public void testChunkedResponseWithoutContentLength() throws Exception {
        var response = client.execute(Request.get(baseUrl + "/chunked"));
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.contentLength(), -1);
        Assert.assertTrue(response.text().startsWith("chunk-0;"));
    }

    // ------------------------------------------------------------------ T8 失败上下文

    /**
     * 连接失败：异常携带已采集的交换记录（原始请求快照、失败阶段、耗时）与监听器回调
     */
    @Test
    public void testFailureCarriesExchange() throws Exception {
        int deadPort;
        try (var socket = new ServerSocket(0)) {
            deadPort = socket.getLocalPort();
        }
        var before = failed.size();
        var request = Request.get("http://127.0.0.1:" + deadPort + "/nothing");

        Exception thrown = null;
        try {
            client.execute(request);
        } catch (Exception e) {
            thrown = e;
        }

        Assert.assertNotNull(thrown, "连接被拒绝应抛异常");
        Assert.assertTrue(thrown instanceof HttpExecutionException, "应包装为 HttpExecutionException，实际为 " + thrown);
        var exception = (HttpExecutionException) thrown;
        Assert.assertNotNull(exception.exchange(), "异常必须携带交换记录");
        Assert.assertTrue(exception.exchange().failure() != null);
        Assert.assertEquals(exception.exchange().stage(), Exchange.Stage.CONNECT);
        Assert.assertEquals(exception.exchange().request().method(), "GET");
        Assert.assertTrue(exception.exchange().request().uri().toString().contains("/nothing"));
        Assert.assertTrue(exception.exchange().timings().totalMillis() >= 0);
        Assert.assertNotNull(exception.getCause(), "底层原因应保留");

        Assert.assertEquals(failed.size(), before + 1, "失败监听器应被回调");
        Assert.assertSame(request.exchange(), exception.exchange(), "Request 应可拿到失败记录");
    }

    // ------------------------------------------------------------------ T10 字符集

    /**
     * text() 按 Content-Type 字符集解码，不再使用平台默认字符集
     */
    @Test
    public void testTextDecodesByContentTypeCharset() throws Exception {
        var utf8 = client.execute(Request.get(baseUrl + "/utf8"));
        Assert.assertEquals(utf8.charset(), StandardCharsets.UTF_8);
        Assert.assertEquals(utf8.text(), "中文测试");
        Assert.assertEquals(utf8.text(), "中文测试");

        var gbk = client.execute(Request.get(baseUrl + "/gbk"));
        Assert.assertEquals(gbk.charset(), Charset.forName("GBK"));
        Assert.assertEquals(gbk.text(), GBK_TEXT);
    }

    /**
     * readme 中的便捷 API 可用
     */
    @Test
    public void testReadmeStyleApi() throws Exception {
        var response = client.execute(Request.get(baseUrl + "/hello"));
        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.header("Content-Type").contains("application/json"));
        Assert.assertTrue(response.text().contains("hi"));
        Assert.assertTrue(response.headers("Content-Type").size() == 1);

        var error = client.execute(Request.get(baseUrl + "/error"));
        Assert.assertFalse(error.isSuccessful());
        Assert.assertEquals(error.statusCode(), 500);
    }

    // ------------------------------------------------------------------ T9 代理

    /**
     * 逐请求代理：同一客户端可指定代理，且路由信息被记录
     */
    @Test
    public void testPerRequestProxyRouting() throws Exception {
        try (var proxy = new TinyProxy()) {
            var response = client.execute(Request.get(baseUrl + "/hello")
                    .proxy("127.0.0.1", proxy.port()));

            Assert.assertEquals(response.statusCode(), 200);
            Assert.assertNotNull(response.exchange().proxy(), "应记录实际使用的代理");
            Assert.assertEquals(response.exchange().proxy().getHostName(), "127.0.0.1");
            Assert.assertEquals(response.exchange().proxy().getPort(), proxy.port());
            Assert.assertTrue(proxy.lastRequestLine.startsWith("GET " + baseUrl + "/hello"),
                    "代理应收到绝对形式的请求行，实际为 " + proxy.lastRequestLine);

            // 同一客户端不带代理再请求一次，验证代理是逐请求生效的
            var direct = client.execute(Request.get(baseUrl + "/hello"));
            Assert.assertEquals(direct.statusCode(), 200);
            Assert.assertNull(direct.exchange().proxy(), "未指定代理时不应记录代理");
        }
    }

    // ------------------------------------------------------------------ T12 生命周期

    /**
     * 关闭后再执行：明确失败
     */
    @Test
    public void testExecuteAfterCloseFails() throws Exception {
        var closed = SimpleHttp.builder().build();
        Assert.assertEquals(closed.execute(Request.get(baseUrl + "/hello")).statusCode(), 200);
        closed.close();
        closed.close(); // 幂等

        Exception thrown = null;
        try {
            closed.execute(Request.get(baseUrl + "/hello"));
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNotNull(thrown, "关闭后应抛异常");
        Assert.assertTrue(chainContains(thrown, IllegalStateException.class),
                "原因链应包含 IllegalStateException，实际为 " + thrown);
    }

    // ------------------------------------------------------------------ query 不回写

    /**
     * query 参数合入线上 URI，但不回写用户对象；最终 URI 从 exchange 获取
     */
    @Test
    public void testQueryNotWrittenBackToRequest() throws Exception {
        var request = Request.get(baseUrl + "/echo")
                .query(Form.create().add("page", "1").add("size", "10"));
        var response = client.execute(request);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(request.uri(), baseUrl + "/echo", "原始 URI 不应被回写");
        var finalUri = response.exchange().finalUri().toString();
        Assert.assertTrue(finalUri.contains("page=1") && finalUri.contains("size=10"),
                "线上最终 URI 应包含 query：" + finalUri);
        Assert.assertNotNull(serverValues("Host"));
    }

    // ------------------------------------------------------------------ 采集上限与落盘

    /**
     * 采集上限：超出即截断并如实标记，同时保留真实大小
     */
    @Test
    public void testMaxCaptureBytesTruncates() throws Exception {
        var bounded = SimpleHttp.builder().maxCaptureBytes(16).build();
        try {
            var response = bounded.execute(Request.get(baseUrl + "/gzip"));
            var attempt = response.exchange().lastAttempt();
            Assert.assertEquals(response.rawBytes().length, 16, "原始响应体应被截断到上限");
            Assert.assertTrue(attempt.rawBodyTruncated());
            Assert.assertEquals(attempt.rawBodySize(), Long.parseLong(headerValue(response.rawHeaders(), "Content-Length")),
                    "真实大小应如实记录");
            Assert.assertEquals(response.text(), PLAIN_TEXT, "截断不影响结果视图");
        } finally {
            bounded.close();
        }
    }

    /**
     * 落盘：原始响应体写入文件并释放内存
     */
    @Test
    public void testCaptureToFile() throws Exception {
        var directory = Files.createTempDirectory("simplehttp-capture");
        var fileClient = SimpleHttp.builder().captureToFile(directory).build();
        try {
            var response = fileClient.execute(Request.get(baseUrl + "/gzip"));
            var attempt = response.exchange().lastAttempt();

            Assert.assertNotNull(attempt.rawBodyFile(), "原始响应体应落盘");
            Assert.assertTrue(Files.exists(attempt.rawBodyFile()));
            Assert.assertEquals(Files.readAllBytes(attempt.rawBodyFile()), response.rawBytes().length == 0
                    ? readGzipRaw() : response.rawBytes());
            Assert.assertEquals(response.rawBytes().length, 0, "落盘后内存应释放");
            Assert.assertEquals(attempt.rawBodySize(), Long.parseLong(headerValue(response.rawHeaders(), "Content-Length")));
            Assert.assertEquals(response.text(), PLAIN_TEXT);
        } finally {
            fileClient.close();
            try (var paths = Files.walk(directory)) {
                for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    /**
     * 关闭采集：不安装采集链路，结果视图与耗时/失败阶段仍可用，线上原始报文视图为空
     */
    @Test
    public void testCaptureDisabled() throws Exception {
        var plain = SimpleHttp.builder().captureEnabled(false).build();
        try {
            var response = plain.execute(Request.get(baseUrl + "/echo")
                    .query(Form.create().add("page", "1")));

            Assert.assertEquals(response.statusCode(), 200);
            Assert.assertEquals(response.text(), "ok", "结果视图不受关闭采集影响");
            Assert.assertNotNull(response.exchange());

            var exchange = response.exchange();
            Assert.assertTrue(exchange.attempts().isEmpty(), "关闭采集后不应产生跳记录");
            Assert.assertEquals(response.rawHeaders().length, 0, "线上原样响应头应为空数组");
            Assert.assertEquals(response.rawBytes().length, 0, "线上原样响应体应为空数组");
            Assert.assertTrue(exchange.isSuccess());
            Assert.assertNotNull(exchange.timings());
            Assert.assertTrue(exchange.timings().totalMillis() >= 0, "耗时仍应记录");

            // 即使关闭采集，最终 URI 也应是实际发送的（含派生出的 query）
            Assert.assertTrue(exchange.finalUri().toString().contains("page=1"),
                    "finalUri 应含 query：" + exchange.finalUri());
        } finally {
            plain.close();
        }
    }

    /**
     * readme 中的链路与真实视图 API 全部可用
     */
    @Test
    public void testReadmeChainApis() throws Exception {
        var response = client.execute(Request.get(baseUrl + "/gzip"));

        // 真实视图
        Assert.assertTrue(response.contentLength() > 0);
        Assert.assertEquals(response.contentEncoding(), "gzip");
        Assert.assertEquals(response.rawBytes().length, response.contentLength());

        // 链路视图
        Assert.assertNotNull(response.exchange().target());
        Assert.assertEquals(response.exchange().target().getHostName(), "127.0.0.1");
        Assert.assertEquals(response.exchange().target().getPort(), server.getAddress().getPort());
        Assert.assertNull(response.exchange().proxy());
        Assert.assertNotNull(response.exchange().id());
        Assert.assertTrue(response.exchange().isSuccess());
        Assert.assertNotNull(response.message());

        var attempt = response.exchange().attempts().get(0);
        Assert.assertEquals(attempt.index(), 0);
        Assert.assertEquals(attempt.method(), "GET");
        Assert.assertEquals(attempt.httpVersion(), "HTTP/1.1");
        Assert.assertNull(attempt.location());
        Assert.assertTrue(attempt.wireHeader("Host").contains("127.0.0.1"));
        Assert.assertNotNull(headerValue(attempt.rawHeaders(), "Content-Type"));
        Assert.assertTrue(attempt.ttfbMillis() >= 0);

        var dump = response.exchange().toWireString();
        Assert.assertTrue(dump.contains("attempt #1"));
        Assert.assertTrue(dump.contains("< body: "));
    }

    /**
     * 监听器：成功时回调并携带完整记录
     */
    @Test
    public void testListenerOnComplete() throws Exception {
        var before = completed.size();
        client.execute(Request.get(baseUrl + "/hello"));
        Assert.assertEquals(completed.size(), before + 1);
        var exchange = completed.get(completed.size() - 1);
        Assert.assertTrue(exchange.isSuccess());
        Assert.assertEquals(exchange.attempts().size(), 1);
        Assert.assertTrue(exchange.toWireString().contains("> GET "));
        Assert.assertTrue(exchange.toWireString().contains("< HTTP/"));
    }

    /**
     * 流式（不可重复读）请求体：不回读实体，靠 tee 随发送记录，服务端仍收到完整请求体
     */
    @Test
    public void testStreamingBodySentAndRecordedViaTee() throws Exception {
        var entity = new InputStreamEntity(new ByteArrayInputStream("stream-body".getBytes(StandardCharsets.UTF_8)),
                ContentType.TEXT_PLAIN);
        var response = client.execute(Request.post(baseUrl + "/echo").body(new StreamRequestEntity(entity)));

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(new String(lastRequestBody, StandardCharsets.UTF_8), "stream-body",
                "服务端应收到完整请求体（采集不得回读实体）");
        var attempt = response.exchange().lastAttempt();
        Assert.assertEquals(new String(attempt.wireBody(), StandardCharsets.UTF_8), "stream-body",
                "采集到的请求体应与线上发送的一致");
        Assert.assertEquals(attempt.requestBodyState(), Attempt.BodyState.CAPTURED);
    }

    /**
     * 重复 body() 不会在线上累积多个 Content-Type
     */
    @Test
    public void testBodyCalledTwiceSetsSingleContentType() throws Exception {
        var request = Request.post(baseUrl + "/echo")
                .body(RequestEntity.json("{\"a\":1}"))
                .body(RequestEntity.json("{\"b\":2}"));
        var response = client.execute(request);
        Assert.assertEquals(response.statusCode(), 200);
        var contentTypes = response.exchange().lastAttempt().wireHeaders();
        var count = 0;
        for (var header : contentTypes) {
            if (header.getName().equalsIgnoreCase("Content-Type")) {
                count++;
            }
        }
        Assert.assertEquals(count, 1, "线上只应有一个 Content-Type");
        Assert.assertEquals(new String(lastRequestBody, StandardCharsets.UTF_8), "{\"b\":2}");
    }

    // ------------------------------------------------------------------ 工具方法

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        } else {
            exchange.close();
        }
    }

    private void record(HttpExchange exchange, byte[] body) throws IOException {
        lastRequestHeaders = Map.copyOf(exchange.getRequestHeaders());
        lastRequestBody = exchange.getRequestBody().readAllBytes();
    }

    private static String headerValue(Header[] headers, String name) {
        for (var header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * 服务端收到的指定请求头的值（忽略大小写）
     */
    private List<String> serverValues(String name) {
        var headers = lastRequestHeaders;
        if (headers == null) {
            return null;
        }
        for (var entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String firstOf(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static boolean chainContains(Throwable throwable, Class<? extends Throwable> type) {
        var cause = throwable;
        while (cause != null) {
            if (type.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private byte[] readGzipRaw() throws IOException {
        var buffer = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(buffer)) {
            gzip.write(PLAIN_TEXT.getBytes(StandardCharsets.UTF_8));
        }
        return buffer.toByteArray();
    }

    /**
     * 不可重复读的请求体（流式）
     */
    private static final class StreamRequestEntity extends RequestEntity {

        private StreamRequestEntity(HttpEntity entity) {
            super(entity, new byte[0]);
        }
    }

    /**
     * 极简 HTTP 正向代理（绝对形式请求行），用于验证逐请求代理路由
     */
    private static final class TinyProxy implements AutoCloseable {

        private final ServerSocket server;
        private volatile String lastRequestLine = "";

        private TinyProxy() throws IOException {
            server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            var acceptor = new Thread(this::acceptLoop, "tiny-proxy");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        private int port() {
            return server.getLocalPort();
        }

        private void acceptLoop() {
            while (!server.isClosed()) {
                try {
                    var socket = server.accept();
                    var worker = new Thread(() -> handle(socket), "tiny-proxy-worker");
                    worker.setDaemon(true);
                    worker.start();
                } catch (IOException e) {
                    return;
                }
            }
        }

        private void handle(Socket client) {
            try (client) {
                client.setSoTimeout(10_000);
                var in = client.getInputStream();
                var requestLine = readLine(in);
                if (requestLine == null) {
                    return;
                }
                lastRequestLine = requestLine;
                var headers = new ArrayList<String>();
                String line;
                while ((line = readLine(in)) != null && !line.isEmpty()) {
                    if (!line.toLowerCase().startsWith("proxy-connection")) {
                        headers.add(line);
                    }
                }
                var parts = requestLine.split(" ");
                var target = URI.create(parts[1]);
                try (var upstream = new Socket(target.getHost(), target.getPort())) {
                    upstream.setSoTimeout(10_000);
                    var out = upstream.getOutputStream();
                    var path = target.getRawPath() + (target.getRawQuery() == null ? "" : "?" + target.getRawQuery());
                    write(out, parts[0] + " " + path + " " + parts[2]);
                    for (var header : headers) {
                        write(out, header);
                    }
                    write(out, "Connection: close");
                    write(out, "");
                    out.flush();
                    var upstreamIn = upstream.getInputStream();
                    var buffer = new byte[8192];
                    var toClient = client.getOutputStream();
                    int read;
                    while ((read = upstreamIn.read(buffer)) != -1) {
                        toClient.write(buffer, 0, read);
                    }
                    toClient.flush();
                }
            } catch (IOException ignored) {
                // 测试代理尽力而为
            }
        }

        private static String readLine(InputStream in) throws IOException {
            var buffer = new ByteArrayOutputStream();
            int value;
            while ((value = in.read()) != -1) {
                if (value == '\n') {
                    var bytes = buffer.toByteArray();
                    var length = bytes.length;
                    if (length > 0 && bytes[length - 1] == '\r') {
                        return new String(bytes, 0, length - 1, StandardCharsets.ISO_8859_1);
                    }
                    return new String(bytes, StandardCharsets.ISO_8859_1);
                }
                buffer.write(value);
            }
            return null;
        }

        private static void write(OutputStream out, String line) throws IOException {
            out.write((line + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
