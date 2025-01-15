package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.HttpEntity;

public class RequestBinaryEntity extends RequestEntity {

    public RequestBinaryEntity(HttpEntity entity, byte[] content) {
        super(entity, content);
    }
}
