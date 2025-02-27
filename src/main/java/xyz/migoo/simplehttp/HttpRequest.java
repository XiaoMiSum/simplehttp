package xyz.migoo.simplehttp;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.Serial;
import java.net.URI;

/**
 * @author xiaomi
 * @date 2019/9/13 11:00
 */
public class HttpRequest extends HttpUriRequestBase {

    @Serial
    private static final long serialVersionUID = -8216620506182835612L;

    public HttpRequest(String method, URI uri) {
        super(method, uri);
    }

}
