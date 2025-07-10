package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static xyz.migoo.simplehttp.RequestJsonEntity.toJson;

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

    public static RequestEntity json(Map<String, ?> body) {
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

    public static RequestEntity binary(NameValuePair fileNvp) {
        return binary(fileNvp, null);
    }

    public static RequestEntity binary(List<NameValuePair> files) {
        return binary(files, null);
    }

    public static RequestEntity binary(NameValuePair fileNvp, Map<String, Object> data) {
        return binary(List.of(fileNvp), data);
    }

    public static RequestEntity binary(List<NameValuePair> files, Map<String, Object> data) {
        var builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);
        if (data != null && !data.isEmpty()) {
            data.forEach((key, value) -> builder.addTextBody(key, Optional.ofNullable(value).orElse("").toString()));
        }
        files.forEach(item -> builder.addBinaryBody(item.getName(), new File(item.getValue())));
        var content = Objects.isNull(data) ? Map.of("binary", files.toString())
                : Map.of("binary", Map.of("binary", files.toString()), "data", data);
        return new RequestBinaryEntity(builder.build(), toJson(content).getBytes(StandardCharsets.UTF_8));
    }

    public HttpEntity getEntity() {
        return entity;
    }

    public byte[] getContent() {
        return content;
    }

}
