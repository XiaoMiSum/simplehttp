/*
 *
 *  * The MIT License (MIT)
 *
 *  * Copyright (c) 2025.  Lorem XiaoMiSum (mi_xiao@qq.com)
 *
 *  * Permission is hereby granted, free of charge, to any person obtaining
 *  * a copy of this software and associated documentation files (the
 *  * 'Software'), to deal in the Software without restriction, including
 *  * without limitation the rights to use, copy, modify, merge, publish,
 *  * distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to
 *  * the following conditions:
 *
 *  * The above copyright notice and this permission notice shall be
 *  * included in all copies or substantial portions of the Software.
 *
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

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.URIScheme;

import java.net.URI;


/**
 * 用户发起的原始请求快照（执行前的意图，非线上报文；线上报文见 {@link Attempt}）。
 * <p>
 * 这是请求信息的对外读取入口：方法、URI、query、请求头、请求体、HTTP 版本。
 * 不可变，执行开始即定格——{@link Request} 上的链式设置不会回写到这里。
 */
public final class RequestSnapshot {

    private final String method;
    private final URI uri;
    private final String query;
    private final Header[] headers;
    private final byte[] body;
    private final String version;

    RequestSnapshot(String method, URI uri, String query, Header[] headers, byte[] body, String version) {
        this.method = method;
        this.uri = uri;
        this.query = query;
        this.headers = headers == null ? new Header[0] : headers;
        this.body = body == null ? new byte[0] : body;
        this.version = version;
    }

    static RequestSnapshot of(Request request) {
        var actual = request.httpRequest();
        Header[] headers;
        try {
            headers = actual.getHeaders();
        } catch (RuntimeException e) {
            headers = new Header[0];
        }
        String version;
        try {
            version = actual.getVersion().toString();
        } catch (RuntimeException e) {
            version = null;
        }
        URI uri;
        try {
            uri = URI.create(uriOf(actual));
        } catch (RuntimeException e) {
            uri = null;
        }
        Form form = request.getQuery();
        return new RequestSnapshot(actual.getMethod(), uri, form == null ? "" : form.toString(),
                headers, request.getBody(), version);
    }

    private static String uriOf(HttpRequest request) {
        StringBuilder buf = new StringBuilder();
        if (request.getAuthority() != null) {
            buf.append(request.getScheme() != null ? request.getScheme() : URIScheme.HTTP.id).append("://");
            buf.append(request.getAuthority().getHostName());
            if (request.getAuthority().getPort() > 0) {
                buf.append(":").append(request.getAuthority().getPort());
            }
        }
        var path = request.getPath();
        if (path == null) {
            buf.append("/");
        } else {
            if (!buf.isEmpty() && !path.startsWith("/")) {
                buf.append("/");
            }
            buf.append(path);
        }
        return buf.toString();
    }

    public String method() {
        return method;
    }

    public URI uri() {
        return uri;
    }

    /**
     * @return 用户设置的查询参数，以 {@link Form} 的 JSON 描述形式给出（与 {@link #body()} 的
     *         描述性一致），未合入 {@link #uri()}；线上合入 query 后的 URI 见 {@link Exchange#finalUri()}
     */
    public String query() {
        return query;
    }

    /**
     * @return 用户显式设置的请求头（不含客户端自动补全的头，线上完整头见 {@link Attempt#wireHeaders()}）
     */
    public Header[] headers() {
        return headers;
    }

    /**
     * @return 用户设置的请求体。注意 multipart 上传时这里是描述性 JSON，线上真实字节见 {@link Attempt#wireBody()}
     */
    public byte[] body() {
        return body;
    }

    public String version() {
        return version;
    }

    @Override
    public String toString() {
        return method + " " + uri;
    }
}
