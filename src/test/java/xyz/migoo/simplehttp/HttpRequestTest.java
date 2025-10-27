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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * HttpRequest类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class HttpRequestTest {

    /**
     * 测试创建HTTP GET请求
     *
     * @throws URISyntaxException 如果URI语法错误
     */
    @Test
    public void testCreateGetRequest() throws URISyntaxException {
        URI uri = new URI("http://example.com/");
        HttpRequest request = new HttpRequest("GET", uri);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getMethod(), "GET");
        Assert.assertEquals(request.getUri(), uri);
    }

    /**
     * 测试创建HTTP POST请求
     *
     * @throws URISyntaxException 如果URI语法错误
     */
    @Test
    public void testCreatePostRequest() throws URISyntaxException {
        URI uri = new URI("http://example.com/api/");
        HttpRequest request = new HttpRequest("POST", uri);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertEquals(request.getUri(), uri);
    }

    /**
     * 测试创建HTTP PUT请求
     *
     * @throws URISyntaxException 如果URI语法错误
     */
    @Test
    public void testCreatePutRequest() throws URISyntaxException {
        URI uri = new URI("http://example.com/resource/");
        HttpRequest request = new HttpRequest("PUT", uri);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getUri(), uri);
    }

    /**
     * 测试创建HTTP DELETE请求
     *
     * @throws URISyntaxException 如果URI语法错误
     */
    @Test
    public void testCreateDeleteRequest() throws URISyntaxException {
        URI uri = new URI("http://example.com/resource/1/");
        HttpRequest request = new HttpRequest("DELETE", uri);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getMethod(), "DELETE");
        Assert.assertEquals(request.getUri(), uri);
    }

    /**
     * 测试创建HTTP请求使用小写方法名
     *
     * @throws URISyntaxException 如果URI语法错误
     */
    @Test
    public void testCreateRequestWithLowerCaseMethod() throws URISyntaxException {
        URI uri = new URI("http://example.com/");
        HttpRequest request = new HttpRequest("get", uri);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getMethod(), "get");
        Assert.assertEquals(request.getUri(), uri);
    }

    /**
     * 测试创建HTTP请求使用复杂URI
     *
     * @throws URISyntaxException 如果URI语法错误
     */
    @Test
    public void testCreateRequestWithComplexUri() throws URISyntaxException {
        URI uri = new URI("https://api.example.com:8443/v1/users?id=123&name=test");
        HttpRequest request = new HttpRequest("GET", uri);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getMethod(), "GET");
        Assert.assertEquals(request.getUri(), uri);
    }

    /**
     * 测试序列化ID是否存在
     */
    @Test
    public void testSerialVersionUID() throws NoSuchFieldException, IllegalAccessException {
        // 通过反射检查serialVersionUID字段
        java.lang.reflect.Field field = HttpRequest.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);

        long serialVersionUID = field.getLong(null);
        Assert.assertEquals(serialVersionUID, -8216620506182835612L);
    }
}
