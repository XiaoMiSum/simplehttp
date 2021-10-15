package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.HttpEntity;

/**
 * @author xiaomi
 * Created in 2021/7/21 19:50
 */
public abstract class BaseRequestEntity {

    private String content;

    private HttpEntity entity;

    public HttpEntity getEntity() {
        return entity;
    }

    public void setEntity(HttpEntity entity) {
        this.entity = entity;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
