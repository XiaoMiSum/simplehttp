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

/**
 * HttpMethod枚举的TestNG单元测试
 *
 * @author xiaomi
 *         Created at 2025/10/27
 */
public class HttpMethodTest {

    /**
     * 测试GET方法
     */
    @Test
    public void testGetMethod() {
        HttpMethod method = HttpMethod.GET;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "GET");
    }

    /**
     * 测试POST方法
     */
    @Test
    public void testPostMethod() {
        HttpMethod method = HttpMethod.POST;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "POST");
    }

    /**
     * 测试PUT方法
     */
    @Test
    public void testPutMethod() {
        HttpMethod method = HttpMethod.PUT;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "PUT");
    }

    /**
     * 测试DELETE方法
     */
    @Test
    public void testDeleteMethod() {
        HttpMethod method = HttpMethod.DELETE;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "DELETE");
    }

    /**
     * 测试HEAD方法
     */
    @Test
    public void testHeadMethod() {
        HttpMethod method = HttpMethod.HEAD;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "HEAD");
    }

    /**
     * 测试PATCH方法
     */
    @Test
    public void testPatchMethod() {
        HttpMethod method = HttpMethod.PATCH;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "PATCH");
    }

    /**
     * 测试TRACE方法
     */
    @Test
    public void testTraceMethod() {
        HttpMethod method = HttpMethod.TRACE;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "TRACE");
    }

    /**
     * 测试OPTIONS方法
     */
    @Test
    public void testOptionsMethod() {
        HttpMethod method = HttpMethod.OPTIONS;
        Assert.assertNotNull(method);
        Assert.assertEquals(method.name(), "OPTIONS");
    }

    /**
     * 测试valueOf方法
     */
    @Test
    public void testValueOf() {
        Assert.assertEquals(HttpMethod.valueOf("GET"), HttpMethod.GET);
        Assert.assertEquals(HttpMethod.valueOf("POST"), HttpMethod.POST);
        Assert.assertEquals(HttpMethod.valueOf("PUT"), HttpMethod.PUT);
        Assert.assertEquals(HttpMethod.valueOf("DELETE"), HttpMethod.DELETE);
    }

    /**
     * 测试values方法
     */
    @Test
    public void testValues() {
        HttpMethod[] methods = HttpMethod.values();
        Assert.assertEquals(methods.length, 8);
    }

    /**
     * 测试HttpMethod在Request.create中的使用
     */
    @Test
    public void testHttpMethodInRequest() {
        Request request = Request.create(HttpMethod.GET, "http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "GET");

        request = Request.create(HttpMethod.POST, "http://example.com");
        Assert.assertEquals(request.method(), "POST");

        request = Request.create(HttpMethod.PUT, "http://example.com");
        Assert.assertEquals(request.method(), "PUT");

        request = Request.create(HttpMethod.DELETE, "http://example.com");
        Assert.assertEquals(request.method(), "DELETE");
    }
}
