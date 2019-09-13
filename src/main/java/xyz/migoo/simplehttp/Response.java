package xyz.migoo.simplehttp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @author xiaomi
 * @date 2019/9/13 11:01
 */

public class Response {

    private Long startTime;
    private Long endTime;
    private HttpClientContext context;
    private CloseableHttpResponse response;

    Response response(CloseableHttpResponse response) {
        this.response = response;
        return this;
    }

    Response context(HttpClientContext context) {
        this.context = context;
        return this;
    }

    Response startTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    Response endTime(long endTime) {
        this.endTime = endTime;
        return this;
    }

    public boolean isSuccess() {
        return response != null && response.getStatusLine().getStatusCode() == 200;
    }

    public int statusCode() {
        return response != null ? response.getStatusLine().getStatusCode() : -1;
    }

    public Header[] headers() {
        return response.getAllHeaders();
    }

    public long duration() {
        return endTime - startTime;
    }

    public HttpClientContext getContext() {
        return context;
    }

    public String body() {
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            return null;
        }
    }

    public JSONArray jsonArray() {
        try {
            return JSONArray.parseArray(EntityUtils.toString(response.getEntity()));
        } catch (Throwable e) {
            return null;
        }
    }

    public JSONObject jsonObject() {
        try {
            return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
        } catch (Throwable e) {
            return null;
        }
    }

    public JSONArray cookies() {
        if (context != null && context.getCookieStore() != null && context.getCookieStore().getCookies() != null) {
            JSONArray cookies = new JSONArray();
            context.getCookieStore().getCookies().forEach(cookie -> {
                JSONObject o = new JSONObject();
                o.put("name", cookie.getName());
                o.put("value", cookie.getValue());
                o.put("domain", cookie.getDomain());
                o.put("path", cookie.getPath());
                cookies.add(o);
            });
            return cookies;
        }
        return null;
    }
}
