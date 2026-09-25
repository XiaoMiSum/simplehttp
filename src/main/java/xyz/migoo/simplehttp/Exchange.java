package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 一次 HTTP 交换（exchange）的完整采集记录。
 * <p>
 * 一次 execute 可能产生多次「跳」（attempt）：重定向、自动重试都会产生新的跳。
 * 每一跳都独立记录了：
 * <ul>
 *     <li>实际发出的请求：最终 URI（含 query、含重定向后的地址）、发送前一刻的完整请求头
 *     （Host/Content-Length/Connection/User-Agent/Accept-Encoding 等由客户端补全的头）、请求体原始字节</li>
 *     <li>实际收到的响应：状态行、HTTP 版本、<b>线上原样响应头</b>（未经自动解压逻辑删改）、
 *     <b>线上原样响应体字节</b>（保留 Content-Encoding 压缩态）</li>
 * </ul>
 * 交换级别的信息包括：实际路由（target/proxy/是否隧道）、重定向链、耗时、失败阶段。
 *
 * @author xiaomi
 */
public final class Exchange {

    /**
     * 交换记录在 {@link HttpClientContext} 中的属性名
     */
    static final String ATTRIBUTE = "xyz.migoo.simplehttp.exchange";

    /**
     * 当前跳（attempt）在 {@link HttpClientContext} 中的属性名
     */
    static final String ATTEMPT_ATTRIBUTE = "xyz.migoo.simplehttp.attempt";
    private final String id;
    private final RequestSnapshot request;
    private final Timings timings;
    private final long maxCaptureBytes;
    private final Path captureDirectory;
    private final List<Attempt> attempts = new ArrayList<>();
    private volatile HttpRoute route;
    private volatile URI sentUri;
    private volatile List<URI> redirectLocations = List.of();
    private volatile boolean completed;
    private volatile Stage stage;
    private volatile Throwable failure;

    private Exchange(RequestSnapshot request, long maxCaptureBytes, Path captureDirectory) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.request = request;
        this.timings = new Timings(System.currentTimeMillis());
        this.maxCaptureBytes = maxCaptureBytes;
        this.captureDirectory = captureDirectory;
    }

    static Exchange create(Request request, long maxCaptureBytes, Path captureDirectory) {
        return new Exchange(RequestSnapshot.of(request), maxCaptureBytes, captureDirectory);
    }

    /**
     * 创建不含请求快照的交换记录（未经 {@link RequestExecutor} 执行时的兜底）
     */
    static Exchange detached(long maxCaptureBytes, Path captureDirectory) {
        return new Exchange(new RequestSnapshot(null, null, null, null, null), maxCaptureBytes, captureDirectory);
    }

    /**
     * @return 交换 id（每次 execute 唯一）
     */
    public String id() {
        return id;
    }

    /**
     * @return 用户发起的原始请求快照（执行前）
     */
    public RequestSnapshot request() {
        return request;
    }

    /**
     * @return 所有跳（按发生顺序），未执行时为空列表
     */
    public List<Attempt> attempts() {
        synchronized (attempts) {
            return Collections.unmodifiableList(new ArrayList<>(attempts));
        }
    }

    /**
     * @return 最后一跳（即最终响应），没有则返回 {@code null}
     */
    public Attempt lastAttempt() {
        synchronized (attempts) {
            return attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
        }
    }

    /**
     * @return 实际请求的目标主机，未建立连接时为 {@code null}
     */
    public HttpHost target() {
        return route == null ? null : route.getTargetHost();
    }

    /**
     * @return 实际使用的代理主机，未使用代理时为 {@code null}
     */
    public HttpHost proxy() {
        return route == null ? null : route.getProxyHost();
    }

    /**
     * @return 是否通过代理隧道（CONNECT）访问
     */
    public boolean isTunnelled() {
        return route != null && route.isTunnelled();
    }

    /**
     * @return 最终实际请求的 URI（含 query、含重定向后的地址），未知时回退到原始 URI
     */
    public URI finalUri() {
        var last = lastAttempt();
        if (last != null) {
            if (last.wireUri() != null) {
                return last.wireUri();
            }
            if (last.uri() != null) {
                return last.uri();
            }
        }
        // 未进入执行链（或已关闭采集）时，用实际派生出的请求 URI 兜底
        return sentUri != null ? sentUri : request.uri();
    }

    /**
     * @return 重定向链（不包含初始 URI），无重定向时为空列表
     */
    public List<URI> redirects() {
        return redirectLocations;
    }

    /**
     * @return 耗时记录
     */
    public Timings timings() {
        return timings;
    }

    /**
     * @return 是否成功完成
     */
    public boolean isSuccess() {
        return completed && failure == null;
    }

    /**
     * @return 失败阶段，未失败时为 {@code null}
     */
    public Stage stage() {
        return stage;
    }

    /**
     * @return 失败原因，未失败时为 {@code null}
     */
    public Throwable failure() {
        return failure;
    }

    /**
     * @return 最终响应的线上原样响应头（未经自动解压逻辑删改），无响应时为空数组
     */
    Header[] rawHeaders() {
        var last = lastAttempt();
        return last == null ? new Header[0] : last.rawHeaders();
    }

    /**
     * @return 最终响应的线上原样响应体字节（保留压缩态），无响应或已落盘时为空数组
     */
    byte[] rawBody() {
        var last = lastAttempt();
        return last == null ? new byte[0] : last.rawBody();
    }

    /**
     * 输出人类可读的完整报文（类 Charles/mitmproxy 风格），便于日志与排障。
     *
     * @return 报文文本
     */
    public String toWireString() {
        var sb = new StringBuilder();
        sb.append("=== exchange ").append(id).append(" ===\n");
        sb.append("request: ").append(request.method()).append(' ').append(request.uri()).append('\n');
        sb.append("target: ").append(target()).append(", proxy: ").append(proxy())
                .append(route != null && route.isTunnelled() ? " (tunnelled)" : "").append('\n');
        for (var attempt : attempts()) {
            sb.append("--- attempt #").append(attempt.index() + 1)
                    .append(attempt.statusCode() > 0 ? " -> " + attempt.statusCode() : "").append(" ---\n");
            sb.append("> ").append(attempt.method()).append(' ')
                    .append(attempt.wireUri() != null ? attempt.wireUri() : attempt.uri()).append('\n');
            for (var header : attempt.wireHeaders()) {
                sb.append("> ").append(header.getName()).append(": ").append(header.getValue()).append('\n');
            }
            sb.append("> body: ").append(attempt.wireBody() == null ? 0 : attempt.wireBody().length)
                    .append(" bytes (").append(attempt.requestBodyState()).append(")\n");
            if (attempt.statusCode() > 0) {
                sb.append("< HTTP/").append(attempt.httpVersion() == null ? "?" : attempt.httpVersion().replace("HTTP/", ""))
                        .append(' ').append(attempt.statusCode());
                if (attempt.reason() != null) {
                    sb.append(' ').append(attempt.reason());
                }
                sb.append('\n');
                for (var header : attempt.rawHeaders()) {
                    sb.append("< ").append(header.getName()).append(": ").append(header.getValue()).append('\n');
                }
                sb.append("< body: ").append(attempt.rawBodySize())
                        .append(attempt.rawBodyTruncated() ? "+ (truncated)" : " bytes")
                        .append(attempt.rawBodyFile() != null ? " -> " + attempt.rawBodyFile() : "").append('\n');
            }
        }
        sb.append("timings: ").append(timings).append('\n');
        if (failure != null) {
            sb.append("failure: [").append(stage).append("] ").append(failure).append('\n');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Exchange{id=" + id + ", attempts=" + attempts().size() + ", timings=" + timings
                + (failure != null ? ", failure=" + failure : "") + "}";
    }

    /**
     * 记录实际派生出的请求 URI，作为 {@link #finalUri()} 在没有采集到任何跳时的兜底
     */
    void rememberSentUri(URI uri) {
        if (uri != null) {
            this.sentUri = uri;
        }
    }

    Attempt beginAttempt(HttpRequest request, HttpRoute route) {
        if (attempts.isEmpty()) {
            // 首跳进入执行链的位置即连接（含 TLS）建立完成，近似连接耗时
            timings.markConnect();
        }
        var attempt = new Attempt(attempts.size(), request, maxCaptureBytes);
        synchronized (attempts) {
            attempts.add(attempt);
        }
        if (route != null) {
            this.route = route;
        }
        return attempt;
    }

    /**
     * 标记收到响应头（多跳时以最后一跳为准，即最终响应头到达时间）
     */
    void markResponseHeaders() {
        timings.markHeaders();
    }

    // ---------------------------------------------------------------- 内部状态

    void finish(HttpClientContext context) {
        if (context != null) {
            var locations = context.getRedirectLocations();
            if (locations != null && locations.size() > 0) {
                redirectLocations = List.copyOf(locations.getAll());
            }
        }
        synchronized (attempts) {
            for (var attempt : attempts) {
                attempt.finishWireBody();
                attempt.finishCapture(captureDirectory, id);
            }
        }
        timings.markCompleted();
        completed = true;
    }

    void markFailed(Stage stage, Throwable failure) {
        this.stage = stage;
        this.failure = failure;
        synchronized (attempts) {
            for (var attempt : attempts) {
                attempt.finishWireBody();
                attempt.finishCapture(captureDirectory, id);
            }
        }
        if (!timings.isCompleted()) {
            timings.markCompleted();
        }
        completed = true;
    }

    /**
     * 交换失败阶段
     */
    public enum Stage {
        /**
         * 域名解析失败
         */
        DNS,
        /**
         * 建立连接（含从连接池获取连接）失败
         */
        CONNECT,
        /**
         * TLS 握手失败
         */
        TLS,
        /**
         * 发送请求阶段失败
         */
        REQUEST,
        /**
         * 接收响应阶段失败
         */
        RESPONSE,
        /**
         * 超时
         */
        TIMEOUT,
        /**
         * 未能归类
         */
        UNKNOWN
    }

}
