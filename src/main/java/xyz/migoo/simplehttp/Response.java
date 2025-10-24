package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

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

    private String message;

    public Response(long startTime) {
        this.startTime = startTime;
    }

    public static void main(String[] args) {
        System.out.println(Path.of("D:\\bs", "ReadMe.md"));
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
        return text(() -> new String(bytes));
    }

    public String text(Supplier<String> supplier) {
        return supplier.get();
    }

    public String save(String path) {
        return text(() -> {
            try {
                return Files.write(Path.of(path), bytes, CREATE, TRUNCATE_EXISTING).toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<Cookie> cookies() {
        return Objects.nonNull(cookieStore) ? cookieStore.getCookies() : null;
    }

    public String version() {
        return version;
    }

    public String message() {
        return message;
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
            result.message = response.getReasonPhrase();
            return result;
        }
    }
}
