package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;

/**
 * @author xiaomi
 * Created at 2022/8/19 22:31
 */
public class RequestBytesEntity extends RequestEntity {

    public RequestBytesEntity(byte[] bytes, String contentType) {
        super(new ByteArrayEntity(bytes, ContentType.parse(contentType)), bytes);
    }
}
