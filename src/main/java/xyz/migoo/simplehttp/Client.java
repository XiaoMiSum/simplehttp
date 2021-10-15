package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLInitializationException;
import org.apache.hc.core5.util.TimeValue;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaomi
 * @date 2019/9/13 10:57
 */
public class Client {

    private final static PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER;
    final static CloseableHttpClient CLIENT;

    static {
        LayeredConnectionSocketFactory ssl = null;
        try {
            ssl = SSLConnectionSocketFactory.getSystemSocketFactory();
        } catch (SSLInitializationException ex) {
            try {
                SSLContext sslcontext = SSLContext.getInstance("SSL");
                sslcontext.init(null, null, null);
                ssl = new SSLConnectionSocketFactory(sslcontext);
            } catch (SecurityException | KeyManagementException | NoSuchAlgorithmException ignore) {
            }
        }

        POOLING_HTTP_CLIENT_CONNECTION_MANAGER = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(ssl != null ? ssl : SSLConnectionSocketFactory.getSocketFactory())
                .setMaxConnPerRoute(2)
                .setMaxConnTotal(20)
                .setValidateAfterInactivity(TimeValue.ofSeconds(1))
                .build();
        CLIENT = HttpClients.custom()
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                .build();
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

    private final CloseableHttpClient httpClient;
    private volatile CookieStore cookieStore;

    private Client(CloseableHttpClient httpClient) {
        super();
        this.httpClient = httpClient;
    }

    public Response execute(Request request) throws Exception {
        if (this.cookieStore != null) {
            HttpClientContext context = HttpClientContext.create();
            context.setAttribute(HttpClientContext.COOKIE_STORE, this.cookieStore);
            request.context(context);
        }
        return request.execute(this.httpClient);
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
