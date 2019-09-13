package xyz.migoo.simplehttp;

import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.util.Args;

import java.net.URI;

/**
 * @author xiaomi
 * @date 2019/9/13 11:00
 */

class HttpRequest extends AbstractHttpMessage implements HttpUriRequest, Configurable {

    private final String method;
    private ProtocolVersion version;
    private URI uri;
    private RequestConfig config;

    HttpRequest(String method, URI requestURI) {
        Args.notBlank(method, "Method");
        Args.notNull(requestURI, "Request URI");
        this.method = method;
        this.uri = requestURI;
    }

    public void setProtocolVersion(ProtocolVersion version) {
        this.version = version;
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return version != null ? version : HttpVersion.HTTP_1_1;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public void abort() throws UnsupportedOperationException {
    }

    @Override
    public boolean isAborted() {
        return false;
    }


    @Override
    public RequestLine getRequestLine() {
        final ProtocolVersion ver = getProtocolVersion();
        final URI uriCopy = getURI();
        String uritext = null;
        if (uriCopy != null) {
            uritext = uriCopy.toASCIIString();
        }
        if (uritext == null || uritext.isEmpty()) {
            uritext = "/";
        }
        return new BasicRequestLine(getMethod(), uritext, ver);
    }

    @Override
    public RequestConfig getConfig() {
        return config;
    }

    public void setConfig(RequestConfig config) {
        this.config = config;
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return getMethod() + " " + getURI() + " " + getProtocolVersion();
    }

}
