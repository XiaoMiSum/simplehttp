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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class FormTest {

    /**
     * 测试创建空表单实例
     */
    @Test
    public void testCreateEmptyForm() {
        Form form = Form.create();
        Assert.assertNotNull(form);

        List<?> dataList = form.build();
        Assert.assertNotNull(dataList);
        Assert.assertTrue(dataList.isEmpty());
    }

    /**
     * 测试根据Map数据创建表单实例
     */
    @Test
    public void testCreateFormWithMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 123);
        data.put("key3", true);

        Form form = Form.create(data);
        Assert.assertNotNull(form);

        List<?> dataList = form.build();
        Assert.assertNotNull(dataList);
        Assert.assertEquals(dataList.size(), 3);
    }

    /**
     * 测试添加键值对到表单
     */
    @Test
    public void testAddNameValuePair() {
        Form form = Form.create();
        Form result = form.add("name", "test");

        // 验证链式调用
        Assert.assertEquals(form, result);

        List<?> dataList = form.build();
        Assert.assertEquals(dataList.size(), 1);
    }

    /**
     * 测试添加Map数据到表单
     */
    @Test
    public void testAddMapData() {
        Form form = Form.create();

        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 123);
        data.put("key3", true);

        Form result = form.add(data);

        // 验证链式调用
        Assert.assertEquals(form, result);

        List<?> dataList = form.build();
        Assert.assertEquals(dataList.size(), 3);
    }

    /**
     * 测试添加包含null值的Map数据到表单
     */
    @Test
    public void testAddMapDataWithNullValue() {
        Form form = Form.create();

        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", null);

        form.add(data);

        List<?> dataList = form.build();
        Assert.assertEquals(dataList.size(), 2);
    }

    /**
     * 测试构建表单数据为NameValuePair列表
     */
    @Test
    public void testBuild() {
        Form form = Form.create();
        form.add("name", "test");
        form.add("value", "123");

        List<?> dataList = form.build();
        Assert.assertNotNull(dataList);
        Assert.assertEquals(dataList.size(), 2);
    }

    /**
     * 测试表单转换为字符串表示形式
     */
    @Test
    public void testToString() {
        Form form = Form.create();
        form.add("name", "test");
        form.add("value", "123");

        String formString = form.toString();
        Assert.assertNotNull(formString);
        Assert.assertFalse(formString.isEmpty());
        Assert.assertTrue(formString.contains("\"name\": \"test\""));
        Assert.assertTrue(formString.contains("\"value\": \"123\""));
    }

    /**
     * 测试表单转换为字符串表示形式（包含null值）
     */
    @Test
    public void testToStringWithNullValue() {
        Form form = Form.create();
        form.add("name", "test");
        form.add("value", null);

        String formString = form.toString();
        Assert.assertNotNull(formString);
        Assert.assertFalse(formString.isEmpty());
        Assert.assertTrue(formString.contains("\"name\": \"test\""));
        Assert.assertTrue(formString.contains("\"value\": null"));
    }
}