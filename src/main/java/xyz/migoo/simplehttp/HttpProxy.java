package xyz.migoo.simplehttp;

/**
 * @author xiaomi
 * @date 2020/1/4 12:36
 */
public class HttpProxy {

    private String scheme;

    private String host;

    private Integer port;

    private String username;

    private String password;

    public HttpProxy() {
    }

    public HttpProxy(String host, Integer port) {
        this(null, host, port);
    }

    public HttpProxy(String scheme, String host, Integer port) {
        this(scheme, host, port, null, null);
    }

    public HttpProxy(String scheme, String host, Integer port, String username, String password) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasUsernameAndPassword() {
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }
}
