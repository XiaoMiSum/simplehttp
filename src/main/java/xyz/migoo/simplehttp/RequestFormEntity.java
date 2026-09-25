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

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 表单请求实体类，用于处理application/x-www-form-urlencoded格式的HTTP请求体
 * 继承自RequestEntity，可以将表单数据编码为URL编码格式
 * 
 * @author xiaomi
 * Created in 2021/7/21 19:52
 */
class RequestFormEntity extends RequestEntity {

    /**
     * 根据表单对象构造一个新的表单请求实体
     * 
     * @param form 表单对象
     */
    RequestFormEntity(Form form) {
        super(new UrlEncodedFormEntity(form.build(), StandardCharsets.UTF_8), form.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据表单数据Map构造一个新的表单请求实体
     * 
     * @param data 表单数据Map
     */
    RequestFormEntity(Map<String, Object> data) {
        this(Form.create(data));
    }
}
