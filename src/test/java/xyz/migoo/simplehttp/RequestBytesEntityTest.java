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

import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

/**
 * RequestBytesEntity类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class RequestBytesEntityTest {

    /**
     * 测试构造函数
     */
    @Test
    public void testConstructor() {
        // 准备测试数据
        byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);
        String contentType = "text/plain";

        // 创建RequestBytesEntity实例
        RequestBytesEntity bytesEntity = new RequestBytesEntity(testData, contentType);

        // 验证实例正确创建
        Assert.assertNotNull(bytesEntity);
        Assert.assertNotNull(bytesEntity.getEntity());
        Assert.assertEquals(bytesEntity.getContent(), testData);

        // 验证实体类型正确
        Assert.assertTrue(bytesEntity.getEntity() instanceof ByteArrayEntity);
    }

    /**
     * 测试不同MIME类型的内容
     */
    @Test
    public void testDifferentContentTypes() {
        byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);

        // 测试JSON内容类型
        RequestBytesEntity jsonEntity = new RequestBytesEntity(testData, "application/json");
        Assert.assertNotNull(jsonEntity);
        Assert.assertNotNull(jsonEntity.getEntity());
        Assert.assertEquals(jsonEntity.getContent(), testData);

        // 测试XML内容类型
        RequestBytesEntity xmlEntity = new RequestBytesEntity(testData, "application/xml");
        Assert.assertNotNull(xmlEntity);
        Assert.assertNotNull(xmlEntity.getEntity());
        Assert.assertEquals(xmlEntity.getContent(), testData);

        // 测试Protobuf内容类型
        RequestBytesEntity protoEntity = new RequestBytesEntity(testData, "application/x-protobuf");
        Assert.assertNotNull(protoEntity);
        Assert.assertNotNull(protoEntity.getEntity());
        Assert.assertEquals(protoEntity.getContent(), testData);
    }

    /**
     * 测试通过工厂方法创建RequestBytesEntity实例
     */
    @Test
    public void testCreateRequestBytesEntityViaFactoryMethod() {
        byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);

        // 通过RequestEntity.bytes方法创建实例
        RequestEntity requestEntity = RequestEntity.bytes(testData, "application/octet-stream");

        // 验证创建的实体是RequestBytesEntity类型
        Assert.assertTrue(requestEntity instanceof RequestBytesEntity);

        // 验证实体正确创建
        Assert.assertNotNull(requestEntity.getEntity());
        Assert.assertEquals(requestEntity.getContent(), testData);
    }

    /**
     * 测试Protobuf实体创建
     */
    @Test
    public void testProtoEntityCreation() {
        byte[] testData = new byte[]{1, 2, 3, 4, 5};

        // 通过RequestEntity.proto方法创建实例
        RequestEntity requestEntity = RequestEntity.proto(() -> testData);

        // 验证创建的实体是RequestBytesEntity类型
        Assert.assertTrue(requestEntity instanceof RequestBytesEntity);

        // 验证实体正确创建
        Assert.assertNotNull(requestEntity.getEntity());
        Assert.assertEquals(requestEntity.getContent(), testData);
    }

    /**
     * 测试文本实体创建
     */
    @Test
    public void testTextEntityCreation() {
        String text = "Hello, World!";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        // 通过RequestEntity.text方法创建实例
        RequestEntity requestEntity = RequestEntity.text(text);

        // 验证创建的实体是RequestBytesEntity类型
        Assert.assertTrue(requestEntity instanceof RequestBytesEntity);

        // 验证实体正确创建
        Assert.assertNotNull(requestEntity.getEntity());
        Assert.assertEquals(requestEntity.getContent(), textBytes);
    }
}
