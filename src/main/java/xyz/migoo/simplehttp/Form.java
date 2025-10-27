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
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表单数据构建器，用于构建HTTP表单请求参数
 * 支持添加键值对参数，并可转换为NameValuePair列表
 *
 * @author xiaomi
 * Created at 2019/9/13 11:03
 */
public class Form {

    /**
     * 存储表单数据的列表
     */
    private final List<NameValuePair> data = new ArrayList<>();

    /**
     * 创建一个空的表单实例
     */
    private Form() {

    }

    /**
     * 根据提供的Map数据创建表单实例
     *
     * @param data 初始化表单的数据
     */
    private Form(Map<String, Object> data) {
        add(data);
    }

    /**
     * 创建一个空的表单实例
     *
     * @return 新的表单实例
     */
    public static Form create() {
        return new Form();
    }

    /**
     * 根据提供的Map数据创建表单实例
     *
     * @param data 初始化表单的数据
     * @return 新的表单实例
     */
    public static Form create(Map<String, Object> data) {
        return new Form().add(data);
    }

    /**
     * 添加一个键值对到表单中
     *
     * @param name  键名
     * @param value 值
     * @return 当前表单实例，支持链式调用
     */
    public Form add(String name, String value) {
        this.data.add(new BasicNameValuePair(name, value));
        return this;
    }

    /**
     * 添加一组键值对到表单中
     *
     * @param data 要添加的数据
     * @return 当前表单实例，支持链式调用
     */
    public Form add(Map<String, Object> data) {
        data.forEach((key, value) -> this.add(key, value == null ? "" : String.valueOf(value)));
        return this;
    }

    /**
     * 构建表单数据为NameValuePair列表
     *
     * @return 表单数据列表
     */
    public List<NameValuePair> build() {
        return this.data;
    }

    /**
     * 将表单数据转换为字符串表示形式
     *
     * @return 表单数据的JSON字符串表示
     */
    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < data.size(); i++) {
            var pair = data.get(i);
            sb.append("\"").append(pair.getName()).append("\": ");
            if (pair.getValue() != null) {
                sb.append("\"").append(pair.getValue()).append("\"");
            } else {
                sb.append(pair.getValue());
            }
            if (i < data.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
