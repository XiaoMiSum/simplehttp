package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.HttpEntity;

import java.util.Map;

/**
 * @author xiaomi
 * Created in 2021/7/21 19:50
 */
public abstract class RequestEntity {

    private final byte[] content;
    private final HttpEntity entity;

    protected RequestEntity(HttpEntity entity, byte[] content) {
        this.entity = entity;
        this.content = content;
    }

    public static RequestEntity json(String json) {
        return new RequestJsonEntity(json);
    }

    public static RequestEntity json(Map<?, ?> body) {
        return new RequestJsonEntity(body);
    }

    public static RequestEntity form(Map<String, Object> data) {
        return new RequestFormEntity(data);
    }

    public static RequestEntity form(Form form) {
        return new RequestFormEntity(form);
    }

    public static RequestEntity bytes(byte[] bytes, String mimeType) {
        return new RequestBytesEntity(bytes, mimeType);
    }

    public HttpEntity getEntity() {
        return entity;
    }

    public byte[] getContent() {
        return content;
    }


}
