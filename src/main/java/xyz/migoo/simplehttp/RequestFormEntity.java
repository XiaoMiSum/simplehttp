package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author xiaomi
 * Created in 2021/7/21 19:52
 */
public class RequestFormEntity extends BaseRequestEntity {

    public RequestFormEntity(Form form) {
        this.setEntity(new UrlEncodedFormEntity(form.build(), StandardCharsets.UTF_8));
        this.setContent(form.toString());
    }

    public RequestFormEntity(Map<String, Object> data) {
        Form form = new Form();
        data.forEach((key, value) -> form.add(key, value == null ? "" : String.valueOf(value)));
        this.setEntity(new UrlEncodedFormEntity(form.build(), StandardCharsets.UTF_8));
        this.setContent(form.toString());
    }
}
