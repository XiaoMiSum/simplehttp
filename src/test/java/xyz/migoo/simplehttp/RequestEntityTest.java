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

import org.apache.hc.core5.http.NameValuePair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RequestEntity抽象类的TestNG单元测试
 *
 * @author xiaomi
 * Created at 2025/10/27
 */
public class RequestEntityTest {

    /**
     * 测试JSON实体创建 - 通过字符串
     */
    @Test
    public void testJsonEntityWithString() {
        String json = "{\"name\":\"test\",\"value\":123}";
        RequestEntity entity = RequestEntity.json(json);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestJsonEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertEquals(new String(entity.getContent(), StandardCharsets.UTF_8), json);
    }

    /**
     * 测试JSON实体创建 - 通过Supplier
     */
    @Test
    public void testJsonEntityWithSupplier() {
        String json = "{\"name\":\"test\",\"value\":123}";
        RequestEntity entity = RequestEntity.json(() -> json);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestJsonEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertEquals(new String(entity.getContent(), StandardCharsets.UTF_8), json);
    }

    /**
     * 测试JSON实体创建 - 通过Map
     */
    @Test
    public void testJsonEntityWithMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);
        data.put("active", true);
        data.put("nullValue", null);

        RequestEntity entity = RequestEntity.json(data);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestJsonEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertNotNull(entity.getContent());
    }

    /**
     * 测试JSON实体创建 - 通过Customizer
     */
    @Test
    public void testJsonEntityWithCustomizer() {
        RequestEntity entity = RequestEntity.json(map -> {
            map.put("name", "test");
            map.put("value", 123);
        });

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestJsonEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertNotNull(entity.getContent());
    }

    /**
     * 测试表单实体创建 - 通过Map
     */
    @Test
    public void testFormEntityWithMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);

        RequestEntity entity = RequestEntity.form(data);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestFormEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertNotNull(entity.getContent());
    }

    /**
     * 测试表单实体创建 - 通过Customizer
     */
    @Test
    public void testFormEntityWithCustomizer() {
        RequestEntity entity = RequestEntity.form(map -> {
            map.put("name", "test");
            map.put("value", 123);
        });

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestFormEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertNotNull(entity.getContent());
    }

    /**
     * 测试表单实体创建 - 通过Form对象
     */
    @Test
    public void testFormEntityWithFormObject() {
        Form form = Form.create()
                .add("name", "test")
                .add("value", "123");

        RequestEntity entity = RequestEntity.form(form);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestFormEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertNotNull(entity.getContent());
    }

    /**
     * 测试表单实体创建 - 通过Form Customizer
     */
    @Test
    public void testFormEntityWithFormCustomizer() {
        RequestEntity entity = RequestEntity.form2(form -> form.add("name", "test").add("value", "123"));

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestFormEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertNotNull(entity.getContent());
    }

    /**
     * 测试文本实体创建
     */
    @Test
    public void testTextEntity() {
        String text = "This is a test text";
        RequestEntity entity = RequestEntity.text(text);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestBytesEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertEquals(new String(entity.getContent(), StandardCharsets.UTF_8), text);
    }

    /**
     * 测试Protobuf实体创建 - 通过字节数组
     */
    @Test
    public void testProtoEntityWithBytes() {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        RequestEntity entity = RequestEntity.proto(bytes);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestBytesEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertEquals(entity.getContent(), bytes);
    }

    /**
     * 测试Protobuf实体创建 - 通过Supplier
     */
    @Test
    public void testProtoEntityWithSupplier() {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        RequestEntity entity = RequestEntity.proto(() -> bytes);

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestBytesEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertEquals(entity.getContent(), bytes);
    }

    /**
     * 测试字节实体创建
     */
    @Test
    public void testBytesEntity() {
        byte[] bytes = "test data".getBytes(StandardCharsets.UTF_8);
        RequestEntity entity = RequestEntity.bytes(bytes, "application/octet-stream");

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof RequestBytesEntity);
        Assert.assertNotNull(entity.getEntity());
        Assert.assertEquals(entity.getContent(), bytes);
    }

    /**
     * 测试二进制实体创建 - 通过NameValuePair
     */
    @Test
    public void testBinaryEntityWithNameValuePair() {
        // 创建临时文件用于测试
        java.io.File tempFile = new java.io.File("temp.txt");
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (Exception e) {
                // 忽略异常
            }
        }

        try {
            NameValuePair nvp = new NameValuePair() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getValue() {
                    return tempFile.getAbsolutePath();
                }
            };

            RequestEntity entity = RequestEntity.binary(nvp);

            Assert.assertNotNull(entity);
            Assert.assertTrue(entity instanceof RequestBinaryEntity);
            Assert.assertNotNull(entity.getEntity());
            Assert.assertNotNull(entity.getContent());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 测试二进制实体创建 - 通过List
     */
    @Test
    public void testBinaryEntityWithList() {
        // 创建临时文件用于测试
        java.io.File tempFile = new java.io.File("temp.txt");
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (Exception e) {
                // 忽略异常
            }
        }

        try {
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

            RequestEntity entity = RequestEntity.binary(files);

            Assert.assertNotNull(entity);
            Assert.assertTrue(entity instanceof RequestBinaryEntity);
            Assert.assertNotNull(entity.getEntity());
            Assert.assertNotNull(entity.getContent());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 测试二进制实体创建 - 通过Customizer
     */
    @Test
    public void testBinaryEntityWithCustomizer() {
        // 创建临时文件用于测试
        java.io.File tempFile = new java.io.File("temp.txt");
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (Exception e) {
                // 忽略异常
            }
        }

        try {
            RequestEntity entity = RequestEntity.binary(list -> {
                list.add(new NameValuePair() {
                    @Override
                    public String getName() {
                        return "file";
                    }

                    @Override
                    public String getValue() {
                        return tempFile.getAbsolutePath();
                    }
                });
            });

            Assert.assertNotNull(entity);
            Assert.assertTrue(entity instanceof RequestBinaryEntity);
            Assert.assertNotNull(entity.getEntity());
            Assert.assertNotNull(entity.getContent());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
