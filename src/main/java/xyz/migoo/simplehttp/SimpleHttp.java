package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;

/**
 * HTTP客户端管理器，持有连接池和默认配置
 *
 * @author xiaomi
 */
public class SimpleHttp implements AutoCloseable {

    private static volatile SimpleHttp defaultInstance;

    private final PoolingHttpClientConnectionManager connectionManager;

    private final int connectTimeout;
    private final int readTimeout;
    private final boolean redirectsEnabled;
    private final HttpProxy defaultProxy;

    private SimpleHttp(Builder builder) {
        this.connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(DefaultClientTlsStrategy.createSystemDefault())
                .setMaxConnPerRoute(builder.maxConnectionsPerRoute)
                .setMaxConnTotal(builder.maxConnections)
                .setDefaultTlsConfig(TlsConfig.DEFAULT)
                .build();
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.redirectsEnabled = builder.redirectsEnabled;
        this.defaultProxy = builder.proxy;
    }

    public static SimpleHttp getDefault() {
        if (defaultInstance == null) {
            synchronized (SimpleHttp.class) {
                if (defaultInstance == null) {
                    defaultInstance = new SimpleHttp(new Builder());
                }
            }
        }
        return defaultInstance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Response execute(Request request) throws Exception {
        return new RequestExecutor(this).execute(request);
    }

    PoolingHttpClientConnectionManager connectionManager() {
        return connectionManager;
    }

    int getConnectTimeout() {
        return connectTimeout;
    }

    int getReadTimeout() {
        return readTimeout;
    }

    boolean isRedirectsEnabled() {
        return redirectsEnabled;
    }

    HttpProxy getDefaultProxy() {
        return defaultProxy;
    }

    @Override
    public void close() {
        if (connectionManager != null) {
            connectionManager.close();
        }
    }

    public static class Builder {
        private int maxConnections = 100;
        private int maxConnectionsPerRoute = 10;
        private int connectTimeout = 180;
        private int readTimeout = 180;
        private boolean redirectsEnabled = true;
        private HttpProxy proxy = null;

        public Builder maxConnections(int max) {
            this.maxConnections = max;
            return this;
        }

        public Builder maxConnectionsPerRoute(int max) {
            this.maxConnectionsPerRoute = max;
            return this;
        }

        public Builder connectTimeout(int seconds) {
            this.connectTimeout = seconds;
            return this;
        }

        public Builder readTimeout(int seconds) {
            this.readTimeout = seconds;
            return this;
        }

        public Builder redirectsEnabled(boolean enabled) {
            this.redirectsEnabled = enabled;
            return this;
        }

        public Builder proxy(String host, int port) {
            this.proxy = new HttpProxy(host, port);
            return this;
        }

        public Builder proxy(HttpProxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public SimpleHttp build() {
            return new SimpleHttp(this);
        }
    }
}
