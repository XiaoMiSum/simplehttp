package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;

/**
 * @author xiaomi
 * Created in 2021/7/21 19:52
 */
public class RequestJsonEntity extends RequestEntity {

    public RequestJsonEntity(String json) {
        this(json, APPLICATION_JSON);
    }

    public RequestJsonEntity(Map<?, ?> body) {
        this(toJson(body));
    }

    public RequestJsonEntity(String content, ContentType contentType) {
        super(new StringEntity(content, contentType), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String toJson(Map<?, ?> body) {
        StringBuilder sb = new StringBuilder("{");
        for (Object key : body.keySet()) {
            Object value = body.get(key);
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append("\"").append(key).append("\": ");
            if (value instanceof Map) {
                sb.append(toJson((Map<?, ?>) value));
            } else if (value instanceof List) {
                sb.append(toJson((List<?>) value));
            } else {
                sb.append(getValue(value));
            }
        }
        return sb.append("}").toString();
    }

    private static String toJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (Object obj : list) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            if (obj instanceof Map) {
                sb.append(toJson((Map<?, ?>) obj));
            } else if (obj instanceof List) {
                sb.append(toJson((List<?>) obj));
            } else {
                sb.append(getValue(obj));
            }
        }
        return sb.append("]").toString();
    }

    private static Object getValue(Object value) {
        return value == null || value instanceof Number ? value : "\"" + value + "\"";
    }
}
