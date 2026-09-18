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
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.apache.hc.core5.http.HttpHeaders.USER_AGENT;
import static xyz.migoo.simplehttp.HttpMethod.DELETE;
import static xyz.migoo.simplehttp.HttpMethod.GET;
import static xyz.migoo.simplehttp.HttpMethod.HEAD;
import static xyz.migoo.simplehttp.HttpMethod.OPTIONS;
import static xyz.migoo.simplehttp.HttpMethod.PATCH;
import static xyz.migoo.simplehttp.HttpMethod.POST;
import static xyz.migoo.simplehttp.HttpMethod.PUT;
import static xyz.migoo.simplehttp.HttpMethod.TRACE;

/**
 * 请求对象，提供链式 API 构建并执行 HTTP 请求
 *
 * @author xiaomi
 *         Created at 2019/9/13 10:58
 */
public class Request {

    private HttpRequest request;
    private Form query;
    private byte[] body;
    private Boolean useExpectContinue;
    private Integer socketTimeout;
    private Integer readTimeout;
    private Boolean redirectsEnabled;
    private HttpProxy proxy;
    private List<Cookie> cookies;

    protected Request(String method, String url) {
        this(new HttpRequest(method.toUpperCase(Locale.ROOT), URI.create(url)));
    }

    private Request(HttpRequest request) {
        this.request = request;
    }

    /**
     * 使用指定的 HTTP 方法和 URL 创建一个请求
     *
     * @param method HTTP 方法
     * @param url    请求地址
     * @return 请求对象
     */
    public static Request create(String method, String url) {
        return new Request(method, url);
    }

    /**
     * 使用指定的 HTTP 方法和 URL 创建一个请求
     *
     * @param method HTTP 方法
     * @param url    请求地址
     * @return 请求对象
     */
    public static Request create(HttpMethod method, String url) {
        return create(method.name(), url);
    }

    /**
     * 创建一个 GET 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request get(String url) {
        return create(GET, url);
    }

    /**
     * 创建一个 POST 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request post(String url) {
        return create(POST, url);
    }

    /**
     * 创建一个 PUT 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request put(String url) {
        return create(PUT, url);
    }

    /**
     * 创建一个 DELETE 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request delete(String url) {
        return create(DELETE, url);
    }

    /**
     * 创建一个 HEAD 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request head(String url) {
        return create(HEAD, url);
    }

    /**
     * 创建一个 PATCH 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request patch(String url) {
        return create(PATCH, url);
    }

    /**
     * 创建一个 TRACE 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request trace(String url) {
        return create(TRACE, url);
    }

    /**
     * 创建一个 OPTIONS 请求
     *
     * @param url 请求地址
     * @return 请求对象
     */
    public static Request options(String url) {
        return create(OPTIONS, url);
    }

    /**
     * 设置 HTTP/2 版本
     *
     * @return 当前请求对象
     */
    public Request http2() {
        request.setVersion(HttpVersion.HTTP_2_0);
        return this;
    }

    /**
     * 设置 HTTP 版本
     *
     * @param version HTTP 版本
     * @return 当前请求对象
     */
    public Request version(HttpVersion version) {
        request.setVersion(version);
        return this;
    }

    /**
     * 设置请求体
     *
     * @param entity 请求体对象
     * @return 当前请求对象
     */
    public Request body(RequestEntity entity) {
        this.body = entity.getContent();
        request.setEntity(entity.getEntity());
        request.addHeader("Content-Type", entity.getEntity().getContentType());
        return this;
    }

    /**
     * 设置查询参数
     *
     * @param query 查询参数表单
     * @return 当前请求对象
     */
    public Request query(Form query) {
        this.query = query;
        return this;
    }

    /**
     * 设置 Cookie 列表
     *
     * @param cookies Cookie 列表
     * @return 当前请求对象
     */
    public Request cookies(List<Cookie> cookies) {
        this.cookies = cookies;
        return this;
    }

    /**
     * 设置 Cookie 列表
     *
     * @param cookies Cookie 数组
     * @return 当前请求对象
     */
    public Request cookies(Cookie... cookies) {
        if (cookies.length > 0) {
            this.cookies = Arrays.stream(cookies).toList();
        }
        return this;
    }

    /**
     * 追加 Cookie
     *
     * @param cookies Cookie 数组
     * @return 当前请求对象
     */
    public Request addCookie(Cookie... cookies) {
        if (cookies.length > 0) {
            this.cookies = Optional.ofNullable(this.cookies).orElse(new ArrayList<>());
            this.cookies.addAll(Arrays.stream(cookies).toList());
        }
        return this;
    }

    /**
     * 追加 Cookie
     *
     * @param name        Cookie 名称
     * @param value       Cookie 值
     * @param domain      Cookie 作用域
     * @param path        Cookie 路径
     * @param expiryDate  Cookie 过期时间
     * @return 当前请求对象
     */
    public Request addCookie(String name, String value, String domain, String path, Date expiryDate) {
        var cookie = new BasicClientCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookie.setExpiryDate(DateUtils.toInstant(expiryDate));
        return this.addCookie(cookie);
    }

    /**
     * 追加 Cookie
     *
     * @param name  Cookie 名称
     * @param value Cookie 值
     * @return 当前请求对象
     */
    public Request addCookie(String name, String value) {
        return this.addCookie(name, value, null, null, null);
    }

    /**
     * 通过自定义器添加请求头
     *
     * @param customizer 请求头自定义器
     * @return 当前请求对象
     */
    public Request headers(Customizer<List<Header>> customizer) {
        var headers = new ArrayList<Header>();
        customizer.customize(headers);
        headers.forEach(request::addHeader);
        return this;
    }

    /**
     * 添加请求头列表
     *
     * @param headers 请求头列表
     * @return 当前请求对象
     */
    public Request headers(List<Header> headers) {
        Args.notNull(headers, "headers").forEach(request::addHeader);
        return this;
    }

    /**
     * 添加请求头
     *
     * @param header 请求头
     * @return 当前请求对象
     */
    public Request addHeader(Header header) {
        request.addHeader(header);
        return this;
    }

    /**
     * 添加请求头
     *
     * @param name  请求头名称
     * @param value 请求头值
     * @return 当前请求对象
     */
    public Request addHeader(String name, String value) {
        return this.addHeader(new BasicHeader(name, value));
    }

    /**
     * 设置代理
     *
     * @param proxy 代理配置
     * @return 当前请求对象
     */
    public Request proxy(HttpProxy proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * 设置代理
     *
     * @param host 代理主机地址
     * @param port 代理端口号
     * @return 当前请求对象
     */
    public Request proxy(String host, Integer port) {
        return this.proxy(null, host, port);
    }

    /**
     * 设置代理
     *
     * @param scheme 代理协议
     * @param host   代理主机地址
     * @param port   代理端口号
     * @return 当前请求对象
     */
    public Request proxy(String scheme, String host, Integer port) {
        return this.proxy(scheme, host, port, null, null);
    }

    /**
     * 设置带认证信息的代理
     *
     * @param scheme   代理协议
     * @param host     代理主机地址
     * @param port     代理端口号
     * @param username 认证用户名
     * @param password 认证密码
     * @return 当前请求对象
     */
    public Request proxy(String scheme, String host, Integer port, String username, String password) {
        return this.proxy(new HttpProxy(scheme, host, port, username, password));
    }

    /**
     * 使用默认客户端执行请求
     *
     * @return 响应对象
     * @throws IOException              执行过程中发生的 IO 异常
     * @throws URISyntaxException       请求地址格式错误
     */
    public Response execute() throws IOException, URISyntaxException {
        return SimpleHttp.getDefault().execute(this);
    }

    /**
     * 使用指定客户端执行请求
     *
     * @param client HTTP 客户端
     * @return 响应对象
     * @throws IOException              执行过程中发生的 IO 异常
     * @throws URISyntaxException       请求地址格式错误
     */
    public Response execute(SimpleHttp client) throws IOException, URISyntaxException {
        return client.execute(this);
    }

    /**
     * 启用 Expect-Continue 机制
     *
     * @return 当前请求对象
     */
    public Request useExpectContinue() {
        this.useExpectContinue = Boolean.TRUE;
        return this;
    }

    /**
     * 设置 User-Agent 请求头
     *
     * @param agent User-Agent 值
     * @return 当前请求对象
     */
    public Request userAgent(final String agent) {
        this.request.setHeader(USER_AGENT, agent);
        return this;
    }

    /**
     * 设置连接超时时间（秒），仅在请求级别覆盖客户端默认值
     *
     * @param timeout 超时时间（秒）
     * @return 当前请求对象
     */
    public Request socketTimeout(final int timeout) {
        this.socketTimeout = timeout;
        return this;
    }

    /**
     * 设置读取超时时间（秒），仅在请求级别覆盖客户端默认值
     *
     * @param timeout 超时时间（秒）
     * @return 当前请求对象
     */
    public Request readTimeout(final int timeout) {
        this.readTimeout = timeout;
        return this;
    }

    /**
     * 设置是否自动重定向
     *
     * @param redirectsEnabled 是否启用重定向
     * @return 当前请求对象
     */
    public Request redirectsEnabled(Boolean redirectsEnabled) {
        this.redirectsEnabled = redirectsEnabled;
        return this;
    }

    protected Request request(HttpRequest request) {
        this.request = request;
        return this;
    }

    /**
     * 获取请求体字节数组
     *
     * @return 请求体字节数组
     */
    public byte[] body() {
        return body == null ? new byte[] {} : body;
    }

    /**
     * 获取查询参数的字符串表示
     *
     * @return 查询参数 JSON 字符串
     */
    public String query() {
        return query == null ? "" : query.toString();
    }

    /**
     * 获取全部请求头
     *
     * @return 请求头数组
     */
    public Header[] headers() {
        return request.getHeaders();
    }

    /**
     * 获取代理配置的字符串表示
     *
     * @return 代理配置字符串
     */
    public String proxy() {
        return proxy == null ? null : proxy.toString();
    }

    /**
     * 获取 HTTP 方法
     *
     * @return HTTP 方法
     */
    public String method() {
        return request.getMethod();
    }

    /**
     * 获取 Cookie 列表
     *
     * @return Cookie 列表
     */
    public List<Cookie> cookies() {
        return cookies;
    }

    /**
     * 获取请求地址（不含查询参数）
     *
     * @return 请求地址
     */
    public String uri() {
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

    /**
     * 获取请求地址（包含 URL 自带查询参数以及 {@link #query(Form)} 设置的查询参数）
     *
     * @return 完整请求地址
     */
    public String fullUri() {
        try {
            URIBuilder builder = new URIBuilder(request.getUri());
            Form form = getQuery();
            if (form != null && !form.build().isEmpty()) {
                builder.addParameters(form.build());
            }
            return builder.build().toString();
        } catch (URISyntaxException e) {
            return uri();
        }
    }

    /**
     * 获取 HTTP 版本
     *
     * @return HTTP 版本
     */
    public String version() {
        return request.getVersion().toString();
    }

    HttpRequest httpRequest() {
        return request;
    }

    Integer getSocketTimeout() {
        return socketTimeout;
    }

    Integer getReadTimeout() {
        return readTimeout;
    }

    Boolean getUseExpectContinue() {
        return useExpectContinue;
    }

    Boolean getRedirectsEnabled() {
        return redirectsEnabled;
    }

    HttpProxy getProxy() {
        return proxy;
    }

    List<Cookie> getCookies() {
        return cookies;
    }

    Form getQuery() {
        return query;
    }
}