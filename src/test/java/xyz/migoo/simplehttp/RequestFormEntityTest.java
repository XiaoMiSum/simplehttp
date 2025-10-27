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

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * RequestFormEntity类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class RequestFormEntityTest {

    /**
     * 测试通过Form对象构造RequestFormEntity
     */
    @Test
    public void testConstructorWithForm() {
        // 创建Form对象并添加数据
        Form form = Form.create()
                .add("username", "testuser")
                .add("password", "secret123")
                .add("age", "25");

        // 使用Form对象创建RequestFormEntity
        RequestFormEntity formEntity = new RequestFormEntity(form);

        // 验证实例正确创建
        Assert.assertNotNull(formEntity);
        Assert.assertNotNull(formEntity.getEntity());
        Assert.assertNotNull(formEntity.getContent());
        Assert.assertTrue(formEntity.getEntity() instanceof UrlEncodedFormEntity);
    }

    /**
     * 测试通过Map数据构造RequestFormEntity
     */
    @Test
    public void testConstructorWithMap() {
        // 创建Map数据
        Map<String, Object> formData = new HashMap<>();
        formData.put("username", "testuser");
        formData.put("password", "secret123");
        formData.put("rememberMe", true);
        formData.put("age", 25);

        // 使用Map数据创建RequestFormEntity
        RequestFormEntity formEntity = new RequestFormEntity(formData);

        // 验证实例正确创建
        Assert.assertNotNull(formEntity);
        Assert.assertNotNull(formEntity.getEntity());
        Assert.assertNotNull(formEntity.getContent());
        Assert.assertTrue(formEntity.getEntity() instanceof UrlEncodedFormEntity);
    }

    /**
     * 测试包含null值的表单数据
     */
    @Test
    public void testFormWithNullValues() {
        // 创建包含null值的Map数据
        Map<String, Object> formData = new HashMap<>();
        formData.put("username", "testuser");
        formData.put("email", null);
        formData.put("age", 25);

        // 使用Map数据创建RequestFormEntity
        RequestFormEntity formEntity = new RequestFormEntity(formData);

        // 验证实例正确创建
        Assert.assertNotNull(formEntity);
        Assert.assertNotNull(formEntity.getEntity());
        Assert.assertNotNull(formEntity.getContent());
    }

    /**
     * 测试空表单数据
     */
    @Test
    public void testEmptyFormData() {
        // 创建空的Map数据
        Map<String, Object> formData = new HashMap<>();

        // 使用空Map数据创建RequestFormEntity
        RequestFormEntity formEntity = new RequestFormEntity(formData);

        // 验证实例正确创建
        Assert.assertNotNull(formEntity);
        Assert.assertNotNull(formEntity.getEntity());
        Assert.assertNotNull(formEntity.getContent());
    }

    /**
     * 测试通过RequestEntity.form方法创建RequestFormEntity实例
     */
    @Test
    public void testCreateRequestFormEntityViaFactoryMethod() {
        // 通过RequestEntity.form方法创建实例（使用Map）
        Map<String, Object> formData = new HashMap<>();
        formData.put("field1", "value1");
        formData.put("field2", 123);

        RequestEntity requestEntity = RequestEntity.form(formData);

        // 验证创建的实体是RequestFormEntity类型
        Assert.assertTrue(requestEntity instanceof RequestFormEntity);
        Assert.assertNotNull(requestEntity.getEntity());
        Assert.assertNotNull(requestEntity.getContent());

        // 通过RequestEntity.form方法创建实例（使用Customizer）
        RequestEntity requestEntity2 = RequestEntity.form(customizer -> {
            customizer.put("fieldA", "valueA");
            customizer.put("fieldB", 456);
        });

        // 验证创建的实体是RequestFormEntity类型
        Assert.assertTrue(requestEntity2 instanceof RequestFormEntity);
        Assert.assertNotNull(requestEntity2.getEntity());
        Assert.assertNotNull(requestEntity2.getContent());

        // 通过RequestEntity.form2方法创建实例（使用Form Customizer）
        RequestEntity requestEntity3 = RequestEntity.form2(form ->
                form.add("name", "test").add("value", "data"));

        // 验证创建的实体是RequestFormEntity类型
        Assert.assertTrue(requestEntity3 instanceof RequestFormEntity);
        Assert.assertNotNull(requestEntity3.getEntity());
        Assert.assertNotNull(requestEntity3.getContent());

        // 通过RequestEntity.form方法创建实例（使用Form对象）
        Form form = Form.create().add("key", "value");
        RequestEntity requestEntity4 = RequestEntity.form(form);

        // 验证创建的实体是RequestFormEntity类型
        Assert.assertTrue(requestEntity4 instanceof RequestFormEntity);
        Assert.assertNotNull(requestEntity4.getEntity());
        Assert.assertNotNull(requestEntity4.getContent());
    }

    /**
     * 测试表单内容编码
     */
    @Test
    public void testFormContentEncoding() {
        // 创建包含特殊字符的表单数据
        Map<String, Object> formData = new HashMap<>();
        formData.put("message", "Hello World!");
        formData.put("chinese", "你好世界");
        formData.put("symbols", "!@#$%^&*()");

        // 使用Map数据创建RequestFormEntity
        RequestFormEntity formEntity = new RequestFormEntity(formData);

        // 验证实例正确创建
        Assert.assertNotNull(formEntity);
        Assert.assertNotNull(formEntity.getEntity());
        Assert.assertNotNull(formEntity.getContent());

        // 验证内容不为空
        String content = new String(formEntity.getContent(), StandardCharsets.UTF_8);
        Assert.assertNotNull(content);
        Assert.assertFalse(content.isEmpty());
    }
}
