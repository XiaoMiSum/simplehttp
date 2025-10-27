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

import java.util.Objects;

/**
 * HTTP代理配置类，用于设置HTTP请求的代理服务器信息
 * 包括代理服务器地址、端口、认证信息等
 *
 * @author xiaomi
 * Created at 2020/1/4 12:36
 */
public class HttpProxy {

    /**
     * 代理协议（如http、https）
     */
    private String scheme;

    /**
     * 代理主机地址
     */
    private String host;

    /**
     * 代理端口号
     */
    private Integer port;

    /**
     * 代理认证用户名
     */
    private String username;

    /**
     * 代理认证密码
     */
    private String password;

    /**
     * 创建一个空的代理配置实例
     */
    public HttpProxy() {
    }

    /**
     * 创建一个代理配置实例
     *
     * @param host 代理主机地址
     * @param port 代理端口号
     */
    public HttpProxy(String host, Integer port) {
        this(null, host, port);
    }

    /**
     * 创建一个代理配置实例
     *
     * @param scheme 代理协议
     * @param host   代理主机地址
     * @param port   代理端口号
     */
    public HttpProxy(String scheme, String host, Integer port) {
        this(scheme, host, port, null, null);
    }

    /**
     * 创建一个代理配置实例
     *
     * @param scheme   代理协议
     * @param host     代理主机地址
     * @param port     代理端口号
     * @param username 认证用户名
     * @param password 认证密码
     */
    public HttpProxy(String scheme, String host, Integer port, String username, String password) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * 获取代理协议
     *
     * @return 代理协议
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * 设置代理协议
     *
     * @param scheme 代理协议
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * 获取代理主机地址
     *
     * @return 代理主机地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置代理主机地址
     *
     * @param host 代理主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取代理端口号
     *
     * @return 代理端口号
     */
    public Integer getPort() {
        return port;
    }

    /**
     * 设置代理端口号
     *
     * @param port 代理端口号
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * 获取认证用户名
     *
     * @return 认证用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置认证用户名
     *
     * @param username 认证用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取认证密码
     *
     * @return 认证密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置认证密码
     *
     * @param password 认证密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 检查是否设置了用户名和密码
     *
     * @return 如果同时设置了用户名和密码返回true，否则返回false
     */
    public boolean hasUsernameAndPassword() {
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }

    /**
     * 返回代理配置的字符串表示形式
     *
     * @return 代理配置的JSON字符串表示
     */
    public String toString() {
        return "{" +
                "\"scheme\": \"" + (Objects.isNull(scheme) ? "" : scheme) + "\", " +
                "\"host\": \"" + (Objects.isNull(host) ? "" : host) + "\", " +
                "\"port\": \"" + (Objects.isNull(port) || port <= 0 ? "" : port) + "\", " +
                "\"username\": \"" + (Objects.isNull(username) ? "" : username) + "\", " +
                "\"password\": \"" + (Objects.isNull(password) ? "" : password) + "\"" +
                "}";
    }
}
