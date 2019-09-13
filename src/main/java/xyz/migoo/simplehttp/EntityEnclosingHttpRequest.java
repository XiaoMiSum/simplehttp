package xyz.migoo.simplehttp;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.protocol.HTTP;

import java.net.URI;

/**
 * @author xiaomi
 * @date 2019/9/13 10:59
 */
class EntityEnclosingHttpRequest extends HttpRequest implements HttpEntityEnclosingRequest {

    private HttpEntity entity;

    public EntityEnclosingHttpRequest(final String method, final URI requestURI) {
        super(method, requestURI);
    }

    @Override
    public HttpEntity getEntity() {
        return this.entity;
    }

    @Override
    public void setEntity(final HttpEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean expectContinue() {
        final Header expect = getFirstHeader(HTTP.EXPECT_DIRECTIVE);
        return expect != null && HTTP.EXPECT_CONTINUE.equalsIgnoreCase(expect.getValue());
    }

}
