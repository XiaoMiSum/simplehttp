package xyz.migoo.simplehttp;

import org.apache.http.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

/**
 * @author xiaomi
 * Created in 2021/7/21 19:52
 */
public class RequestJsonEntity extends RequestEntity {

    public RequestJsonEntity(String json) {
        this.setContent(json);
        this.setEntity(new StringEntity(json, APPLICATION_JSON));
    }

    public RequestJsonEntity(Map<String, Object> body) {
        this.setContent(toJson(body));
        this.setEntity(new StringEntity(this.getContent(), StandardCharsets.UTF_8));
    }

    private String toJson(Map<String, Object> body) {
        StringBuilder sb = new StringBuilder("{");
        for (String key : body.keySet()) {
            Object value = body.get(key);
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append("\"").append(key).append("\": ");
            if (value instanceof Map) {
                sb.append(toJson((Map<String, Object>) value));
            } else if (value instanceof List) {
                sb.append(listToString((List<?>) value));
            } else {
                sb.append(getValue(value));
            }
        }
        return sb.append("}").toString();
    }

    private String listToString(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (Object obj : list) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            if (obj instanceof Map) {
                sb.append(toJson((Map<String, Object>) obj));
            } else if (obj instanceof List) {
                sb.append(listToString((List<?>) obj));
            } else {
                sb.append(getValue(obj));
            }
        }
        return sb.append("]").toString();
    }

    public Object getValue(Object value) {
        return value == null || value instanceof Number ? value : "\"" + value + "\"";
    }
}
