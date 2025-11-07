# simplehttp

[![License](http://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/XiaoMiSum/simplehttp/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.migoo/simplehttp)](https://central.sonatype.com/artifact/xyz.migoo/simplehttp)
[![MiGoo Author](https://img.shields.io/badge/Author-xiaomi-yellow.svg)](https://github.com/XiaoMiSum)
[![GitHub release](https://img.shields.io/github/release/XiaoMiSum/simplehttp.svg)](https://github.com/XiaoMiSum/simplehttp/releases)

## 1. 介绍

一个简单的httpclient，基于Apache HttpClient

## 2. 引用

已上传Maven中央仓库

### Maven 引入

在您的 `pom.xml` 中添加以下依赖：

``` xml
<!-- https://mvnrepository.com/artifact/xyz.migoo/simplehttp -->
<dependency>
    <groupId>xyz.migoo</groupId>
    <artifactId>simplehttp</artifactId>
    <version>${version}</version>
</dependency>
```

### Gradle 引入

在您的 `build.gradle` 中添加：

```gradle
implementation 'xyz.migoo:simplehttp:${version}'
```

## 3. 使用示例

### 2.1 基本GET请求

```java
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;
import xyz.migoo.simplehttp.Form;

public class Demo {
    static void main(String[] args) {
        // 发送基本GET请求 
        Response response = Request.get("https://httpbin.org/get").execute();
        System.out.println(response.string());
        // 带查询参数的GET请求 
        Response response2 = Request.get("https://httpbin.org/get").query(Form.create().add("key", "value").add("page", "1")).execute();
        System.out.println(response2.string());
    }
}

```

### 3.2 POST请求

#### 发送JSON数据

```java 
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.RequestEntity;

import java.util.HashMap;
import java.util.Map;

public class Demo {
    static void main(String[] args) {
        // 准备JSON数据 
        Map<String, Object> data = new HashMap<>();
        data.put("username", "testuser");
        data.put("password", "secret");
        // 发送POST请求 
        Response response = Request.post("https://httpbin.org/post").body(RequestEntity.json(data)).execute();
        System.out.println(response.string());
    }
}
```

#### 发送表单数据

```java 
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.RequestEntity;
import xyz.migoo.simplehttp.Form;

public class Demo {
    static void main(String[] args) {
        // 发送POST请求 
        Response response = Request.post("https://httpbin.org/post")
                .body(RequestEntity.form(Form.create().add("username", "testuser").add("password", "secret")))
                .execute();
        System.out.println(response.string());
    }
}
```

### 3.3 添加请求头

```java 
import xyz.migoo.simplehttp.Request;

public class Demo {
    static void main(String[] args) {
        // 添加单个请求头 
        Response response = Request.get("https://httpbin.org/headers").addHeader("Authorization", "Bearer your-token").addHeader("User-Agent", "MyApp/1.0").execute();
        System.out.println(response.string());
        // 或者使用链式调用添加多个请求头 
        Response response2 = Request.get("https://httpbin.org/headers").headers(headers -> {
            headers.add(new BasicHeader("Authorization", "Bearer your-token"));
            headers.add(new BasicHeader("User-Agent", "MyApp/1.0"));
            headers.add(new BasicHeader("Accept", "application/json"));
        }).execute();
        System.out.println(response2.string());
    }
}

```

### 3.4 其他HTTP方法

```java 
import xyz.migoo.simplehttp.Request;

public class Demo {
    static void main(String[] args) {
        // PUT请求
        Response putResponse = Request.put("https://httpbin.org/put").body(RequestEntity.json("{\"id\": 1, \"name\": \"updated\"}")).execute();
        // DELETE请求 
        Response deleteResponse = Request.delete("https://httpbin.org/delete").execute();
        // HEAD请求 
        Response headResponse = Request.head("https://httpbin.org/get").execute();
        // PATCH请求 
        Response patchResponse = Request.patch("https://httpbin.org/patch").body(RequestEntity.json("{\"name\": \"patched\"}")).execute();
    }
}

```

### 3.5 设置代理

```java 
import xyz.migoo.simplehttp.Request;

public class Demo {
    static void main(String[] args) {
        // 使用代理发送请求
        Response response = Request.get("https://httpbin.org/get").proxy("127.0.0.1", 8080).execute();
        // 带认证的代理
        Response response2 = Request.get("https://httpbin.org/get").proxy("http", "127.0.0.1", 8080, "username", "password").execute();
        System.out.println(response2.string());
    }
}

```

### 3.6 处理响应

```java 
import xyz.migoo.simplehttp.Request;

public class Demo {
    static void main(String[] args) {
        // 使用代理发送请求
        Response response = Request.get("https://httpbin.org/get").execute();
        // 获取响应状态码 
        int statusCode = response.statusCode();
        System.out.println("Status Code: " + statusCode);
        // 获取响应头
        String contentType = response.header("Content-Type");
        System.out.println("Content-Type: " + contentType);

        // 获取响应体
        String responseBody = response.string();
        System.out.println("Response Body: " + responseBody);
        // 检查请求是否成功
        if (response.isSuccessful()) {
            System.out.println("Request successful!");
        }
    }
}

```