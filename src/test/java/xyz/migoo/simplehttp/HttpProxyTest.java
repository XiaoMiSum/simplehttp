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
 * HttpProxy类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class HttpProxyTest {

    /**
     * 测试创建空的代理配置实例
     */
    @Test
    public void testCreateEmptyHttpProxy() {
        HttpProxy proxy = new HttpProxy();
        Assert.assertNotNull(proxy);
    }

    /**
     * 测试使用主机和端口创建代理配置实例
     */
    @Test
    public void testCreateHttpProxyWithHostAndPort() {
        HttpProxy proxy = new HttpProxy("localhost", 8080);
        Assert.assertNotNull(proxy);
        Assert.assertEquals(proxy.getHost(), "localhost");
        Assert.assertEquals(proxy.getPort(), Integer.valueOf(8080));
        Assert.assertNull(proxy.getScheme());
        Assert.assertNull(proxy.getUsername());
        Assert.assertNull(proxy.getPassword());
    }

    /**
     * 测试使用协议、主机和端口创建代理配置实例
     */
    @Test
    public void testCreateHttpProxyWithSchemeHostAndPort() {
        HttpProxy proxy = new HttpProxy("http", "localhost", 8080);
        Assert.assertNotNull(proxy);
        Assert.assertEquals(proxy.getScheme(), "http");
        Assert.assertEquals(proxy.getHost(), "localhost");
        Assert.assertEquals(proxy.getPort(), Integer.valueOf(8080));
        Assert.assertNull(proxy.getUsername());
        Assert.assertNull(proxy.getPassword());
    }

    /**
     * 测试使用完整参数创建代理配置实例
     */
    @Test
    public void testCreateHttpProxyWithFullParameters() {
        HttpProxy proxy = new HttpProxy("http", "localhost", 8080, "user", "password");
        Assert.assertNotNull(proxy);
        Assert.assertEquals(proxy.getScheme(), "http");
        Assert.assertEquals(proxy.getHost(), "localhost");
        Assert.assertEquals(proxy.getPort(), Integer.valueOf(8080));
        Assert.assertEquals(proxy.getUsername(), "user");
        Assert.assertEquals(proxy.getPassword(), "password");
    }

    /**
     * 测试设置和获取代理协议
     */
    @Test
    public void testSetAndGetScheme() {
        HttpProxy proxy = new HttpProxy();
        proxy.setScheme("https");
        Assert.assertEquals(proxy.getScheme(), "https");
    }

    /**
     * 测试设置和获取代理主机地址
     */
    @Test
    public void testSetAndGetHost() {
        HttpProxy proxy = new HttpProxy();
        proxy.setHost("proxy.example.com");
        Assert.assertEquals(proxy.getHost(), "proxy.example.com");
    }

    /**
     * 测试设置和获取代理端口号
     */
    @Test
    public void testSetAndGetPort() {
        HttpProxy proxy = new HttpProxy();
        proxy.setPort(3128);
        Assert.assertEquals(proxy.getPort(), Integer.valueOf(3128));
    }

    /**
     * 测试设置和获取认证用户名
     */
    @Test
    public void testSetAndGetUsername() {
        HttpProxy proxy = new HttpProxy();
        proxy.setUsername("testuser");
        Assert.assertEquals(proxy.getUsername(), "testuser");
    }

    /**
     * 测试设置和获取认证密码
     */
    @Test
    public void testSetAndGetPassword() {
        HttpProxy proxy = new HttpProxy();
        proxy.setPassword("testpass");
        Assert.assertEquals(proxy.getPassword(), "testpass");
    }

    /**
     * 测试检查用户名和密码是否设置的方法（都有设置）
     */
    @Test
    public void testHasUsernameAndPasswordWithBothSet() {
        HttpProxy proxy = new HttpProxy("http", "localhost", 8080, "user", "password");
        Assert.assertTrue(proxy.hasUsernameAndPassword());
    }

    /**
     * 测试检查用户名和密码是否设置的方法（只有用户名）
     */
    @Test
    public void testHasUsernameAndPasswordWithOnlyUsername() {
        HttpProxy proxy = new HttpProxy();
        proxy.setUsername("user");
        Assert.assertFalse(proxy.hasUsernameAndPassword());
    }

    /**
     * 测试检查用户名和密码是否设置的方法（只有密码）
     */
    @Test
    public void testHasUsernameAndPasswordWithOnlyPassword() {
        HttpProxy proxy = new HttpProxy();
        proxy.setPassword("password");
        Assert.assertFalse(proxy.hasUsernameAndPassword());
    }

    /**
     * 测试检查用户名和密码是否设置的方法（都没有设置）
     */
    @Test
    public void testHasUsernameAndPasswordWithNoneSet() {
        HttpProxy proxy = new HttpProxy();
        Assert.assertFalse(proxy.hasUsernameAndPassword());
    }

    /**
     * 测试检查用户名和密码是否设置的方法（都为空字符串）
     */
    @Test
    public void testHasUsernameAndPasswordWithEmptyStrings() {
        HttpProxy proxy = new HttpProxy();
        proxy.setUsername("");
        proxy.setPassword("");
        Assert.assertFalse(proxy.hasUsernameAndPassword());
    }

    /**
     * 测试代理配置的字符串表示形式
     */
    @Test
    public void testToString() {
        HttpProxy proxy = new HttpProxy("http", "localhost", 8080, "user", "password");
        String proxyString = proxy.toString();
        Assert.assertNotNull(proxyString);
        Assert.assertFalse(proxyString.isEmpty());
        Assert.assertTrue(proxyString.contains("\"scheme\": \"http\""));
        Assert.assertTrue(proxyString.contains("\"host\": \"localhost\""));
        Assert.assertTrue(proxyString.contains("\"port\": \"8080\""));
        Assert.assertTrue(proxyString.contains("\"username\": \"user\""));
        Assert.assertTrue(proxyString.contains("\"password\": \"password\""));
    }

    /**
     * 测试代理配置的字符串表示形式（部分字段为空）
     */
    @Test
    public void testToStringWithNullFields() {
        HttpProxy proxy = new HttpProxy();
        proxy.setHost("localhost");
        proxy.setPort(8080);
        String proxyString = proxy.toString();
        Assert.assertNotNull(proxyString);
        Assert.assertFalse(proxyString.isEmpty());
        Assert.assertTrue(proxyString.contains("\"host\": \"localhost\""));
        Assert.assertTrue(proxyString.contains("\"port\": \"8080\""));
        Assert.assertTrue(proxyString.contains("\"scheme\": \"\""));
        Assert.assertTrue(proxyString.contains("\"username\": \"\""));
        Assert.assertTrue(proxyString.contains("\"password\": \"\""));
    }
}
