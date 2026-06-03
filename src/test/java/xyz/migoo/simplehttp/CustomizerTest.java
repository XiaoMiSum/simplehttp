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

import java.util.ArrayList;
import java.util.List;

/**
 * Customizer接口的TestNG单元测试
 *
 * @author xiaomi
 *         Created at 2025/10/27
 */
public class CustomizerTest {

    /**
     * 测试Customizer的基本功能
     */
    @Test
    public void testCustomizerBasicFunction() {
        List<String> list = new ArrayList<>();
        Customizer<List<String>> customizer = l -> l.add("test");

        customizer.customize(list);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0), "test");
    }

    /**
     * 测试Customizer修改对象状态
     */
    @Test
    public void testCustomizerModifyObject() {
        List<String> list = new ArrayList<>();
        Customizer<List<String>> customizer = l -> {
            l.add("item1");
            l.add("item2");
        };

        customizer.customize(list);
        Assert.assertEquals(list.size(), 2);
        Assert.assertEquals(list.get(0), "item1");
        Assert.assertEquals(list.get(1), "item2");
    }

    /**
     * 测试Customizer作为Lambda表达式
     */
    @Test
    public void testCustomizerAsLambda() {
        StringBuilder sb = new StringBuilder();
        Customizer<StringBuilder> customizer = s -> s.append("Hello");

        customizer.customize(sb);
        Assert.assertEquals(sb.toString(), "Hello");
    }

    /**
     * 测试Customizer作为方法引用
     */
    @Test
    public void testCustomizerAsMethodReference() {
        List<Integer> list = new ArrayList<>();
        Customizer<List<Integer>> customizer = l -> l.add(100);

        customizer.customize(list);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0), Integer.valueOf(100));
    }

    /**
     * 测试Customizer在Request中的实际应用
     */
    @Test
    public void testCustomizerInRequest() {
        Request request = Request.get("http://example.com");

        // 使用Customizer添加headers
        request.headers(headers -> {
            headers.add(new org.apache.hc.core5.http.message.BasicHeader("X-Custom-1", "value1"));
            headers.add(new org.apache.hc.core5.http.message.BasicHeader("X-Custom-2", "value2"));
        });

        org.apache.hc.core5.http.Header[] headers = request.headers();
        Assert.assertEquals(headers.length, 2);
    }
}
