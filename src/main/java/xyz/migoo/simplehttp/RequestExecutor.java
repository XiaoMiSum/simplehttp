package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;
import java.util.Objects;

import static org.apache.hc.core5.util.Timeout.ofSeconds;

/**
 * @author xiaomi
 */
class RequestExecutor {

    private final SimpleHttp client;

    RequestExecutor(SimpleHttp client) {
        this.client = client;
    }

    Response execute(Request request) throws Exception {
        try (CloseableHttpClient httpClient = buildHttpClient(request)) {
            HttpClientContext context = buildContext(request);
            applyQueryParameters(request);
            return httpClient.execute(request.httpRequest(), context, new Response.ResponseHandler(context));
        }
    }

    private CloseableHttpClient buildHttpClient(Request request) {
        var builder = HttpClients.custom()
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setConnectionManager(client.connectionManager());
        HttpProxy proxy = request.getProxy() != null ? request.getProxy() : client.getDefaultProxy();
        if (proxy != null) {
            var httpHost = new HttpHost(proxy.getScheme(), proxy.getHost(), proxy.getPort());
            builder.setRoutePlanner(new DefaultProxyRoutePlanner(httpHost));
            if (proxy.hasUsernameAndPassword()) {
                var provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(httpHost),
                        new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword().toCharArray()));
                builder.setDefaultCredentialsProvider(provider);
            }
        }
        return builder.build();
    }

    private HttpClientContext buildContext(Request request) {
        var localContext = HttpClientContext.create();
        var builder = RequestConfig.custom();
        builder.setExpectContinueEnabled(
                Objects.nonNull(request.getUseExpectContinue()) ? request.getUseExpectContinue() : false);
        builder.setConnectionRequestTimeout(ofSeconds(
                Objects.nonNull(request.getSocketTimeout()) ? request.getSocketTimeout() : client.getConnectTimeout()));
        builder.setResponseTimeout(ofSeconds(
                Objects.nonNull(request.getReadTimeout()) ? request.getReadTimeout() : client.getReadTimeout()));
        builder.setRedirectsEnabled(Objects.nonNull(request.getRedirectsEnabled()) ? request.getRedirectsEnabled()
                : client.isRedirectsEnabled());
        if (Objects.nonNull(request.getCookies()) && !request.getCookies().isEmpty()) {
            var cookieStore = new BasicCookieStore();
            request.getCookies().forEach(cookieStore::addCookie);
            localContext.setCookieStore(cookieStore);
        }
        localContext.setRequestConfig(builder.build());
        return localContext;
    }

    private void applyQueryParameters(Request request) throws URISyntaxException {
        Form query = request.getQuery();
        if (query != null && !query.build().isEmpty()) {
            request.httpRequest().setUri(
                    new URIBuilder(request.httpRequest().getUri())
                            .addParameters(query.build())
                            .build());
        }
    }
}
