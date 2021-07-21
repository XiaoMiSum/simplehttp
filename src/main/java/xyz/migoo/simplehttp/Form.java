package xyz.migoo.simplehttp;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xiaomi
 * @date 2019/9/13 11:03
 */
public class Form {

    private final List<NameValuePair> data = new ArrayList<>();

    public static Form form() {
        return new Form();
    }

    public static Form form(Map<String, Object> data) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < data.size(); i++){
            NameValuePair pair = data.get(i);
            sb.append("\"").append(pair.getName()).append("\": ");
            if (pair.getValue() != null) {
                sb.append("\"").append(pair.getValue()).append("\"");
            } else {
                sb.append(pair.getValue());
            }
            if (i < data.size() -1){
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
