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

import java.net.URI;


/**
 * 用户发起的原始请求快照（执行前的意图，非线上报文；线上报文见 {@link Attempt}）
 */
public final class RequestSnapshot {

    private final String method;
    private final URI uri;
    private final Header[] headers;
    private final byte[] body;
    private final String version;

    RequestSnapshot(String method, URI uri, Header[] headers, byte[] body, String version) {
        this.method = method;
        this.uri = uri;
        this.headers = headers == null ? new Header[0] : headers;
        this.body = body == null ? new byte[0] : body;
        this.version = version;
    }

    static RequestSnapshot of(Request request) {
        Header[] headers;
        try {
            headers = request.headers();
        } catch (RuntimeException e) {
            headers = new Header[0];
        }
        String version;
        try {
            version = request.version();
        } catch (RuntimeException e) {
            version = null;
        }
        URI uri;
        try {
            uri = URI.create(request.uri());
        } catch (RuntimeException e) {
            uri = null;
        }
        return new RequestSnapshot(request.method(), uri, headers, request.body(), version);
    }

    public String method() {
        return method;
    }

    public URI uri() {
        return uri;
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
