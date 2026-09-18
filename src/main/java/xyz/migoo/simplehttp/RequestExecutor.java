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

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import static org.apache.hc.core5.util.Timeout.ofSeconds;

/**
 * 请求执行器，基于 SimpleHttp 持有的共享客户端执行请求
 *
 * @author xiaomi
 */
class RequestExecutor {

    private final SimpleHttp client;

    RequestExecutor(SimpleHttp client) {
        this.client = client;
    }

    Response execute(Request request) throws IOException, URISyntaxException {
        HttpClientContext context = buildContext(request);
        applyQueryParameters(request);
        HttpProxy proxy = request.getProxy() != null ? request.getProxy() : client.getDefaultProxy();
        return client.httpClient(proxy).execute(request.httpRequest(), context, new Response.ResponseHandler(context));
    }

    private HttpClientContext buildContext(Request request) {
        var localContext = HttpClientContext.create();
        var builder = RequestConfig.custom();
        if (Objects.nonNull(request.getUseExpectContinue())) {
            builder.setExpectContinueEnabled(request.getUseExpectContinue());
        }
        if (Objects.nonNull(request.getSocketTimeout())) {
            builder.setConnectTimeout(ofSeconds(request.getSocketTimeout()));
        }
        builder.setConnectionRequestTimeout(ofSeconds(client.getConnectTimeout()));
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