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

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;

/**
 * JSON请求实体类，用于处理application/json格式的HTTP请求体
 * 继承自RequestEntity，可以将Map或JSON字符串转换为JSON格式的请求体
 *
 * @author xiaomi
 * Created in 2021/7/21 19:52
 */
public class RequestJsonEntity extends RequestEntity {

    /**
     * 根据JSON字符串构造一个新的JSON请求实体
     *
     * @param json JSON字符串
     */
    public RequestJsonEntity(String json) {
        this(json, APPLICATION_JSON);
    }

    /**
     * 根据Map数据构造一个新的JSON请求实体
     *
     * @param body JSON数据Map
     */
    public RequestJsonEntity(Map<String, ?> body) {
        this(toJson(body));
    }

    /**
     * 根据内容和内容类型构造一个新的JSON请求实体
     *
     * @param content     内容字符串
     * @param contentType 内容类型
     */
    public RequestJsonEntity(String content, ContentType contentType) {
        super(new StringEntity(content, contentType), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将Map对象转换为JSON字符串
     *
     * @param body 要转换的Map对象
     * @return JSON字符串
     */
    static String toJson(Map<?, ?> body) {
        var sb = new StringBuilder("{");
        for (Object key : body.keySet()) {
            var value = body.get(key);
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append("\"").append(key).append("\": ");
            sb.append(switch (value) {
                case Map<?, ?> object -> toJson(object);
                case List<?> objects -> toJson(objects);
                case null, default -> getValue(value);
            });
        }
        return sb.append("}").toString();
    }

    /**
     * 将List对象转换为JSON字符串
     *
     * @param list 要转换的List对象
     * @return JSON字符串
     */
    static String toJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (Object obj : list) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(switch (obj) {
                case Map<?, ?> object -> toJson(object);
                case List<?> objects -> toJson(objects);
                case null, default -> getValue(obj);
            });
        }
        return sb.append("]").toString();
    }

    /**
     * 获取值的字符串表示形式
     *
     * @param value 值对象
     * @return 值的字符串表示形式
     */
    private static Object getValue(Object value) {
        return value == null || value instanceof Number || value instanceof Boolean ? value : "\"" + value + "\"";
    }
}
