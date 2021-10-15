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

    private Long startTime;
    private Long endTime;
    private String text;
    private int statusCode;
    private Header[] headers;
    private HttpClientContext context;

    public Response response(CloseableHttpResponse response) throws Exception {
        this.statusCode = response.getCode();
        this.headers = response.getHeaders();
        this.text = EntityUtils.toString(response.getEntity(), "UTF-8");
        response.close();
        return this;
    }

    Response context(HttpClientContext context) {
        this.context = context;
        return this;
    }

    Response startTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    Response endTime(long endTime) {
        this.endTime = endTime;
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

    public String text() {
        return text;
    }

    public List<Cookie> cookies() {
        return context != null ? context.getCookieStore().getCookies() : null;
    }
}
