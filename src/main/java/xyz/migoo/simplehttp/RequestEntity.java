package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

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

    public static RequestEntity json(Supplier<String> supplier) {
        return new RequestJsonEntity(supplier.get());
    }

    public static RequestEntity json(String json) {
        return new RequestJsonEntity(json);
    }

    public static RequestEntity json(Customizer<Map<String, Object>> customizer) {
        Map<String, Object> body = HashMap.newHashMap(16);
        customizer.customize(body);
        return new RequestJsonEntity(body);
    }

    public static RequestEntity json(Map<String, ?> body) {
        return new RequestJsonEntity(body);
    }

    public static RequestEntity form(Customizer<Map<String, Object>> customizer) {
        Map<String, Object> data = HashMap.newHashMap(16);
        customizer.customize(data);
        return new RequestFormEntity(data);
    }

    public static RequestEntity form(Map<String, Object> data) {
        return new RequestFormEntity(data);
    }

    public static RequestEntity form2(Customizer<Form> customizer) {
        var form = Form.create();
        customizer.customize(form);
        return new RequestFormEntity(form);
    }

    public static RequestEntity form(Form form) {
        return new RequestFormEntity(form);
    }

    public static RequestEntity text(String text) {
        return new RequestBytesEntity(text.getBytes(StandardCharsets.UTF_8), "text/plain");
    }

    public static RequestEntity proto(byte[] bytes) {
        return new RequestBytesEntity(bytes, "application/x-protobuf");
    }

    public static RequestEntity proto(Supplier<byte[]> supplier) {
        return new RequestBytesEntity(supplier.get(), "application/x-protobuf");
    }

    public static RequestEntity bytes(byte[] bytes, String mimeType) {
        return new RequestBytesEntity(bytes, mimeType);
    }

    public static RequestEntity binary(Supplier<NameValuePair> supplier) {
        return binary(supplier.get(), null);
    }

    public static RequestEntity binary(NameValuePair fileNvp) {
        return binary(fileNvp, null);
    }

    public static RequestEntity binary(Customizer<List<NameValuePair>> customizer) {
        var files = new ArrayList<NameValuePair>();
        customizer.customize(files);
        return binary(files, null);
    }

    public static RequestEntity binary(List<NameValuePair> files) {
        return binary(files, null);
    }

    public static RequestEntity binary(Supplier<NameValuePair> supplier, Customizer<Map<String, Object>> customizer) {
        Map<String, Object> data = HashMap.newHashMap(16);
        customizer.customize(data);
        return binary(List.of(supplier.get()), data);
    }

    public static RequestEntity binary(NameValuePair fileNvp, Map<String, Object> data) {
        return binary(List.of(fileNvp), data);
    }

    public static RequestEntity binary(Customizer<List<NameValuePair>> f, Customizer<Map<String, Object>> d) {
        var files = new ArrayList<NameValuePair>();
        f.customize(files);
        Map<String, Object> data = HashMap.newHashMap(16);
        d.customize(data);
        return binary(files, data);
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
