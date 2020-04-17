package xyz.migoo.simplehttp;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author xiaomi
 * @date 2019/9/13 10:58
 */
public class Request {

    private HttpRequest request;
    private Form form;
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

    public Request body(HttpEntity body) {
        ((EntityEnclosingHttpRequest) request).setEntity(body);
        return this;
    }

    public Request bodyString(String body) {
        Args.notBlank(body, "body");
        this.body = body;
        return this.body(new StringEntity(body, StandardCharsets.UTF_8));
    }

    public Request bodyJson(String body) {
        Args.notBlank(body, "body");
        this.body = body;
        this.addHeader("Content-Type", "application/json; charset=utf-8");
        return this.body(new StringEntity(body, StandardCharsets.UTF_8));
    }

    public Request bodyFile(String file) {
        Args.notBlank(file, "file");
        return this.bodyFile(new File(file));
    }

    public Request bodyFile(File file) {
        Args.notNull(file, "file");
        this.body = file.getPath();
        return this.body(new FileEntity(file, ContentType.DEFAULT_BINARY));
    }

    public Request data(Form data) {
        Args.notNull(data, "data");
        this.form = data;
        this.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        return this.body(new UrlEncodedFormEntity(form.build(), StandardCharsets.UTF_8));
    }

    public Request data(Map<String, String> data) {
        Args.notNull(data, "data");
        form = form == null ? Form.form() : form;
        data.forEach((k, v) -> form.add(k, v));
        return this.data(form);
    }

    public Request query(Map<String, String> query) {
        Args.notNull(query, "query");
        form = form == null ? Form.form() : form;
        query.forEach((k, v) -> form.add(k, v));
        return this.query(form);
    }

    public Request query(Form data) {
        Args.notNull(data, "data");
        this.form = data;
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

    private void query() throws URISyntaxException {
        if (!HttpPost.METHOD_NAME.equals(request.getMethod())
                && !HttpPut.METHOD_NAME.equals(request.getMethod()) && form != null) {
            request.setURI(new URIBuilder(request.getURI()).addParameters(form.build()).build());
        }
    }

    public Response execute() throws IOException, HttpException {
        return execute(Client.CLIENT);
    }

    public Response execute(CloseableHttpClient client) throws IOException, HttpException {
        try {
            this.setRequestConfig(client);
            this.query();
            return new Response().startTime(System.currentTimeMillis())
                    .response(client.execute(request, context))
                    .context(context).endTime(System.currentTimeMillis());
        } catch (URISyntaxException e) {
            throw new HttpException("uri parse error", e);
        }
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
        return form != null ? form.toString() : body;
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
        return uri.substring(0, uri.contains("?") ? uri.indexOf("?") : uri.length());
    }
}
