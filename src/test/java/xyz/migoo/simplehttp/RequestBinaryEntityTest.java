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

import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RequestBinaryEntity类的TestNG单元测试
 *
 * @author xiaomi
 * @date 2025/10/27
 */
public class RequestBinaryEntityTest {

    /**
     * 测试构造函数
     */
    @Test
    public void testConstructor() {
        // 创建一个模拟的HttpEntity
        StringEntity entity = new StringEntity("test content", ContentType.TEXT_PLAIN);
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);

        // 创建RequestBinaryEntity实例
        RequestBinaryEntity binaryEntity = new RequestBinaryEntity(entity, content);

        // 验证实体和内容正确设置
        Assert.assertNotNull(binaryEntity);
        Assert.assertEquals(binaryEntity.getEntity(), entity);
        Assert.assertEquals(binaryEntity.getContent(), content);
    }

    /**
     * 测试通过RequestEntity.binary方法创建RequestBinaryEntity实例
     */
    @Test
    public void testCreateRequestBinaryEntityViaFactoryMethod() {
        // 创建临时文件用于测试
        File tempFile = new File("temp.txt");
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (Exception e) {
                // 忽略异常，测试仍可继续
            }
        }

        try {
            // 准备测试数据
            List<NameValuePair> files = new ArrayList<>();
            files.add(new NameValuePair() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getValue() {
                    return tempFile.getAbsolutePath();
                }
            });

            Map<String, Object> data = new HashMap<>();
            data.put("param1", "value1");
            data.put("param2", 123);

            // 通过工厂方法创建RequestBinaryEntity
            RequestEntity requestEntity = RequestEntity.binary(files, data);

            // 验证创建的实体是RequestBinaryEntity类型
            Assert.assertTrue(requestEntity instanceof RequestBinaryEntity);

            // 验证实体不为空
            Assert.assertNotNull(requestEntity.getEntity());
            Assert.assertNotNull(requestEntity.getContent());
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 测试仅包含文件的二进制请求实体
     */
    @Test
    public void testBinaryEntityWithFilesOnly() {
        // 创建临时文件用于测试
        File tempFile = new File("temp.txt");
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (Exception e) {
                // 忽略异常，测试仍可继续
            }
        }

        try {
            // 准备测试数据
            List<NameValuePair> files = new ArrayList<>();
            files.add(new NameValuePair() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getValue() {
                    return tempFile.getAbsolutePath();
                }
            });

            // 通过工厂方法创建RequestBinaryEntity
            RequestEntity requestEntity = RequestEntity.binary(files);

            // 验证创建的实体是RequestBinaryEntity类型
            Assert.assertTrue(requestEntity instanceof RequestBinaryEntity);

            // 验证实体不为空
            Assert.assertNotNull(requestEntity.getEntity());
            Assert.assertNotNull(requestEntity.getContent());
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 测试使用自定义构建器创建的实体
     */
    @Test
    public void testCustomMultipartEntityBuilder() {
        // 创建一个自定义的multipart实体
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.STRICT)
                .addTextBody("field1", "value1");

        byte[] content = "{\"test\": \"data\"}".getBytes(StandardCharsets.UTF_8);

        // 直接创建RequestBinaryEntity实例
        RequestBinaryEntity binaryEntity = new RequestBinaryEntity(builder.build(), content);

        // 验证实体正确创建
        Assert.assertNotNull(binaryEntity);
        Assert.assertNotNull(binaryEntity.getEntity());
        Assert.assertEquals(binaryEntity.getContent(), content);
    }
}
