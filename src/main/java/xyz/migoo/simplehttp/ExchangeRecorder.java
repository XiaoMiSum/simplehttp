package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;

import java.io.IOException;

/**
 * 执行链采集器，插在执行链最内层（ConnectExec 之内、MainClientExec 之外）。
 * <p>
 * 这个位置能看到且仅能看到真实信息：
 * <ul>
 *     <li>请求：当前跳的 URI（含 query、含重定向目标）、实际路由（target/proxy），
 *     并为 {@link WireRequestInterceptor} 挂载当前跳；</li>
 *     <li>响应：此时外层的 ContentCompressionExec <b>尚未</b> 解压，
 *     因此拿到的是服务端原样响应头（Content-Length/Content-Encoding 不会被删改）；</li>
 *     <li>重定向与自动重试：RedirectExec/HttpRequestRetryExec 在本处理器外层，
 *     每一跳都会重新进入本处理器，天然形成逐跳记录。</li>
 * </ul>
 * 响应体的线上原始字节通过 {@link TeeEntity} 旁路记录，不影响上层读取（含自动解压）。
 *
 * @author xiaomi
 */
final class ExchangeRecorder implements ExecChainHandler {

    static final String NAME = "simplehttp-exchange-recorder";

    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest request, final ExecChain.Scope scope,
                                       final ExecChain chain) throws IOException, HttpException {
        var context = scope.clientContext;
        var exchange = (Exchange) context.getAttribute(Exchange.ATTRIBUTE);
        if (exchange == null) {
            exchange = Exchange.detached(-1, null);
            context.setAttribute(Exchange.ATTRIBUTE, exchange);
        }
        var attempt = exchange.beginAttempt(request, scope.route);
        context.setAttribute(Exchange.ATTEMPT_ATTRIBUTE, attempt);

        var response = chain.proceed(request, scope);

        attempt.markResponse(response);
        exchange.markResponseHeaders();
        var entity = response.getEntity();
        if (entity != null) {
            response.setEntity(new TeeEntity(entity, attempt.responseBodySink()));
        }
        return response;
    }
}
