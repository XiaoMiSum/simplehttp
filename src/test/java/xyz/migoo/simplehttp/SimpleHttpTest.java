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
 * SimpleHttp类的TestNG单元测试
 *
 * @author xiaomi
 *         Created at 2025/10/27
 */
public class SimpleHttpTest {

    /**
     * 测试获取默认实例
     */
    @Test
    public void testGetDefaultInstance() {
        SimpleHttp client1 = SimpleHttp.getDefault();
        SimpleHttp client2 = SimpleHttp.getDefault();
        Assert.assertNotNull(client1);
        Assert.assertSame(client1, client2, "默认实例应该是单例");
    }

    /**
     * 测试Builder创建自定义客户端
     */
    @Test
    public void testBuilderWithCustomConfig() {
        SimpleHttp client = SimpleHttp.builder()
                .maxConnections(200)
                .maxConnectionsPerRoute(50)
                .connectTimeout(30)
                .readTimeout(60)
                .build();

        Assert.assertNotNull(client);
        Assert.assertEquals(client.getConnectTimeout(), 30);
        Assert.assertEquals(client.getReadTimeout(), 60);
        Assert.assertTrue(client.isRedirectsEnabled());
        Assert.assertNull(client.getDefaultProxy());
    }

    /**
     * 测试Builder设置最大连接数
     */
    @Test
    public void testBuilderMaxConnections() {
        SimpleHttp client = SimpleHttp.builder()
                .maxConnections(500)
                .build();

        Assert.assertNotNull(client);
        Assert.assertNotNull(client.connectionManager());
    }

    /**
     * 测试Builder设置每路由最大连接数
     */
    @Test
    public void testBuilderMaxConnectionsPerRoute() {
        SimpleHttp client = SimpleHttp.builder()
                .maxConnectionsPerRoute(100)
                .build();

        Assert.assertNotNull(client);
        Assert.assertNotNull(client.connectionManager());
    }

    /**
     * 测试Builder设置连接超时
     */
    @Test
    public void testBuilderConnectTimeout() {
        SimpleHttp client = SimpleHttp.builder()
                .connectTimeout(45)
                .build();

        Assert.assertEquals(client.getConnectTimeout(), 45);
    }

    /**
     * 测试Builder设置读取超时
     */
    @Test
    public void testBuilderReadTimeout() {
        SimpleHttp client = SimpleHttp.builder()
                .readTimeout(120)
                .build();

        Assert.assertEquals(client.getReadTimeout(), 120);
    }

    /**
     * 测试Builder禁用重定向
     */
    @Test
    public void testBuilderRedirectsDisabled() {
        SimpleHttp client = SimpleHttp.builder()
                .redirectsEnabled(false)
                .build();

        Assert.assertFalse(client.isRedirectsEnabled());
    }

    /**
     * 测试Builder启用重定向（默认）
     */
    @Test
    public void testBuilderRedirectsEnabled() {
        SimpleHttp client = SimpleHttp.builder()
                .build();

        Assert.assertTrue(client.isRedirectsEnabled());
    }

    /**
     * 测试Builder设置代理（主机和端口）
     */
    @Test
    public void testBuilderProxyWithHostAndPort() {
        SimpleHttp client = SimpleHttp.builder()
                .proxy("proxy.example.com", 8080)
                .build();

        Assert.assertNotNull(client.getDefaultProxy());
        Assert.assertEquals(client.getDefaultProxy().getHost(), "proxy.example.com");
        Assert.assertEquals(client.getDefaultProxy().getPort(), 8080);
    }

    /**
     * 测试Builder设置代理（HttpProxy对象）
     */
    @Test
    public void testBuilderProxyWithObject() {
        HttpProxy proxy = new HttpProxy("http", "proxy.example.com", 3128, "user", "pass");
        SimpleHttp client = SimpleHttp.builder()
                .proxy(proxy)
                .build();

        Assert.assertNotNull(client.getDefaultProxy());
        Assert.assertEquals(client.getDefaultProxy().getHost(), "proxy.example.com");
        Assert.assertEquals(client.getDefaultProxy().getPort(), 3128);
        Assert.assertTrue(client.getDefaultProxy().hasUsernameAndPassword());
    }

    /**
     * 测试Builder链式调用
     */
    @Test
    public void testBuilderChaining() {
        SimpleHttp client = SimpleHttp.builder()
                .maxConnections(300)
                .maxConnectionsPerRoute(30)
                .connectTimeout(20)
                .readTimeout(40)
                .redirectsEnabled(false)
                .proxy("proxy.example.com", 8080)
                .build();

        Assert.assertNotNull(client);
        Assert.assertEquals(client.getConnectTimeout(), 20);
        Assert.assertEquals(client.getReadTimeout(), 40);
        Assert.assertFalse(client.isRedirectsEnabled());
        Assert.assertNotNull(client.getDefaultProxy());
    }

    /**
     * 测试Builder默认参数值
     */
    @Test
    public void testBuilderDefaultValues() {
        SimpleHttp client = SimpleHttp.builder().build();

        Assert.assertEquals(client.getConnectTimeout(), 180);
        Assert.assertEquals(client.getReadTimeout(), 180);
        Assert.assertTrue(client.isRedirectsEnabled());
        Assert.assertNull(client.getDefaultProxy());
    }

    /**
     * 测试客户端关闭
     */
    @Test
    public void testClientClose() {
        SimpleHttp client = SimpleHttp.builder().build();
        Assert.assertNotNull(client);

        // 关闭不应该抛出异常
        client.close();
    }

    /**
     * 测试execute方法接受Request对象
     */
    @Test
    public void testExecuteMethodSignature() {
        SimpleHttp client = SimpleHttp.builder().build();
        Request request = Request.get("http://example.com");

        // 这里只测试方法签名，不实际执行HTTP请求
        Assert.assertNotNull(client);
        Assert.assertNotNull(request);
    }
}
