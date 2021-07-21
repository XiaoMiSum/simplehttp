package xyz.migoo.simplehttp;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;

import java.net.URI;
import java.util.List;

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
        return new Request(new EntityEnclosingHttpRequest(HttpPost.METHOD_NAME, URI.create(url)));
    }

    public static Request put(String url) {
        return new Request(new EntityEnclosingHttpRequest(HttpPut.METHOD_NAME, URI.create(url)));
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

    protected Request(HttpRequest request) {
        super();
        this.request = request;
    }

    public Request body(RequestEntity entity) {
        this.body = entity.getContent();
        ((EntityEnclosingHttpRequest) request).setEntity(entity.getEntity());
        return this;
    }

    public Request query(Form query) {
        Args.notNull(query, "query");
        this.query = query;
        return this;
    }

    public Request context(HttpClientContext context) {
        Args.notNull(context, "context");
        this.context = context;
        return this;
    }

    public Request cookies(CookieStore cookieStore) {
        Args.notNull(cookieStore, "cookies");
        context = context == null ? HttpClientContext.create() : context;
        context.setCookieStore(cookieStore);
        return this;
    }

    public Request cookies(List<Cookie> cookies) {
        Args.notNull(cookies, "cookies");
        context = context == null ? HttpClientContext.create() : context;
        CookieStore cookieStore = new BasicCookieStore();
        cookies.forEach(cookieStore::addCookie);
        context.setCookieStore(cookieStore);
        return this;
    }

    public Request headers(List<Header> headers) {
        Args.notNull(headers, "headers");
        headers.forEach(request::addHeader);
        return this;
    }

    public Request addHeader(Header header) {
        request.addHeader(header);
        return this;
    }

    public Request addHeader(String name, String value) {
        return this.addHeader(new BasicHeader(name, value));
    }

    public Request proxy(HttpProxy proxy) throws MalformedChallengeException {
        if (proxy.hasUsernameAndPassword()) {
            HttpHost host = new HttpHost(proxy.getHost(), proxy.getPort());
            BasicScheme proxyAuth = new BasicScheme();
            proxyAuth.processChallenge(new BasicHeader(AUTH.PROXY_AUTH, "BASIC realm=default"));
            BasicAuthCache authCache = new BasicAuthCache();
            authCache.put(host, proxyAuth);
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword()));
            context = context == null ? HttpClientContext.create() : context;
            context.setAuthCache(authCache);
            context.setCredentialsProvider(provider);
        } else {
            this.proxy = new HttpHost(proxy.getHost(), proxy.getPort());
        }
        return this;
    }

    public Response execute() throws Exception {
        return execute(Client.CLIENT);
    }

    public Response execute(CloseableHttpClient client) throws Exception {
        this.setRequestConfig(client);
        if (query != null && query.build().size() > 0) {
            request.setURI(new URIBuilder(request.getURI()).addParameters(query.build()).build());
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
            builder.setSocketTimeout(this.socketTimeout * 1000);
        }
        if (this.connectTimeout != null) {
            builder.setConnectTimeout(this.connectTimeout * 1000);
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
        this.request.setHeader(HTTP.USER_AGENT, agent);
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
        return request.getAllHeaders();
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
        return request.getURI().toString();
    }

    public String uriNotContainsParam() {
        String uri = uri();
        return uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri;
    }
}
