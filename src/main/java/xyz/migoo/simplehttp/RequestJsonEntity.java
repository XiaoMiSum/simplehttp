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

    public RequestJsonEntity(Map<String, ?> body) {
        this(toJson(body));
    }

    public RequestJsonEntity(String content, ContentType contentType) {
        super(new StringEntity(content, contentType), content.getBytes(StandardCharsets.UTF_8));
    }

    static String toJson(Map<?, ?> body) {
        var sb = new StringBuilder("{");
        for (Object key : body.keySet()) {
            var value = body.get(key);
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append("\"").append(key).append("\": ");
            sb.append(switch (value) {
                case Map<?, ?> object -> toJson(object);
                case List<?> objects -> toJson(objects);
                default -> getValue(value);
            });
        }
        return sb.append("}").toString();
    }

    static String toJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (Object obj : list) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(switch (obj) {
                case Map<?, ?> object -> toJson(object);
                case List<?> objects -> toJson(objects);
                default -> getValue(obj);
            });
        }
        return sb.append("]").toString();
    }

    private static Object getValue(Object value) {
        return value == null || value instanceof Number ? value : "\"" + value + "\"";
    }
}
