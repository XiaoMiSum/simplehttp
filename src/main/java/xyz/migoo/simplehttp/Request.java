package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Args;

import java.net.URI;
import java.util.*;

import static org.apache.hc.core5.http.HttpHeaders.USER_AGENT;
import static xyz.migoo.simplehttp.HttpMethod.*;

/**
 * 请求构建器：链式设置方法、URI、请求头、请求体与执行参数。
 * <p>
 * 这里只负责「写」；请求信息的读取统一走 {@link Exchange#request()}，
 * 它是执行前的 {@link RequestSnapshot}，与线上报文 {@link Attempt} 对照使用。
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
    private Long maxCaptureBytes;
    private ExchangeListener exchangeListener;
    private Exchange exchange;

    protected Request(String method, String url) {
        this(new HttpRequest(method.toUpperCase(Locale.ROOT), URI.create(url)));
    }

    private Request(HttpRequest request) {
        this.request = request;
    }

    public static Request create(String method, String url) {
        return new Request(method, url);
    }

    public static Request create(HttpMethod method, String url) {
        return create(method.name(), url);
    }

    public static Request get(String url) {
        return create(GET, url);
    }

    public static Request post(String url) {
        return create(POST, url);
    }

    public static Request put(String url) {
        return create(PUT, url);
    }

    public static Request delete(String url) {
        return create(DELETE, url);
    }

    public static Request head(String url) {
        return create(HEAD, url);
    }

    public static Request patch(String url) {
        return create(PATCH, url);
    }

    public static Request trace(String url) {
        return create(TRACE, url);
    }

    public static Request options(String url) {
        return create(OPTIONS, url);
    }

    public Request http2() {
        request.setVersion(HttpVersion.HTTP_2_0);
        return this;
    }

    public Request version(HttpVersion version) {
        request.setVersion(version);
        return this;
    }

    public Request body(RequestEntity entity) {
        this.body = entity.getContent();
        request.setEntity(entity.getEntity());
        // setHeader 而非 addHeader：重复设置 body 不会在线上累积多个 Content-Type
        request.setHeader("Content-Type", entity.getEntity().getContentType());
        return this;
    }

    public Request query(Form query) {
        this.query = query;
        return this;
    }

    public Request cookies(List<Cookie> cookies) {
        this.cookies = cookies;
        return this;
    }

    public Request cookies(Cookie... cookies) {
        if (cookies.length > 0) {
            this.cookies = Arrays.stream(cookies).toList();
        }
        return this;
    }

    public Request addCookie(Cookie... cookies) {
        if (cookies.length > 0) {
            this.cookies = Optional.ofNullable(this.cookies).orElse(new ArrayList<>());
            this.cookies.addAll(Arrays.stream(cookies).toList());
        }
        return this;
    }

    public Request addCookie(String name, String value, String domain, String path, Date expiryDate) {
        var cookie = new BasicClientCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookie.setExpiryDate(DateUtils.toInstant(expiryDate));
        return this.addCookie(cookie);
    }

    public Request addCookie(String name, String value) {
        return this.addCookie(name, value, null, null, null);
    }

    public Request headers(Customizer<List<Header>> customizer) {
        var headers = new ArrayList<Header>();
        customizer.customize(headers);
        headers.forEach(request::addHeader);
        return this;
    }

    public Request headers(List<Header> headers) {
        Args.notNull(headers, "headers").forEach(request::addHeader);
        return this;
    }

    public Request addHeader(Header header) {
        request.addHeader(header);
        return this;
    }

    public Request addHeader(String name, String value) {
        return this.addHeader(new BasicHeader(name, value));
    }

    public Request proxy(HttpProxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public Request proxy(String host, Integer port) {
        return this.proxy(null, host, port);
    }

    public Request proxy(String scheme, String host, Integer port) {
        return this.proxy(scheme, host, port, null, null);
    }

    public Request proxy(String scheme, String host, Integer port, String username, String password) {
        return this.proxy(new HttpProxy(scheme, host, port, username, password));
    }

    public Response execute() throws Exception {
        return SimpleHttp.getDefault().execute(this);
    }

    public Response execute(SimpleHttp client) throws Exception {
        return client.execute(this);
    }

    public Request useExpectContinue() {
        this.useExpectContinue = Boolean.TRUE;
        return this;
    }

    public Request userAgent(final String agent) {
        this.request.setHeader(USER_AGENT, agent);
        return this;
    }

    /**
     * 设置从连接池获取连接的超时（秒）
     *
     * @param timeout 超时秒数
     * @return 当前请求实例
     */
    public Request connectionRequestTimeout(final int timeout) {
        this.socketTimeout = timeout;
        return this;
    }

    /**
     * 设置从连接池获取连接的超时（秒）
     *
     * @param timeout 超时秒数
     * @return 当前请求实例
     * @deprecated 命名与实际语义不符，请使用 {@link #connectionRequestTimeout(int)}；
     * 连接建立（含 TLS 握手）超时请在 {@link SimpleHttp.Builder#connectTimeout(int)} 配置
     */
    @Deprecated
    public Request socketTimeout(final int timeout) {
        this.socketTimeout = timeout;
        return this;
    }

    /**
     * 覆盖本次请求的线上原始报文采集上限（字节），超出即截断；{@code -1} 表示不限制
     *
     * @param maxCaptureBytes 采集上限
     * @return 当前请求实例
     */
    public Request maxCaptureBytes(long maxCaptureBytes) {
        this.maxCaptureBytes = maxCaptureBytes;
        return this;
    }

    /**
     * 覆盖本次请求的交换记录监听器（优先于客户端级配置）
     *
     * @param listener 监听器
     * @return 当前请求实例
     */
    public Request exchangeListener(ExchangeListener listener) {
        this.exchangeListener = listener;
        return this;
    }

    /**
     * 最近一次执行（成功或失败）的交换记录；未执行过时为 {@code null}
     *
     * @return 交换记录
     */
    public Exchange exchange() {
        return exchange;
    }

    void exchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public Request readTimeout(final int timeout) {
        this.readTimeout = timeout;
        return this;
    }

    protected Request request(HttpRequest request) {
        this.request = request;
        return this;
    }

    public Request redirectsEnabled(Boolean redirectsEnabled) {
        this.redirectsEnabled = redirectsEnabled;
        return this;
    }

    HttpRequest httpRequest() {
        return request;
    }

    byte[] getBody() {
        return body == null ? new byte[] {} : body;
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

    Long getMaxCaptureBytes() {
        return maxCaptureBytes;
    }

    ExchangeListener getExchangeListener() {
        return exchangeListener;
    }

    List<Cookie> getCookies() {
        return cookies;
    }

    Form getQuery() {
        return query;
    }
}
