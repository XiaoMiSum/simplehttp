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

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

/**
 * Response类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/11/06
 */
public class ResponseTest {

    /**
     * 测试Response的基本getter方法
     */
    @Test
    public void testBasicGetters() {
        long startTime = System.currentTimeMillis() - 1000; // 模拟1秒前开始
        Response response = new Response(startTime);

        // 设置response属性
        response.endTime = System.currentTimeMillis();

        Assert.assertEquals(response.startTime(), startTime);
        Assert.assertTrue(response.endTime() >= startTime);
        Assert.assertTrue(response.duration() >= 0);
    }

    /**
     * 测试Response的statusCode方法
     */
    @Test
    public void testStatusCode() {
        Response response = new Response(System.currentTimeMillis());
        response.statusCode = 200;
        Assert.assertEquals(response.statusCode(), 200);
    }

    /**
     * 测试Response的headers方法
     */
    @Test
    public void testHeaders() {
        Response response = new Response(System.currentTimeMillis());
        Header[] headers = {
                new BasicHeader("Content-Type", "application/json"),
                new BasicHeader("Authorization", "Bearer token")
        };
        response.headers = headers;
        Assert.assertEquals(response.headers(), headers);
    }

    /**
     * 测试Response的bytes和text方法
     */
    @Test
    public void testBytesAndText() {
        Response response = new Response(System.currentTimeMillis());
        String content = "Hello, World!";
        response.bytes = content.getBytes(StandardCharsets.UTF_8);

        Assert.assertEquals(response.bytes(), content.getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(response.text(), content);
    }

    /**
     * 测试Response的text方法与自定义转换器
     */
    @Test
    public void testTextWithConverter() {
        Response response = new Response(System.currentTimeMillis());
        String content = "Hello, World!";
        response.bytes = content.getBytes(StandardCharsets.UTF_8);

        String result = response.text(bytes -> new String(bytes, StandardCharsets.UTF_8).toUpperCase());
        Assert.assertEquals(result, content.toUpperCase());
    }

    /**
     * 测试Response的version方法
     */
    @Test
    public void testVersion() {
        Response response = new Response(System.currentTimeMillis());
        response.version = "HTTP/1.1";
        Assert.assertEquals(response.version(), "HTTP/1.1");
    }

    /**
     * 测试Response的message方法
     */
    @Test
    public void testMessage() {
        Response response = new Response(System.currentTimeMillis());
        response.message = "OK";
        Assert.assertEquals(response.message(), "OK");
    }

    /**
     * 测试Response的cookies方法（有cookie的情况）
     */
    @Test
    public void testCookiesWithCookies() {
        Response response = new Response(System.currentTimeMillis());
        response.cookieStore = new CookieStore() {
            @Override
            public void addCookie(Cookie cookie) {
                // 空实现
            }

            @Override
            public List<Cookie> getCookies() {
                return Collections.emptyList();
            }

            @Override
            public boolean clearExpired(java.util.Date date) {
                return false;
            }

            @Override
            public void clear() {
                // 空实现
            }
        };
        Assert.assertEquals(response.cookies(), Collections.emptyList());
    }

    /**
     * 测试Response的cookies方法（没有cookie的情况）
     */
    @Test
    public void testCookiesWithoutCookies() {
        Response response = new Response(System.currentTimeMillis());
        response.cookieStore = null;
        Assert.assertNull(response.cookies());
    }

    /**
     * 测试Response的save方法
     *
     * @throws IOException 如果文件操作出错
     */
    @Test
    public void testSave() throws IOException {
        Response response = new Response(System.currentTimeMillis());
        String content = "Test content for file saving";
        response.bytes = content.getBytes(StandardCharsets.UTF_8);

        // 创建临时文件路径
        File tempFile = File.createTempFile("response_test", ".txt");
        tempFile.deleteOnExit();
        String filePath = tempFile.getAbsolutePath();

        // 保存内容到文件
        String savedPath = response.save(filePath);
        Assert.assertEquals(savedPath, filePath);

        // 验证文件内容
        String fileContent = Files.readString(tempFile.toPath());
        Assert.assertEquals(fileContent, content);
    }

    /**
     * 测试ResponseHandler构造函数
     */
    @Test
    public void testResponseHandlerConstructor() {
        HttpClientContext context = HttpClientContext.create();
        Response.ResponseHandler handler = new Response.ResponseHandler(context);
        Assert.assertNotNull(handler);
    }

    /**
     * 测试ResponseHandler的handleResponse方法
     *
     * @throws IOException 如果处理响应时出错
     */
    @Test
    public void testHandleResponse() throws IOException {
        // 创建HttpClientContext
        HttpClientContext context = HttpClientContext.create();

        // 创建ResponseHandler
        Response.ResponseHandler handler = new Response.ResponseHandler(context);

        // 创建模拟的响应
        ClassicHttpResponse httpResponse = new BasicClassicHttpResponse(200, "OK");
        httpResponse.setVersion(HttpVersion.HTTP_1_1);
        httpResponse.setHeaders(new BasicHeader("Content-Type", "text/plain"),
                new BasicHeader("Server", "Test Server"));
        httpResponse.setEntity(new org.apache.hc.core5.http.io.entity.StringEntity("Test response body"));

        // 处理响应
        long beforeHandling = System.currentTimeMillis();
        Response response = handler.handleResponse(httpResponse);
        long afterHandling = System.currentTimeMillis();

        // 验证结果
        Assert.assertNotNull(response);
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.message(), "OK");
        Assert.assertEquals(response.version(), "HTTP/1.1");
        Assert.assertNotNull(response.headers());
        Assert.assertEquals(response.headers().length, 2);
        Assert.assertTrue(response.startTime() >= beforeHandling);
        Assert.assertTrue(response.endTime() <= afterHandling);
        Assert.assertTrue(response.duration() >= 0);
        Assert.assertNotNull(response.bytes());
        Assert.assertEquals(new String(response.bytes(), StandardCharsets.UTF_8), "Test response body");
    }
}
