/*
 *
 *  * The MIT License (MIT)
 *  *
 *  * Copyright (c) 2025.  Lorem XiaoMiSum (mi_xiao@qq.com)
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining
 *  * a copy of this software and associated documentation files (the
 *  * 'Software'), to deal in the Software without restriction, including
 *  * without limitation the rights to use, copy, modify, merge, publish,
 *  * distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to
 *  * the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be
 *  * included in all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 *  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * HTTP客户端管理器，持有连接池、共享客户端和默认配置
 * <p>
 * 无代理请求复用默认客户端；带代理的请求按代理配置缓存对应客户端，所有客户端共享同一个连接池。
 *
 * @author xiaomi
 */
public class SimpleHttp implements AutoCloseable {

    private static volatile SimpleHttp defaultInstance;

    private final PoolingHttpClientConnectionManager connectionManager;

    private final CloseableHttpClient httpClient;

    private final ConcurrentMap<ProxyKey, CloseableHttpClient> proxiedClients = new ConcurrentHashMap<>();

    private final int connectTimeout;
    private final int readTimeout;
    private final boolean redirectsEnabled;
    private final HttpProxy defaultProxy;

    private SimpleHttp(Builder builder) {
        Args.check(builder.maxConnections > 0, "maxConnections must be positive");
        Args.check(builder.maxConnectionsPerRoute > 0, "maxConnectionsPerRoute must be positive");
        Args.check(builder.connectTimeout >= 0, "connectTimeout must not be negative");
        Args.check(builder.readTimeout >= 0, "readTimeout must not be negative");
        this.connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(DefaultClientTlsStrategy.createSystemDefault())
                .setMaxConnPerRoute(builder.maxConnectionsPerRoute)
                .setMaxConnTotal(builder.maxConnections)
                .setDefaultTlsConfig(TlsConfig.DEFAULT)
                .build();
        this.httpClient = buildHttpClient(null);
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

    public Response execute(Request request) throws IOException, URISyntaxException {
        return new RequestExecutor(this).execute(request);
    }

    CloseableHttpClient httpClient(HttpProxy proxy) {
        if (proxy == null || proxy.getHost() == null) {
            return httpClient;
        }
        return proxiedClients.computeIfAbsent(new ProxyKey(proxy), key -> buildHttpClient(key));
    }

    PoolingHttpClientConnectionManager connectionManager() {
        return connectionManager;
    }

    private CloseableHttpClient buildHttpClient(ProxyKey proxy) {
        var builder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setRedirectStrategy(new DefaultRedirectStrategy());
        if (proxy != null) {
            var httpHost = new HttpHost(
                    proxy.scheme(),
                    proxy.host(),
                    proxy.port());
            builder.setRoutePlanner(new DefaultProxyRoutePlanner(httpHost));
            if (proxy.username() != null && !proxy.username().isEmpty()
                    && proxy.password() != null && !proxy.password().isEmpty()) {
                var provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(httpHost),
                        new UsernamePasswordCredentials(proxy.username(), proxy.password().toCharArray()));
                builder.setDefaultCredentialsProvider(provider);
            }
        }
        return builder.build();
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
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            proxiedClients.values().forEach(simplehttp -> {
                try {
                    simplehttp.close();
                } catch (IOException ignored) {
                    // 关闭失败无需抛异常，连接池会随 JVM 回收
                }
            });
        } catch (IOException ignored) {
            // 关闭失败无需抛异常，连接池会随 JVM 回收
        }
        if (this == defaultInstance) {
            defaultInstance = null;
        }
    }

    /**
     * 代理配置的缓存键，用于缓存对应的 HTTP 客户端
     *
     * @param scheme   代理协议
     * @param host     代理主机地址
     * @param port     代理端口号
     * @param username 认证用户名
     * @param password 认证密码
     */
    private record ProxyKey(String scheme, String host, int port, String username, String password) {

        ProxyKey(HttpProxy proxy) {
            this(proxy.getScheme() != null ? proxy.getScheme() : URIScheme.HTTP.id,
                    proxy.getHost(),
                    proxy.getPort() != null ? proxy.getPort() : -1,
                    proxy.getUsername(),
                    proxy.getPassword());
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