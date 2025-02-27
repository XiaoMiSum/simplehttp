package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xiaomi
 * @date 2019/9/13 11:03
 */
public class Form {

    private final List<NameValuePair> data = new ArrayList<>();

    private Form() {

    }

    private Form(Map<String, Object> data) {
        add(data);
    }

    public static Form create() {
        return new Form();
    }

    public static Form create(Map<String, Object> data) {
        return new Form().add(data);
    }

    public Form add(String name, String value) {
        this.data.add(new BasicNameValuePair(name, value));
        return this;
    }

    public Form add(Map<String, Object> data) {
        data.forEach((key, value) -> this.add(key, value == null ? "" : String.valueOf(value)));
        return this;
    }

    public List<NameValuePair> build() {
        return this.data;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < data.size(); i++) {
            var pair = data.get(i);
            sb.append("\"").append(pair.getName()).append("\": ");
            if (pair.getValue() != null) {
                sb.append("\"").append(pair.getValue()).append("\"");
            } else {
                sb.append(pair.getValue());
            }
            if (i < data.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
