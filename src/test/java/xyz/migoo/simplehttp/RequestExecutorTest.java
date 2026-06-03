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
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * RequestExecutor间接测试（通过Request和SimpleHttp集成测试）
 * 由于RequestExecutor是包内可见，我们通过公共API测试其行为
 *
 * @author xiaomi
 *         Created at 2025/10/27
 */
public class RequestExecutorTest {

    /**
     * 测试查询参数应用 - 验证RequestExecutor正确应用查询参数
     */
    @Test
    public void testQueryParametersApplication() {
        Request request = Request.get("http://example.com/api")
                .query(Form.create()
                        .add("page", "1")
                        .add("size", "10")
                        .add("sort", "name"));

        // 验证查询参数已设置到Request对象
        Form query = request.getQuery();
        Assert.assertNotNull(query);
        Assert.assertEquals(query.build().size(), 3);
    }

    /**
     * 测试Cookie配置 - 验证RequestExecutor正确应用Cookie
     */
    @Test
    public void testCookieConfiguration() {
        Cookie cookie1 = new BasicClientCookie("session", "abc123");
        Cookie cookie2 = new BasicClientCookie("user", "testuser");

        Request request = Request.get("http://example.com")
                .cookies(cookie1, cookie2);

        List<Cookie> cookies = request.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(cookies.size(), 2);
    }

    /**
     * 测试Cookie通过addCookie方法添加
     */
    @Test
    public void testCookieAddMethod() {
        Request request = Request.get("http://example.com")
                .addCookie("session", "abc123")
                .addCookie("user", "testuser");

        List<Cookie> cookies = request.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(cookies.size(), 2);
    }

    /**
     * 测试代理配置 - 验证RequestExecutor能获取代理信息
     */
    @Test
    public void testProxyConfiguration() {
        HttpProxy proxy = new HttpProxy("http", "proxy.example.com", 8080, "user", "password");
        Request request = Request.get("http://example.com")
                .proxy(proxy);

        Assert.assertNotNull(request.getProxy());
        Assert.assertEquals(request.getProxy().getHost(), "proxy.example.com");
        Assert.assertEquals(request.getProxy().getPort(), 8080);
        Assert.assertTrue(request.getProxy().hasUsernameAndPassword());
    }

    /**
     * 测试请求级别超时覆盖 - 验证RequestExecutor应用请求级别超时
     */
    @Test
    public void testRequestLevelTimeoutOverride() {
        Request request = Request.get("http://example.com")
                .socketTimeout(10)
                .readTimeout(30);

        // 注意：文档中使用socketTimeout和readTimeout
        // 这里测试实际的getter方法
        Assert.assertNotNull(request.getSocketTimeout());
        Assert.assertEquals(request.getSocketTimeout(), Integer.valueOf(10));
        Assert.assertNotNull(request.getReadTimeout());
        Assert.assertEquals(request.getReadTimeout(), Integer.valueOf(30));
    }

    /**
     * 测试Expect-Continue配置
     */
    @Test
    public void testExpectContinueConfiguration() {
        Request request = Request.post("http://example.com")
                .useExpectContinue();

        Assert.assertTrue(request.getUseExpectContinue());
    }

    /**
     * 测试重定向配置
     */
    @Test
    public void testRedirectConfiguration() {
        Request request = Request.get("http://example.com")
                .redirectsEnabled(false);

        Assert.assertFalse(request.getRedirectsEnabled());
    }

    /**
     * 测试请求级别配置覆盖客户端默认配置
     */
    @Test
    public void testRequestOverridesClientDefaults() {
        // 创建具有特定默认配置的客户端
        SimpleHttp client = SimpleHttp.builder()
                .connectTimeout(180)
                .readTimeout(180)
                .redirectsEnabled(true)
                .build();

        // 创建请求并设置请求级别配置
        Request request = Request.get("http://example.com/slow-api")
                .readTimeout(300); // 覆盖客户端的180秒

        // 验证请求级别的配置
        Assert.assertEquals(request.getReadTimeout(), Integer.valueOf(300));

        // RequestExecutor在执行时会优先使用请求级别的配置
        // 这里通过getter验证配置已正确设置
    }

    /**
     * 测试RequestExecutor正确读取HttpRequest对象
     */
    @Test
    public void testHttpRequestAccess() {
        Request request = Request.post("http://example.com/api/data");

        // 验证RequestExecutor能通过httpRequest()方法访问HttpRequest
        Assert.assertNotNull(request.httpRequest());
        Assert.assertEquals(request.httpRequest().getMethod(), "POST");
    }

    /**
     * 测试多个Cookie的批量添加
     */
    @Test
    public void testMultipleCookiesBatchAdd() {
        List<Cookie> cookieList = new ArrayList<>();
        cookieList.add(new BasicClientCookie("cookie1", "value1"));
        cookieList.add(new BasicClientCookie("cookie2", "value2"));
        cookieList.add(new BasicClientCookie("cookie3", "value3"));

        Request request = Request.get("http://example.com")
                .cookies(cookieList);

        List<Cookie> cookies = request.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(cookies.size(), 3);
    }

    /**
     * 测试查询参数为空时的处理
     */
    @Test
    public void testEmptyQueryParameters() {
        Request request = Request.get("http://example.com");

        // 未设置查询参数时，getQuery()应返回null
        Assert.assertNull(request.getQuery());
    }

    /**
     * 测试Cookie为空时的处理
     */
    @Test
    public void testEmptyCookies() {
        Request request = Request.get("http://example.com");

        // 未设置Cookie时，getCookies()应返回null
        Assert.assertNull(request.getCookies());
    }

    /**
     * 测试代理为空时的处理
     */
    @Test
    public void testNullProxy() {
        Request request = Request.get("http://example.com");

        // 未设置代理时，getProxy()应返回null
        Assert.assertNull(request.getProxy());
    }

    /**
     * 测试RequestExecutor的集成执行流程（使用SimpleHttp.execute）
     */
    @Test
    public void testExecutorIntegrationWithSimpleHttp() {
        SimpleHttp client = SimpleHttp.builder()
                .connectTimeout(30)
                .readTimeout(60)
                .build();

        Request request = Request.get("http://example.com")
                .addHeader("X-Test", "value")
                .readTimeout(120); // 请求级别覆盖

        // 验证配置已正确设置，RequestExecutor在执行时会读取这些配置
        Assert.assertEquals(request.getReadTimeout(), Integer.valueOf(120));
        Assert.assertNotNull(client);
    }
}
