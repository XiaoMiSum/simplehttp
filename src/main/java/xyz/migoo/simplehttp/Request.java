package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Args;

import java.net.URI;
import java.util.*;

import static org.apache.hc.core5.http.HttpHeaders.USER_AGENT;
import static org.apache.hc.core5.util.Timeout.ofSeconds;
import static xyz.migoo.simplehttp.HttpMethod.*;

/**
 * @author xiaomi
 * Created at 2019/9/13 10:58
 */
public class Request {

    private final static PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER =
            PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(DefaultClientTlsStrategy.createSystemDefault())
                    .setMaxConnPerRoute(2)
                    .setMaxConnTotal(20)
                    .setDefaultTlsConfig(TlsConfig.DEFAULT)
                    .build();


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
        request.addHeader("Content-Type", entity.getEntity().getContentType());
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
        return this.addCookie();
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

    Response execute(CloseableHttpClient client, HttpClientContext context) throws Exception {
        if (query != null && !query.build().isEmpty()) {
            request.setUri(new URIBuilder(request.getUri()).addParameters(query.build()).build());
        }
        return client.execute(request, context, new Response.ResponseHandler(context));
    }

    public Response execute() throws Exception {
        var builder = HttpClients.custom().setRedirectStrategy(new DefaultRedirectStrategy()).setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER);
        if (this.proxy != null) {
            var proxy = new HttpHost(this.proxy.getScheme(), this.proxy.getHost(), this.proxy.getPort());
            builder.setRoutePlanner(new DefaultProxyRoutePlanner(proxy));
            if (this.proxy.hasUsernameAndPassword()) {
                var provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(this.proxy.getUsername(), this.proxy.getPassword().toCharArray()));
                builder.setDefaultCredentialsProvider(provider);
            }
        }
        return execute(builder.build());
    }

    public Response execute(CloseableHttpClient client) throws Exception {
        var localContext = HttpClientContext.create();
        var builder = client instanceof Configurable c ? RequestConfig.copy(c.getConfig()) : RequestConfig.custom();
        builder.setExpectContinueEnabled(Objects.nonNull(useExpectContinue) ? useExpectContinue : false);
        builder.setConnectionRequestTimeout(Objects.nonNull(socketTimeout) ? ofSeconds(socketTimeout) : ofSeconds(180));
        builder.setResponseTimeout(Objects.nonNull(readTimeout) ? ofSeconds(readTimeout) : ofSeconds(180));
        if (Objects.nonNull(cookies) && !cookies.isEmpty()) {
            var cookieStore = new BasicCookieStore();
            cookies.forEach(cookieStore::addCookie);
            localContext.setCookieStore(cookieStore);
        }
        builder.setRedirectsEnabled(Objects.nonNull(redirectsEnabled) ? redirectsEnabled : true);
        localContext.setRequestConfig(builder.build());
        if (Objects.nonNull(query) && !query.build().isEmpty()) {
            request.setUri(new URIBuilder(request.getUri()).addParameters(query.build()).build());
        }
        return execute(client, localContext);
    }

    public Request useExpectContinue() {
        this.useExpectContinue = Boolean.TRUE;
        return this;
    }

    public Request userAgent(final String agent) {
        this.request.setHeader(USER_AGENT, agent);
        return this;
    }

    public Request socketTimeout(final int timeout) {
        this.socketTimeout = timeout;
        return this;
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

    public byte[] body() {
        return body == null ? new byte[]{} : body;
    }

    public String query() {
        return query == null ? "" : query.toString();
    }

    public Header[] headers() {
        return request.getHeaders();
    }

    public String proxy() {
        return proxy == null ? null : proxy.toString();
    }

    public String method() {
        return request.getMethod();
    }

    public List<Cookie> cookies() {
        return cookies;
    }

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

    public String version() {
        return request.getVersion().toString();
    }
}
