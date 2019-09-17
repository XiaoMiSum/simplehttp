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
    private HttpClientContext context;
    private CloseableHttpResponse response;

    public Response response(CloseableHttpResponse response) {
        this.response = response;
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
        return response != null ? response.getStatusLine().getStatusCode() : -1;
    }

    public Header[] headers() {
        return response.getAllHeaders();
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
        try {
           return text = text == null ? EntityUtils.toString(response.getEntity()) : text;
        } catch (IOException e) {
            return null;
        }
    }

    public List<Cookie> cookies() {
        return context != null ? context.getCookieStore().getCookies() : null;
    }
}
