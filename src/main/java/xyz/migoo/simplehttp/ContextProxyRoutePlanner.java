package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * 支持逐请求代理的路由规划器：代理配置从 {@link HttpContext} 读取，
 * 因此同一个 {@link SimpleHttp} 实例可以对不同请求使用不同代理（或不使用代理），
 * 无需为每个代理重建客户端。
 *
 * @author xiaomi
 */
final class ContextProxyRoutePlanner implements HttpRoutePlanner {

    /**
     * 代理配置在 {@link HttpContext} 中的属性名
     */
    static final String PROXY_ATTRIBUTE = "xyz.migoo.simplehttp.proxy";

    private static final SchemePortResolver SCHEME_PORT_RESOLVER = DefaultSchemePortResolver.INSTANCE;

    private final HttpRoutePlanner plain = new DefaultRoutePlanner(SCHEME_PORT_RESOLVER);

    @Override
    public HttpRoute determineRoute(final HttpHost target, final HttpContext context) throws HttpException {
        var attribute = context.getAttribute(PROXY_ATTRIBUTE);
        if (!(attribute instanceof HttpProxy proxy) || proxy.getHost() == null || proxy.getHost().isBlank()) {
            return plain.determineRoute(target, context);
        }
        return new DefaultProxyRoutePlanner(proxyHost(proxy), SCHEME_PORT_RESOLVER)
                .determineRoute(target, context);
    }

    /**
     * 把代理配置转换为 {@link HttpHost}，协议缺省为 http
     */
    static HttpHost proxyHost(HttpProxy proxy) {
        var scheme = proxy.getScheme();
        if (scheme == null || scheme.isBlank()) {
            scheme = "http";
        }
        return new HttpHost(scheme, proxy.getHost(), proxy.getPort());
    }
}
