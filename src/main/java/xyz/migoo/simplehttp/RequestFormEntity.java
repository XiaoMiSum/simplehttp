package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author xiaomi
 * Created in 2021/7/21 19:52
 */
public class RequestFormEntity extends RequestEntity {

    public RequestFormEntity(Form form) {
        super(new UrlEncodedFormEntity(form.build(), StandardCharsets.UTF_8), form.toString().getBytes(StandardCharsets.UTF_8));
    }

    public RequestFormEntity(Map<String, Object> data) {
        this(Form.create(data));
    }
}
