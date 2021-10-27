package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.List;
import java.util.Map;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;

/**
 * @author xiaomi
 * Created in 2021/7/21 19:52
 */
public class RequestJsonEntity extends BaseRequestEntity {

    public RequestJsonEntity(String json) {
        this(json, APPLICATION_JSON);
    }

    public RequestJsonEntity(Map<?, ?> body) {
        this.setContent(toJson(body));
        this.setEntity(new StringEntity(this.getContent(), APPLICATION_JSON));
    }

    public RequestJsonEntity(String content, ContentType contentType) {
        this.setContent(content);
        this.setEntity(new StringEntity(content, contentType));
    }

    private String toJson(Map<?, ?> body) {
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
                sb.append(toJson((Map<?, ?>) obj));
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
