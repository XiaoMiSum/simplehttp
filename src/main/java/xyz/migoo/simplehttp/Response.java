package xyz.migoo.simplehttp;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
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

    public Response response(CloseableHttpResponse response) throws IOException {
        this.statusCode = response.getStatusLine().getStatusCode();
        this.headers = response.getAllHeaders();
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

    public long startTime(){
        return startTime;
    }

    public long endTime(){
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
