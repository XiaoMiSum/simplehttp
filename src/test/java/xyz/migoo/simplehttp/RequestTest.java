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

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.message.BasicHeader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Request类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class RequestTest {

    /**
     * 测试创建GET请求
     */
    @Test
    public void testCreateGetRequest() {
        Request request = Request.get("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "GET");
    }

    /**
     * 测试创建POST请求
     */
    @Test
    public void testCreatePostRequest() {
        Request request = Request.post("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "POST");
    }

    /**
     * 测试创建PUT请求
     */
    @Test
    public void testCreatePutRequest() {
        Request request = Request.put("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "PUT");
    }

    /**
     * 测试创建DELETE请求
     */
    @Test
    public void testCreateDeleteRequest() {
        Request request = Request.delete("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "DELETE");
    }

    /**
     * 测试创建HEAD请求
     */
    @Test
    public void testCreateHeadRequest() {
        Request request = Request.head("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "HEAD");
    }

    /**
     * 测试创建PATCH请求
     */
    @Test
    public void testCreatePatchRequest() {
        Request request = Request.patch("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "PATCH");
    }

    /**
     * 测试创建TRACE请求
     */
    @Test
    public void testCreateTraceRequest() {
        Request request = Request.trace("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "TRACE");
    }

    /**
     * 测试创建OPTIONS请求
     */
    @Test
    public void testCreateOptionsRequest() {
        Request request = Request.options("http://example.com");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.method(), "OPTIONS");
    }

    /**
     * 测试设置HTTP版本为HTTP/2
     */
    @Test
    public void testSetHttp2Version() {
        Request request = Request.get("http://example.com");
        Request result = request.http2();
        Assert.assertEquals(request, result);
        Assert.assertEquals(request.version(), "HTTP/2.0");
    }

    /**
     * 测试设置自定义HTTP版本
     */
    @Test
    public void testSetCustomHttpVersion() {
        Request request = Request.get("http://example.com");
        Request result = request.version(HttpVersion.HTTP_1_1);
        Assert.assertEquals(request, result);
        Assert.assertEquals(request.version(), "HTTP/1.1");
    }

    /**
     * 测试添加请求头
     */
    @Test
    public void testAddHeader() {
        Request request = Request.get("http://example.com");
        Request result = request.addHeader("Content-Type", "application/json");
        Assert.assertEquals(request, result);

        Header[] headers = request.headers();
        Assert.assertEquals(headers.length, 1);
        Assert.assertEquals(headers[0].getName(), "Content-Type");
        Assert.assertEquals(headers[0].getValue(), "application/json");
    }

    /**
     * 测试添加Header对象作为请求头
     */
    @Test
    public void testAddHeaderObject() {
        Request request = Request.get("http://example.com");
        Header header = new BasicHeader("Authorization", "Bearer token");
        Request result = request.addHeader(header);
        Assert.assertEquals(request, result);

        Header[] headers = request.headers();
        Assert.assertEquals(headers.length, 1);
        Assert.assertEquals(headers[0].getName(), "Authorization");
        Assert.assertEquals(headers[0].getValue(), "Bearer token");
    }

    /**
     * 测试添加多个请求头
     */
    @Test
    public void testAddMultipleHeaders() {
        Request request = Request.get("http://example.com");
        Request result = request.addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer token")
                .addHeader("User-Agent", "TestAgent");
        Assert.assertEquals(request, result);

        Header[] headers = request.headers();
        Assert.assertEquals(headers.length, 3);
    }

    /**
     * 测试使用Customizer添加请求头
     */
    @Test
    public void testAddHeadersWithCustomizer() {
        Request request = Request.get("http://example.com");
        Request result = request.headers(headers -> {
            headers.add(new BasicHeader("Content-Type", "application/json"));
            headers.add(new BasicHeader("Authorization", "Bearer token"));
        });
        Assert.assertEquals(request, result);

        Header[] headers = request.headers();
        Assert.assertEquals(headers.length, 2);
        Assert.assertEquals(headers[0].getName(), "Content-Type");
        Assert.assertEquals(headers[0].getValue(), "application/json");
        Assert.assertEquals(headers[1].getName(), "Authorization");
        Assert.assertEquals(headers[1].getValue(), "Bearer token");
    }

    /**
     * 测试使用Header列表添加请求头
     */
    @Test
    public void testAddHeadersWithList() {
        Request request = Request.get("http://example.com");
        List<Header> headerList = new ArrayList<>();
        headerList.add(new BasicHeader("Content-Type", "application/json"));
        headerList.add(new BasicHeader("Authorization", "Bearer token"));

        Request result = request.headers(headerList);
        Assert.assertEquals(request, result);

        Header[] headers = request.headers();
        Assert.assertEquals(headers.length, 2);
    }

    /**
     * 测试设置User-Agent
     */
    @Test
    public void testSetUserAgent() {
        Request request = Request.get("http://example.com");
        Request result = request.userAgent("TestAgent/1.0");
        Assert.assertEquals(request, result);

        Header[] headers = request.headers();
        boolean foundUserAgent = false;
        for (Header header : headers) {
            if ("User-Agent".equals(header.getName()) && "TestAgent/1.0".equals(header.getValue())) {
                foundUserAgent = true;
                break;
            }
        }
        Assert.assertTrue(foundUserAgent);
    }

    /**
     * 测试设置查询参数
     */
    @Test
    public void testSetQuery() {
        Request request = Request.get("http://example.com");
        Form queryForm = Form.create().add("param1", "value1").add("param2", "value2");
        Request result = request.query(queryForm);
        Assert.assertEquals(request, result);
        Assert.assertTrue(request.query().contains("\"param1\": \"value1\""));
        Assert.assertTrue(request.query().contains("\"param2\": \"value2\""));
    }

    /**
     * 测试设置代理
     */
    @Test
    public void testSetProxy() {
        Request request = Request.get("http://example.com");
        Request result = request.proxy("localhost", 8080);
        Assert.assertEquals(request, result);
        Assert.assertNotNull(request.proxy());
    }

    /**
     * 测试设置完整代理信息
     */
    @Test
    public void testSetFullProxy() {
        Request request = Request.get("http://example.com");
        Request result = request.proxy("http", "localhost", 8080, "user", "pass");
        Assert.assertEquals(request, result);
        Assert.assertNotNull(request.proxy());
    }

    /**
     * 测试启用Expect-Continue
     */
    @Test
    public void testUseExpectContinue() {
        Request request = Request.post("http://example.com");
        Request result = request.useExpectContinue();
        Assert.assertEquals(request, result);
    }

    /**
     * 测试设置socket超时
     */
    @Test
    public void testSetSocketTimeout() {
        Request request = Request.get("http://example.com");
        Request result = request.socketTimeout(30);
        Assert.assertEquals(request, result);
    }

    /**
     * 测试设置读取超时
     */
    @Test
    public void testSetReadTimeout() {
        Request request = Request.get("http://example.com");
        Request result = request.readTimeout(30);
        Assert.assertEquals(request, result);
    }

    /**
     * 测试启用或禁用重定向
     */
    @Test
    public void testSetRedirectsEnabled() {
        Request request = Request.get("http://example.com");
        Request result = request.redirectsEnabled(false);
        Assert.assertEquals(request, result);
    }

    /**
     * 测试获取请求URI
     */
    @Test
    public void testGetUri() {
        Request request = Request.get("http://example.com/path");
        Assert.assertEquals(request.uri(), "http://example.com/path");
    }

    /**
     * 测试获取默认URI（只有域名）
     */
    @Test
    public void testGetUriWithDefaultPath() {
        Request request = Request.get("http://example.com");
        Assert.assertEquals(request.uri(), "http://example.com/");
    }
}
