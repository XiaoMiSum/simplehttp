package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.TimeValue;

import java.util.concurrent.TimeUnit;

/**
 * @author xiaomi
 * @date 2019/9/13 10:57
 */
public class Client {

    final static CloseableHttpClient CLIENT;
    private final static PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER;

    static {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(DefaultClientTlsStrategy.createSystemDefault())
                .setMaxConnPerRoute(2)
                .setMaxConnTotal(20)
                .setDefaultTlsConfig(TlsConfig.DEFAULT)
                .build();
        CLIENT = HttpClients.custom()
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                .build();
    }

    private final CloseableHttpClient httpClient;
    private volatile CookieStore cookieStore;

    private Client(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    static CloseableHttpClient httpClient(HttpProxy httpProxy) {
        var builder = HttpClients.custom()
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER);
        if (httpProxy != null) {
            var proxy = new HttpHost(httpProxy.getScheme(), httpProxy.getHost(), httpProxy.getPort());
            builder.setRoutePlanner(new DefaultProxyRoutePlanner(proxy));
            if (httpProxy.hasUsernameAndPassword()) {
                var provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(httpProxy.getUsername(), httpProxy.getPassword().toCharArray()));
                builder.setDefaultCredentialsProvider(provider);
            }
        }
        return builder.build();
    }

    public static void closeIdleConnections() {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.closeIdle(TimeValue.of(0, TimeUnit.MILLISECONDS));
    }

    public static Client newClient(CloseableHttpClient httpClient) {
        return new Client(httpClient == null ? CLIENT : httpClient);
    }

    public static Client newClient() {
        return new Client(CLIENT);
    }

    public Response execute(Request request) throws Exception {
        var context = HttpClientContext.create();
        if (this.cookieStore != null) {
            context.setCookieStore(this.cookieStore);
        }
        return request.execute(this.httpClient, context);
    }

    public Client cookieStore(final CookieStore cookieStore) {
        this.cookieStore = cookieStore;
        return this;
    }

    public Client clearCookies() {
        if (this.cookieStore != null) {
            this.cookieStore.clear();
        }
        return this;
    }
}
