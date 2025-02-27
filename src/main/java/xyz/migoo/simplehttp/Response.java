package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

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
    private String version;
    private CookieStore cookieStore;

    public Response(long startTime) {
        this.startTime = startTime;
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

    public byte[] bytes() {
        return bytes;
    }

    public String text() {
        return new String(bytes);
    }

    public List<Cookie> cookies() {
        return Objects.nonNull(cookieStore) ? cookieStore.getCookies() : null;
    }

    public String version() {
        return version;
    }

    public static class ResponseHandler implements HttpClientResponseHandler<Response> {
        
        private final Response result = new Response(System.currentTimeMillis());

        public ResponseHandler(HttpClientContext context) {
            result.cookieStore = context.getCookieStore();
        }

        public Response handleResponse(ClassicHttpResponse response) throws IOException {
            result.endTime = System.currentTimeMillis();
            result.statusCode = response.getCode();
            result.headers = response.getHeaders();
            result.version = response.getVersion().toString();
            result.bytes = EntityUtils.toByteArray(response.getEntity());
            return result;
        }
    }
}
