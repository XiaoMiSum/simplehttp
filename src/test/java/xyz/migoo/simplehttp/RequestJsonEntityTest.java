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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * RequestJsonEntity类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class RequestJsonEntityTest {

    /**
     * 测试通过JSON字符串构造RequestJsonEntity
     */
    @Test
    public void testConstructorWithString() {
        String json = "{\"name\":\"test\",\"value\":123}";
        RequestJsonEntity jsonEntity = new RequestJsonEntity(json);

        Assert.assertNotNull(jsonEntity);
        Assert.assertNotNull(jsonEntity.getEntity());
        Assert.assertNotNull(jsonEntity.getContent());
        Assert.assertEquals(new String(jsonEntity.getContent(), StandardCharsets.UTF_8), json);
    }

    /**
     * 测试通过Map数据构造RequestJsonEntity
     */
    @Test
    public void testConstructorWithMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);
        data.put("active", true);
        data.put("nullValue", null);

        RequestJsonEntity jsonEntity = new RequestJsonEntity(data);

        Assert.assertNotNull(jsonEntity);
        Assert.assertNotNull(jsonEntity.getEntity());
        Assert.assertNotNull(jsonEntity.getContent());
        String content = new String(jsonEntity.getContent(), StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("\"name\": \"test\""));
        Assert.assertTrue(content.contains("\"value\": 123"));
        Assert.assertTrue(content.contains("\"active\": true"));
        Assert.assertTrue(content.contains("\"nullValue\": null"));
    }

    /**
     * 测试通过内容和内容类型构造RequestJsonEntity
     */
    @Test
    public void testConstructorWithContentAndContentType() {
        String content = "{\"test\": \"data\"}";
        ContentType contentType = ContentType.APPLICATION_JSON;

        RequestJsonEntity jsonEntity = new RequestJsonEntity(content, contentType);

        Assert.assertNotNull(jsonEntity);
        Assert.assertNotNull(jsonEntity.getEntity());
        Assert.assertNotNull(jsonEntity.getContent());
        Assert.assertEquals(new String(jsonEntity.getContent(), StandardCharsets.UTF_8), content);
    }

    /**
     * 测试通过RequestEntity.json方法创建RequestJsonEntity实例
     */
    @Test
    public void testCreateRequestJsonEntityViaFactoryMethod() {
        // 通过RequestEntity.json方法创建实例（使用字符串）
        String json = "{\"field1\":\"value1\",\"field2\":123}";
        RequestEntity requestEntity = RequestEntity.json(json);

        Assert.assertTrue(requestEntity instanceof RequestJsonEntity);
        Assert.assertNotNull(requestEntity.getEntity());
        Assert.assertNotNull(requestEntity.getContent());

        // 通过RequestEntity.json方法创建实例（使用Supplier）
        RequestEntity requestEntity2 = RequestEntity.json(() -> json);

        Assert.assertTrue(requestEntity2 instanceof RequestJsonEntity);
        Assert.assertNotNull(requestEntity2.getEntity());
        Assert.assertNotNull(requestEntity2.getContent());

        // 通过RequestEntity.json方法创建实例（使用Customizer）
        RequestEntity requestEntity3 = RequestEntity.json(map -> {
            map.put("keyA", "valueA");
            map.put("keyB", 456);
        });

        Assert.assertTrue(requestEntity3 instanceof RequestJsonEntity);
        Assert.assertNotNull(requestEntity3.getEntity());
        Assert.assertNotNull(requestEntity3.getContent());

        // 通过RequestEntity.json方法创建实例（使用Map）
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);
        RequestEntity requestEntity4 = RequestEntity.json(data);

        Assert.assertTrue(requestEntity4 instanceof RequestJsonEntity);
        Assert.assertNotNull(requestEntity4.getEntity());
        Assert.assertNotNull(requestEntity4.getContent());
    }

    /**
     * 测试复杂嵌套结构的JSON序列化
     */
    @Test
    public void testComplexNestedStructure() {
        Map<String, Object> data = new HashMap<>();
        data.put("user", Map.of(
                "name", "John",
                "age", 30,
                "address", Map.of(
                        "street", "123 Main St",
                        "city", "New York"
                )
        ));
        data.put("hobbies", List.of("reading", "swimming"));
        data.put("active", true);
        data.put("score", 95.5);

        RequestJsonEntity jsonEntity = new RequestJsonEntity(data);

        Assert.assertNotNull(jsonEntity);
        Assert.assertNotNull(jsonEntity.getEntity());
        Assert.assertNotNull(jsonEntity.getContent());
        String content = new String(jsonEntity.getContent(), StandardCharsets.UTF_8);

        // 验证嵌套对象正确序列化
        Assert.assertTrue(content.contains("\"user\": {"));
        Assert.assertTrue(content.contains("\"name\": \"John\""));
        Assert.assertTrue(content.contains("\"age\": 30"));
        Assert.assertTrue(content.contains("\"hobbies\": ["));
        Assert.assertTrue(content.contains("\"reading\""));
        Assert.assertTrue(content.contains("\"active\": true"));
        Assert.assertTrue(content.contains("\"score\": 95.5"));
    }

    /**
     * 测试空Map的JSON序列化
     */
    @Test
    public void testEmptyMapSerialization() {
        Map<String, Object> emptyData = new HashMap<>();
        RequestJsonEntity jsonEntity = new RequestJsonEntity(emptyData);

        Assert.assertNotNull(jsonEntity);
        Assert.assertNotNull(jsonEntity.getEntity());
        Assert.assertNotNull(jsonEntity.getContent());
        String content = new String(jsonEntity.getContent(), StandardCharsets.UTF_8);
        Assert.assertEquals(content, "{}");
    }

    /**
     * 测试空List的JSON序列化
     */
    @Test
    public void testEmptyListSerialization() {
        Map<String, Object> data = new HashMap<>();
        data.put("items", new ArrayList<>());

        RequestJsonEntity jsonEntity = new RequestJsonEntity(data);

        Assert.assertNotNull(jsonEntity);
        Assert.assertNotNull(jsonEntity.getEntity());
        Assert.assertNotNull(jsonEntity.getContent());
        String content = new String(jsonEntity.getContent(), StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("\"items\": []"));
    }

    /**
     * 测试toJson方法处理各种数据类型
     */
    @Test
    public void testToJsonMethodWithDataTypes() {
        Map<String, Object> testData = new HashMap<>();
        testData.put("string", "test");
        testData.put("integer", 42);
        testData.put("double", 3.14);
        testData.put("boolean", true);
        testData.put("nullValue", null);
        testData.put("list", Arrays.asList("item1", "item2"));
        testData.put("nestedMap", Map.of("innerKey", "innerValue"));

        String json = RequestJsonEntity.toJson(testData);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("\"string\": \"test\""));
        Assert.assertTrue(json.contains("\"integer\": 42"));
        Assert.assertTrue(json.contains("\"double\": 3.14"));
        Assert.assertTrue(json.contains("\"boolean\": true"));
        Assert.assertTrue(json.contains("\"nullValue\": null"));
        Assert.assertTrue(json.contains("\"list\": ["));
        Assert.assertTrue(json.contains("\"nestedMap\": {"));
    }
}
