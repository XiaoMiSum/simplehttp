package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.net.URIBuilder;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Objects;

import static org.apache.hc.core5.util.Timeout.ofSeconds;

/**
 * 请求执行器：负责组装逐请求配置与执行上下文、派生真实发送的请求对象、
 * 并把执行结果/失败统一归档到 {@link Exchange}。
 *
 * @author xiaomi
 */
class RequestExecutor {

    private final SimpleHttp client;

    RequestExecutor(SimpleHttp client) {
        this.client = client;
    }

    Response execute(Request request) throws Exception {
        var maxCaptureBytes = request.getMaxCaptureBytes() != null
                ? request.getMaxCaptureBytes() : client.maxCaptureBytes();
        var exchange = Exchange.create(request, maxCaptureBytes, client.captureDirectory());
        var context = buildContext(request, exchange);

        HttpRequest actualRequest;
        try {
            actualRequest = buildHttpRequest(request);
            exchange.rememberSentUri(actualRequest.getUri());
        } catch (Exception t) {
            throw fail(request, exchange, t);
        }

        // 客户端已关闭时直接抛 IllegalStateException（调用方错误，不属于 HTTP 失败）
        var httpClient = client.httpClient();
        try {
            var response = httpClient.execute(actualRequest, context,
                    new Response.ResponseHandler(context));
            request.exchange(exchange);
            notifyComplete(request, exchange, null);
            return response;
        } catch (Exception t) {
            throw fail(request, exchange, t);
        }
    }

    /**
     * 执行失败：归档已采集信息、回调监听器、包装为 {@link HttpExecutionException}
     */
    private Exception fail(Request request, Exchange exchange, Throwable t) {
        exchange.markFailed(stageOf(t), t);
        request.exchange(exchange);
        notifyComplete(request, exchange, t);
        if (t instanceof HttpExecutionException e) {
            return e;
        }
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        var message = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
        return new HttpExecutionException("HTTP request failed at stage [" + exchange.stage() + "]: " + message,
                t, exchange);
    }

    private void notifyComplete(Request request, Exchange exchange, Throwable cause) {
        var listener = request.getExchangeListener() != null ? request.getExchangeListener() : client.exchangeListener();
        if (listener == null) {
            return;
        }
        try {
            if (cause == null) {
                listener.onComplete(exchange);
            } else {
                listener.onFailure(exchange, cause);
            }
        } catch (RuntimeException ignored) {
            // 监听器异常不影响请求结果
        }
    }

    /**
     * 组装逐请求上下文：采集属性、请求配置（超时/重定向/expect-continue）、代理与代理认证、Cookie
     */
    private HttpClientContext buildContext(Request request, Exchange exchange) {
        var context = HttpClientContext.create();
        context.setAttribute(Exchange.ATTRIBUTE, exchange);

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
            context.setCookieStore(cookieStore);
        }
        context.setRequestConfig(builder.build());

        var proxy = request.getProxy() != null ? request.getProxy() : client.getDefaultProxy();
        if (proxy != null) {
            context.setAttribute(ContextProxyRoutePlanner.PROXY_ATTRIBUTE, proxy);
            if (proxy.hasUsernameAndPassword()) {
                var provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(ContextProxyRoutePlanner.proxyHost(proxy)),
                        new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword().toCharArray()));
                context.setCredentialsProvider(provider);
            }
        }
        return context;
    }

    /**
     * 派生实际发送的请求对象：把查询参数合入 URI，<b>不回写</b>用户传入的 {@link Request}
     * （用户对象始终保留原始 URI，最终 URI 见 {@link Exchange#finalUri()}）。
     */
    private HttpRequest buildHttpRequest(Request request) throws URISyntaxException {
        var source = request.httpRequest();
        var query = request.getQuery();
        if (query == null || query.build().isEmpty()) {
            return source;
        }
        URI uri = new URIBuilder(source.getUri()).addParameters(query.build()).build();
        var actual = new HttpRequest(source.getMethod(), uri);
        for (Header header : source.getHeaders()) {
            actual.addHeader(header);
        }
        var entity = source.getEntity();
        if (entity != null) {
            actual.setEntity(entity);
        }
        var version = source.getVersion();
        if (version != null) {
            actual.setVersion(version);
        }
        return actual;
    }

    /**
     * 从异常链推断失败阶段，用于排障
     */
    static Exchange.Stage stageOf(Throwable throwable) {
        var cause = throwable;
        while (cause != null) {
            if (cause instanceof UnknownHostException) {
                return Exchange.Stage.DNS;
            }
            if (cause instanceof SSLException) {
                return Exchange.Stage.TLS;
            }
            if (cause instanceof ConnectTimeoutException || cause instanceof ConnectException
                    || cause instanceof NoRouteToHostException) {
                return Exchange.Stage.CONNECT;
            }
            if (cause instanceof SocketTimeoutException) {
                return Exchange.Stage.TIMEOUT;
            }
            if (cause instanceof NoHttpResponseException) {
                return Exchange.Stage.RESPONSE;
            }
            if (cause instanceof IOException) {
                return Exchange.Stage.RESPONSE;
            }
            cause = cause.getCause();
        }
        return Exchange.Stage.UNKNOWN;
    }
}
