package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Args;
import org.apache.hc.core5.util.Timeout;

import java.net.URI;
import java.util.List;

import static org.apache.hc.core5.http.HttpHeaders.USER_AGENT;

/**
 * @author xiaomi
 * @date 2019/9/13 10:58
 */
public class Request {

    private HttpRequest request;
    private Form query;
    private String body;
    private HttpClientContext context;
    private Boolean useExpectContinue;
    private Integer socketTimeout;
    private Integer connectTimeout;
    private HttpHost proxy;

    public static Request get(String url) {
        return new Request(new HttpRequest(HttpGet.METHOD_NAME, URI.create(url)));
    }

    public static Request post(String url) {
        return new Request(new HttpRequest(HttpPost.METHOD_NAME, URI.create(url)));
    }

    public static Request put(String url) {
        return new Request(new HttpRequest(HttpPut.METHOD_NAME, URI.create(url)));
    }

    public static Request delete(String url) {
        return new Request(new HttpRequest(HttpDelete.METHOD_NAME, URI.create(url)));
    }

    public static Request head(String url) {
        return new Request(new HttpRequest(HttpHead.METHOD_NAME, URI.create(url)));
    }

    public static Request patch(String url) {
        return new Request(new HttpRequest(HttpPatch.METHOD_NAME, URI.create(url)));
    }

    public static Request trace(String url) {
        return new Request(new HttpRequest(HttpTrace.METHOD_NAME, URI.create(url)));
    }

    public static Request options(String url) {
        return new Request(new HttpRequest(HttpOptions.METHOD_NAME, URI.create(url)));
    }

    protected Request() {
        super();
    }

    private Request(HttpRequest request) {
        super();
        this.request = request;
    }

    public Request body(BaseRequestEntity entity) {
        this.body = entity.getContent();
        request.setEntity(entity.getEntity());
        return this;
    }

    public Request query(Form query) {
        this.query = Args.notNull(query, "query");
        return this;
    }

    public Request context(HttpClientContext context) {
        this.context = Args.notNull(context, "context");
        return this;
    }

    public Request cookies(CookieStore cookieStore) {
        context = context == null ? HttpClientContext.create() : context;
        context.setCookieStore(Args.notNull(cookieStore, "cookies"));
        return this;
    }

    public Request cookies(List<Cookie> cookies) {
        context = context == null ? HttpClientContext.create() : context;
        CookieStore cookieStore = new BasicCookieStore();
        Args.notNull(cookies, "cookies").forEach(cookieStore::addCookie);
        context.setCookieStore(cookieStore);
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
        this.proxy = new HttpHost(proxy.getScheme(), proxy.getHost(), proxy.getPort());
        if (proxy.hasUsernameAndPassword()) {
            BasicCredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(new AuthScope(this.proxy), new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword().toCharArray()));
            context = context == null ? HttpClientContext.create() : context;
            context.setCredentialsProvider(provider);
        }
        return this;
    }

    public Response execute() throws Exception {
        return execute(Client.CLIENT);
    }

    public Response execute(CloseableHttpClient client) throws Exception {
        this.setRequestConfig(client);
        if (query != null && query.build().size() > 0) {
            request.setUri(new URIBuilder(request.getUri()).addParameters(query.build()).build());
        }
        return new Response().startTime(System.currentTimeMillis())
                .response(client.execute(request, context))
                .context(context).endTime(System.currentTimeMillis());
    }

    private void setRequestConfig(CloseableHttpClient client) {
        final RequestConfig.Builder builder;
        if (client instanceof Configurable) {
            builder = RequestConfig.copy(((Configurable) client).getConfig());
        } else {
            builder = RequestConfig.custom();
        }
        if (this.useExpectContinue != null) {
            builder.setExpectContinueEnabled(this.useExpectContinue);
        }
        if (this.socketTimeout != null) {
            builder.setConnectionRequestTimeout(Timeout.ofSeconds(socketTimeout));
        }
        if (this.connectTimeout != null) {
            builder.setConnectTimeout(Timeout.ofSeconds(connectTimeout));
        }
        if (this.proxy != null) {
            builder.setProxy(this.proxy);
        }
        final RequestConfig config = builder.setRedirectsEnabled(true).build();
        this.request.setConfig(config);
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

    public Request connectTimeout(final int timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    protected Request request(HttpRequest request) {
        this.request = request;
        return this;
    }

    public String body() {
        return body == null ? "" : body;
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

    public HttpClientContext context() {
        return context;
    }

    public String method() {
        return request.getMethod();
    }

    public String uri() {
        return request.getRequestUri();
    }

    public String uriNotContainsParam() {
        String uri = uri();
        return uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri;
    }
}
