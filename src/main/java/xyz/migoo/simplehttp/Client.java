package xyz.migoo.simplehttp;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

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
                SSLContext sslcontext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
                sslcontext.init(null, null, null);
                ssl = new SSLConnectionSocketFactory(sslcontext);
            } catch (SecurityException | KeyManagementException | NoSuchAlgorithmException ignore) {
            }
        }
        Registry<ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", ssl != null ? ssl : SSLConnectionSocketFactory.getSocketFactory())
                .build();
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(sfr);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(2);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setMaxTotal(20);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setValidateAfterInactivity(1000);
        CLIENT = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                .build();
    }

    public static void closeIdleConnections() {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.closeIdleConnections(0, TimeUnit.MICROSECONDS);
    }

    public static Client newClient(CloseableHttpClient httpClient) {
        return new Client(httpClient == null ? CLIENT : httpClient);
    }

    public static Client newClient() {
        return new Client(CLIENT);
    }

    private CloseableHttpClient httpClient;
    private volatile CookieStore cookieStore;

    private Client(CloseableHttpClient httpClient) {
        super();
        this.httpClient = httpClient;
    }

    public Response execute(Request request) throws HttpException {
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
