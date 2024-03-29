package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.util.List;

/**
 * @author xiaomi
 * @date 2019/9/13 11:01
 */
public class Response {

    private final Long startTime;
    private Long endTime;
    private byte[] bytes;
    private int statusCode;
    private Header[] headers;
    private HttpClientContext context;

    public Response() {
        this.startTime = System.currentTimeMillis();
    }

    public Response response(CloseableHttpResponse response) throws Exception {
        this.endTime = System.currentTimeMillis();
        this.statusCode = response.getCode();
        this.headers = response.getHeaders();
        this.bytes = EntityUtils.toByteArray(response.getEntity());
        return this;
    }

    Response context(HttpClientContext context) {
        this.context = context;
        return this;
    }

    public int statusCode() {
        return statusCode;
    }

    public Header[] headers() {
        return headers;
    }

    public long startTime() {
        return startTime;
    }

    public long endTime() {
        return endTime;
    }

    public long duration() {
        return endTime - startTime;
    }

    public HttpClientContext getContext() {
        return context;
    }

    public byte[] bytes() {
        return bytes;
    }

    public String text() {
        return new String(bytes);
    }


    public List<Cookie> cookies() {
        return context != null && context.getCookieStore() != null ? context.getCookieStore().getCookies() : null;
    }
}
