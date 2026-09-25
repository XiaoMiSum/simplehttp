/*
 *
 *  * The MIT License (MIT)
 *  *
 *  * Copyright (c) 2025.  Lorem XiaoMiSum (mi_xiao@qq.com)
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining
 *  * a copy of this software and associated documentation files (the
 *  * 'Software'), to deal in the Software without restriction, including
 *  * without limitation the rights to use, copy, modify, merge, publish,
 *  * distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to
 *  * the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be
 *  * included in all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 *  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * HTTP响应封装类，包含两类信息：
 * <ul>
 *     <li><b>结果视图</b>（便于使用）：{@link #text()} 按 Content-Type 字符集解码、
 *     {@link #bytes()} 为自动解压后的响应体；</li>
 *     <li><b>真实视图</b>（线上原样）：{@link #rawHeaders()} 为未经自动解压逻辑删改的原始响应头
 *     （Content-Length/Content-Encoding 均保留），{@link #rawBytes()} 为保留压缩态的线上原始字节。</li>
 * </ul>
 * 链路信息（最终 URI、重定向链、实际路由、耗时明细、逐跳原始报文）统一从 {@link #exchange()} 获取，
 * 本类只保留响应自身的结果与起止耗时。
 *
 * @author xiaomi
 * Created at 2019/9/13 11:01
 */
public class Response {

    /**
     * 请求开始时间戳
     */
    private final Long startTime;

    /**
     * 请求结束时间戳（读完响应体之后）
     */
    Long endTime;

    /**
     * 响应体的字节数组（自动解压后）
     */
    byte[] bytes;

    /**
     * HTTP状态码
     */
    int statusCode;

    /**
     * 响应头数组
     */
    Header[] headers;

    /**
     * HTTP版本
     */
    String version;

    /**
     * Cookie存储
     */
    CookieStore cookieStore;

    /**
     * 响应消息（原因短语）
     */
    String message;

    /**
     * 线上原样响应头（未经自动解压逻辑删改）
     */
    private Header[] rawHeaders = new Header[0];

    /**
     * 线上原样响应体（保留压缩态）
     */
    private byte[] rawBytes = new byte[0];

    /**
     * 完整交换记录
     */
    private Exchange exchange;

    /**
     * 构造一个新的响应对象
     *
     * @param startTime 请求开始时间戳
     */
    public Response(long startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取HTTP状态码
     *
     * @return HTTP状态码
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * 获取响应头数组
     *
     * @return 响应头数组
     */
    public Header[] headers() {
        return headers;
    }

    /**
     * 获取线上原样响应头（未经自动解压逻辑删改，Content-Length/Content-Encoding 均保留）
     *
     * @return 线上原样响应头数组
     */
    public Header[] rawHeaders() {
        return rawHeaders;
    }

    /**
     * 按名称获取第一个响应头（忽略大小写）
     *
     * @param name 头名称
     * @return 头值，不存在时返回 {@code null}
     */
    public String header(String name) {
        var header = firstHeader(name);
        return header == null ? null : header.getValue();
    }

    /**
     * 按名称获取所有同名响应头（忽略大小写，如多个 Set-Cookie）
     *
     * @param name 头名称
     * @return 匹配的头列表，无匹配时为空列表
     */
    public List<Header> headers(String name) {
        var matched = new ArrayList<Header>();
        if (headers != null) {
            for (var header : headers) {
                if (header.getName().equalsIgnoreCase(name)) {
                    matched.add(header);
                }
            }
        }
        return matched;
    }

    /**
     * 判断响应是否成功（状态码 2xx）
     *
     * @return 是否 2xx
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * 获取请求开始时间戳
     *
     * @return 请求开始时间戳
     */
    public long startTime() {
        return startTime;
    }

    /**
     * 获取请求结束时间戳（读完响应体之后）
     *
     * @return 请求结束时间戳
     */
    public long endTime() {
        return endTime;
    }

    /**
     * 获取请求持续时间（毫秒），含响应体读取
     *
     * @return 请求持续时间
     */
    public long duration() {
        return endTime - startTime;
    }

    /**
     * 获取响应体的字节数组（自动解压后）
     *
     * @return 响应体字节数组
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * 获取线上原始响应体字节（保留 Content-Encoding 压缩态）
     *
     * @return 线上原始响应体，超出采集上限被截断时见 {@link Attempt#rawBodyTruncated()}，
     * 配置落盘时为空数组，见 {@link Attempt#rawBodyFile()}
     */
    public byte[] rawBytes() {
        return rawBytes;
    }

    /**
     * 获取响应体的文本表示（按 Content-Type 中的字符集解码，缺省 UTF-8）
     *
     * @return 响应体文本
     */
    public String text() {
        return text(charset());
    }

    /**
     * 使用指定字符集解码响应体
     *
     * @param charset 字符集
     * @return 响应体文本
     */
    public String text(Charset charset) {
        return bytes == null ? null : new String(bytes, charset);
    }

    /**
     * 使用指定的转换器响应体的文本表示
     *
     * @param byteToStringConverter 文本转换器
     * @return 响应体文本
     */
    public String text(Function<byte[], String> byteToStringConverter) {
        return byteToStringConverter.apply(bytes);
    }

    /**
     * 响应体应使用的字符集：优先取 Content-Type 中的 charset，缺省 UTF-8
     *
     * @return 字符集
     */
    Charset charset() {
        var contentType = contentType();
        if (contentType != null) {
            try {
                var charset = ContentType.parse(contentType).getCharset();
                if (charset != null) {
                    return charset;
                }
            } catch (Exception ignored) {
                // Content-Type 非法时回退默认字符集
            }
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * 获取 Content-Type 头的值（线上原样优先）
     *
     * @return Content-Type 值，不存在时返回 {@code null}
     */
    private String contentType() {
        return rawHeaderOrHeader(HttpHeaders.CONTENT_TYPE);
    }

    /**
     * 获取 Content-Encoding 头的值（线上原样优先，自动解压不会使其丢失）
     *
     * @return Content-Encoding 值，不存在（即未压缩）时返回 {@code null}
     */
    public String contentEncoding() {
        return rawHeaderOrHeader(HttpHeaders.CONTENT_ENCODING);
    }

    /**
     * 获取线上 Content-Length（线上原样优先）
     *
     * @return Content-Length，不存在或非法时返回 {@code -1}
     */
    public long contentLength() {
        var value = rawHeaderOrHeader(HttpHeaders.CONTENT_LENGTH);
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 将响应体保存到指定路径的文件中
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public String save(String path) {
        return text(byteToStringConverter -> {
            try {
                return Files.write(Path.of(path), bytes, CREATE, TRUNCATE_EXISTING).toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 获取响应中的Cookie列表
     *
     * @return Cookie列表，无 Cookie 时返回空列表
     */
    public List<Cookie> cookies() {
        return cookieStore == null ? Collections.emptyList() : cookieStore.getCookies();
    }

    /**
     * 获取HTTP版本
     *
     * @return HTTP版本
     */
    public String version() {
        return version;
    }

    /**
     * 获取响应消息（原因短语）
     *
     * @return 响应消息
     */
    public String message() {
        return message;
    }

    /**
     * 获取本次交换的完整采集记录（真实请求头/请求体/原始响应/逐跳链路）
     *
     * @return 交换记录，未经由本库执行时为 {@code null}
     */
    public Exchange exchange() {
        return exchange;
    }

    private Header firstHeader(String name) {
        if (headers == null) {
            return null;
        }
        for (var header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    private String rawHeaderOrHeader(String name) {
        if (rawHeaders != null) {
            for (var header : rawHeaders) {
                if (header.getName().equalsIgnoreCase(name)) {
                    return header.getValue();
                }
            }
        }
        return header(name);
    }

    /**
     * HTTP响应处理器，用于处理HttpClient的响应结果
     */
    public static class ResponseHandler implements HttpClientResponseHandler<Response> {

        /**
         * 执行上下文（用于读取交换记录）
         */
        private final HttpClientContext context;

        /**
         * 响应结果对象
         */
        private final Response result;

        /**
         * 构造一个新的响应处理器
         *
         * @param context HttpClient上下文
         */
        public ResponseHandler(HttpClientContext context) {
            this.context = context;
            var exchange = exchangeOf(context);
            this.result = new Response(exchange != null ? exchange.timings().startTime() : System.currentTimeMillis());
            result.cookieStore = context.getCookieStore();
            result.exchange = exchange;
        }

        /**
         * 处理HTTP响应
         *
         * @param response HTTP响应对象
         * @return 封装后的响应对象
         * @throws IOException 如果处理过程中发生IO错误
         */
        public Response handleResponse(ClassicHttpResponse response) throws IOException {
            var entity = response.getEntity();
            // HEAD/204/304 等响应没有实体
            var body = entity == null ? new byte[0] : EntityUtils.toByteArray(entity);
            result.endTime = System.currentTimeMillis();
            result.statusCode = response.getCode();
            var responseHeaders = response.getHeaders();
            result.headers = responseHeaders == null ? new Header[0] : responseHeaders;
            var version = response.getVersion();
            result.version = version == null ? null : version.toString();
            result.bytes = body;
            result.message = response.getReasonPhrase();
            if (result.exchange != null) {
                // 结束采集：完成耗时统计、落盘/取出线上原始响应体、记录重定向链
                result.exchange.finish(context);
                result.rawHeaders = result.exchange.rawHeaders();
                result.rawBytes = result.exchange.rawBody();
            }
            return result;
        }

        private static Exchange exchangeOf(HttpClientContext context) {
            var attribute = context.getAttribute(Exchange.ATTRIBUTE);
            return attribute instanceof Exchange value ? value : null;
        }
    }
}
