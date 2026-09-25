package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;

import java.io.IOException;
import java.nio.file.Path;

import static org.apache.hc.core5.util.Timeout.ofSeconds;

/**
 * HTTP 客户端管理器：持有连接池与默认配置，并以<b>单个长生命周期</b> {@link CloseableHttpClient}
 * 执行所有请求（连接池可复用，不会因单次请求关闭而失效）。
 * <p>
 * 同时负责装配真实报文采集链路（见 {@link Exchange}）：
 * <ul>
 *     <li>{@link ExchangeRecorder}：逐跳记录线上原样响应、实际路由、重定向/重试链</li>
 *     <li>{@link WireRequestInterceptor}：记录发送前一刻的完整请求头与请求体</li>
 * </ul>
 * 链路可在 {@link Builder#captureEnabled(boolean)} 关闭，关闭后不安装上述拦截器，
 * 运行时开销与裸 HttpClient 相当。
 *
 * @author xiaomi
 */
public class SimpleHttp implements AutoCloseable {

    /**
     * 线上原始响应体默认采集上限（字节），超出即截断；{@code -1} 表示不限制
     */
    public static final long DEFAULT_MAX_CAPTURE_BYTES = 8L * 1024 * 1024;

    private static volatile SimpleHttp defaultInstance;

    private final PoolingHttpClientConnectionManager connectionManager;

    private final int connectTimeout;
    private final int readTimeout;
    private final boolean redirectsEnabled;
    private final HttpProxy defaultProxy;
    private final ExchangeListener exchangeListener;
    private final long maxCaptureBytes;
    private final Path captureDirectory;
    private final boolean captureEnabled;

    private volatile CloseableHttpClient httpClient;
    private volatile boolean closed;

    private SimpleHttp(Builder builder) {
        this.connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(DefaultClientTlsStrategy.createSystemDefault())
                .setMaxConnPerRoute(builder.maxConnectionsPerRoute)
                .setMaxConnTotal(builder.maxConnections)
                .setDefaultTlsConfig(TlsConfig.DEFAULT)
                // 连接（含 TLS 握手）超时：HC5 已把 RequestConfig.connectTimeout 迁移到 ConnectionConfig
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(ofSeconds(builder.connectTimeout))
                        .build())
                .build();
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.redirectsEnabled = builder.redirectsEnabled;
        this.defaultProxy = builder.proxy;
        this.exchangeListener = builder.exchangeListener;
        this.maxCaptureBytes = builder.maxCaptureBytes;
        this.captureDirectory = builder.captureDirectory;
        this.captureEnabled = builder.captureEnabled;
    }

    /**
     * 获取全局单例客户端。连接池由该单例持有，<b>不要调用其 {@link #close()}</b>，
     * 进程内所有基于 {@code Request#execute()} 的请求都会复用它。
     *
     * @return 全局单例
     */
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

    /**
     * 懒加载的共享客户端。连接池生命周期由本实例管理（{@link #close()}），
     * 客户端关闭不会带走连接池（{@code connectionManagerShared}）。
     */
    CloseableHttpClient httpClient() {
        var client = httpClient;
        if (client == null) {
            synchronized (this) {
                if (closed) {
                    throw new IllegalStateException("SimpleHttp client has been closed");
                }
                if (httpClient == null) {
                    httpClient = buildHttpClient();
                }
                client = httpClient;
            }
        }
        return client;
    }

    private CloseableHttpClient buildHttpClient() {
        var builder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 连接池归 SimpleHttp 所有，避免关闭客户端时把共享连接池一并 shutdown
                .setConnectionManagerShared(true)
                .setRedirectStrategy(new DefaultRedirectStrategy())
                // 逐请求代理：从执行上下文读取，无需为每个代理重建客户端
                .setRoutePlanner(new ContextProxyRoutePlanner());
        if (captureEnabled) {
            // 发送前一刻的最终请求头（含 Host/Content-Length 等客户端补全头）
            builder.addRequestInterceptorLast(new WireRequestInterceptor());
            // 执行链最内层：逐跳采集线上原样响应与实际路由
            builder.addExecInterceptorLast(ExchangeRecorder.NAME, new ExchangeRecorder());
        }
        return builder.build();
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

    ExchangeListener exchangeListener() {
        return exchangeListener;
    }

    long maxCaptureBytes() {
        return maxCaptureBytes;
    }

    Path captureDirectory() {
        return captureDirectory;
    }

    boolean isCaptureEnabled() {
        return captureEnabled;
    }

    /**
     * 关闭客户端与连接池（幂等）。关闭后继续执行请求会抛出 {@link IllegalStateException}。
     */
    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            var client = httpClient;
            httpClient = null;
            if (client != null) {
                try {
                    // connectionManagerShared=true：这里不会关闭共享连接池
                    client.close();
                } catch (IOException ignored) {
                    // 关闭失败不影响后续关闭连接池
                }
            }
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
        private ExchangeListener exchangeListener = null;
        private long maxCaptureBytes = DEFAULT_MAX_CAPTURE_BYTES;
        private Path captureDirectory = null;
        private boolean captureEnabled = true;

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

        /**
         * 交换记录监听器：每次请求成功/失败后回调（调用线程）
         */
        public Builder exchangeListener(ExchangeListener listener) {
            this.exchangeListener = listener;
            return this;
        }

        /**
         * 线上原始响应体/请求体的采集上限（字节），超出即截断并标记；{@code -1} 表示不限制
         */
        public Builder maxCaptureBytes(long maxCaptureBytes) {
            this.maxCaptureBytes = maxCaptureBytes;
            return this;
        }

        /**
         * 把线上原始响应体落盘到指定目录（完成后写入，内存随即释放）
         */
        public Builder captureToFile(Path directory) {
            this.captureDirectory = directory;
            return this;
        }

        /**
         * 是否装配真实报文采集链路，默认 {@code true}。
         * <p>
         * 设为 {@code false} 时不安装采集拦截器，因而不缓冲请求体/响应体、不复制请求头，
         * 运行时开销与裸 HttpClient 相当。代价是 {@link Response#exchange()} 只保留耗时、
         * HTTP 状态行、重定向链与失败阶段，{@link Exchange#attempts()} 为空，
         * {@link Response#rawHeaders()} / {@link Response#rawBytes()} 亦为空数组。
         *
         * @param enabled 是否开启报文采集
         * @return 构建器
         */
        public Builder captureEnabled(boolean enabled) {
            this.captureEnabled = enabled;
            return this;
        }

        public SimpleHttp build() {
            return new SimpleHttp(this);
        }
    }
}
