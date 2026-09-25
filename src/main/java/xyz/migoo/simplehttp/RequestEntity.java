/*
 *
 *  * The MIT License (MIT)
 *  *
 *  * Copyright (c) 2025.  Lorem XiaoMiSum (mi_xiao@qq.com)
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining
 *  * a copy of this software and associated documentation files (the
 *  * 'Software'), to deal in the Software without restriction, including
 *  * without limitation the rights to use, copy, modify, merge, publish,
 *  * distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to
 *  * the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be
 *  * included in all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 *  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static xyz.migoo.simplehttp.RequestJsonEntity.toJson;

/**
 * 请求体工厂：全部构造入口都是静态工厂，不暴露实现子类，也不暴露底层 {@code HttpEntity}。
 * 返回值只作为 {@link Request#body(RequestEntity)} 的入参；请求体的实际内容读取走
 * {@link Exchange#request()} 的 {@link RequestSnapshot#body()}。
 *
 * @author xiaomi
 *         Created in 2021/7/21 19:50
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

    public static RequestEntity json(Customizer<Map<String, Object>> customizer) {
        Map<String, Object> body = HashMap.newHashMap(16);
        customizer.customize(body);
        return new RequestJsonEntity(body);
    }

    public static RequestEntity json(Map<String, ?> body) {
        return new RequestJsonEntity(body);
    }

    public static RequestEntity form(Map<String, Object> data) {
        return new RequestFormEntity(data);
    }

    public static RequestEntity form(Customizer<Form> customizer) {
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

    public static RequestEntity bytes(byte[] bytes, String mimeType) {
        return new RequestBytesEntity(bytes, mimeType);
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
        files.forEach(item -> {
            var file = new File(item.getValue());
            if (!file.exists()) {
                throw new IllegalArgumentException("File not found: " + item.getValue());
            }
            if (!file.canRead()) {
                throw new IllegalArgumentException("File not readable: " + item.getValue());
            }
            builder.addBinaryBody(item.getName(), file);
        });
        var content = Objects.isNull(data) ? Map.of("binary", files.toString())
                : Map.of("binary", Map.of("binary", files.toString()), "data", data);
        return new RequestBinaryEntity(builder.build(), toJson(content).getBytes(StandardCharsets.UTF_8));
    }

    HttpEntity getEntity() {
        return entity;
    }

    byte[] getContent() {
        return content;
    }

}
