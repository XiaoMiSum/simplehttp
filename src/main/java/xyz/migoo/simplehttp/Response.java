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
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * HTTP响应封装类，用于处理HTTP响应结果
 * 包含状态码、响应头、响应体等信息，并提供了便捷的方法来访问这些信息
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
     * 请求结束时间戳
     */
    Long endTime;

    /**
     * 响应体的字节数组
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
     * 获取请求开始时间戳
     *
     * @return 请求开始时间戳
     */
    public long startTime() {
        return startTime;
    }

    /**
     * 获取请求结束时间戳
     *
     * @return 请求结束时间戳
     */
    public long endTime() {
        return endTime;
    }

    /**
     * 获取请求持续时间（毫秒）
     *
     * @return 请求持续时间
     */
    public long duration() {
        return endTime - startTime;
    }

    /**
     * 获取响应体的字节数组
     *
     * @return 响应体字节数组
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * 获取响应体的文本表示
     *
     * @return 响应体文本
     */
    public String text() {
        return text(String::new);
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
     * @return Cookie列表
     */
    public List<Cookie> cookies() {
        return Objects.nonNull(cookieStore) ? cookieStore.getCookies() : null;
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
     * HTTP响应处理器，用于处理HttpClient的响应结果
     */
    public static class ResponseHandler implements HttpClientResponseHandler<Response> {

        /**
         * 响应结果对象
         */
        private final Response result = new Response(System.currentTimeMillis());

        /**
         * 构造一个新的响应处理器
         *
         * @param context HttpClient上下文
         */
        public ResponseHandler(HttpClientContext context) {
            result.cookieStore = context.getCookieStore();
        }

        /**
         * 处理HTTP响应
         *
         * @param response HTTP响应对象
         * @return 封装后的响应对象
         * @throws IOException 如果处理过程中发生IO错误
         */
        public Response handleResponse(ClassicHttpResponse response) throws IOException {
            result.endTime = System.currentTimeMillis();
            result.statusCode = response.getCode();
            result.headers = response.getHeaders();
            result.version = response.getVersion().toString();
            result.bytes = EntityUtils.toByteArray(response.getEntity());
            result.message = response.getReasonPhrase();
            return result;
        }
    }
}
