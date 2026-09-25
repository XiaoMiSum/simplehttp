# simplehttp

[![License](http://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/XiaoMiSum/simplehttp/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.migoo/simplehttp)](https://central.sonatype.com/artifact/xyz.migoo/simplehttp)
[![MiGoo Author](https://img.shields.io/badge/Author-xiaomi-yellow.svg)](https://github.com/XiaoMiSum)
[![GitHub release](https://img.shields.io/github/release/XiaoMiSum/simplehttp.svg)](https://github.com/XiaoMiSum/simplehttp/releases)
[![Java CI](https://github.com/XiaoMiSum/simplehttp/actions/workflows/ci.yml/badge.svg)](https://github.com/XiaoMiSum/simplehttp/actions/workflows/ci.yml)
[![Publish to Maven Central](https://github.com/XiaoMiSum/simplehttp/actions/workflows/publish.yml/badge.svg)](https://github.com/XiaoMiSum/simplehttp/actions/workflows/publish.yml)

## 1. 介绍

一个简单的httpclient，基于Apache HttpClient

> 🔁 **从 2.2.7 升级？** 2.3.0 的重构背景、三层架构、双轨模型与**完整破坏性变更清单（23 个删除成员 + 5 个降级类型 + 5 项行为变更）**
> 见 [docs/refactoring-2.3.0.md](docs/refactoring-2.3.0.md)。

### ✨ 特性

- 🚀 简洁的 API 设计，易于使用
- 🔥 支持所有 HTTP 方法（GET、POST、PUT、DELETE 等）
- 📦 支持 JSON、表单、二进制等多种请求体类型
- 🔒 支持 HTTPS 与逐请求代理（同一客户端可对不同请求使用不同代理）
- 🎯 支持 Cookie 管理和超时控制
- 🔄 可复用的 HTTP 客户端（SimpleHttp 单实例长连接池，可并发复用）
- 🔬 **完整且真实的请求响应信息**：线上真实请求头/请求体、线上原样响应头/响应体、
  重定向链、最终 URI、实际路由（target/proxy）、逐跳链路与耗时
- 🧾 一键输出类 Charles/mitmproxy 风格的完整报文（`Exchange#toWireString()`）
- ⚠️ 请求失败（连接拒绝、超时、TLS 失败）同样携带已采集的交换记录（`HttpExecutionException#exchange()`）
- ✅ 190+ 测试用例，含真实 HTTP 链路集成测试（本地起服务端/代理验证线上报文）
- 🤖 自动化 CI/CD，持续集成和发布

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

### 3.1 基本GET请求

```java
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;
import xyz.migoo.simplehttp.Form;

public class Demo {
    static void main(String[] args) {
        // 发送基本GET请求 
        Response response = Request.get("https://httpbin.org/get").execute();
        System.out.println(response.text());
        // 带查询参数的GET请求 
        Response response2 = Request.get("https://httpbin.org/get").query(Form.create().add("key", "value").add("page", "1")).execute();
        System.out.println(response2.text());
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
        System.out.println(response.text());
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
        System.out.println(response.text());
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
        System.out.println(response.text());
        // 或者使用链式调用添加多个请求头 
        Response response2 = Request.get("https://httpbin.org/headers").headers(headers -> {
            headers.add(new BasicHeader("Authorization", "Bearer your-token"));
            headers.add(new BasicHeader("User-Agent", "MyApp/1.0"));
            headers.add(new BasicHeader("Accept", "application/json"));
        }).execute();
        System.out.println(response2.text());
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
        System.out.println(response2.text());
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
        String responseBody = response.text();
        System.out.println("Response Body: " + responseBody);
        // 检查请求是否成功
        if (response.isSuccessful()) {
            System.out.println("Request successful!");
        }
    }
}

```

### 3.7 获取完整且真实的请求响应信息

集成与排障需要的不只是「结果」，而是线上真实发生的报文与链路信息。
`Response` 同时提供两类视图，刻意并存：

```java
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import xyz.migoo.simplehttp.Attempt;
import xyz.migoo.simplehttp.Exchange;
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;
import xyz.migoo.simplehttp.Timings;

import java.net.URI;
import java.util.List;

public class Demo {
    static void main(String[] args) throws Exception {
        Response response = Request.get("https://httpbin.org/gzip").execute();

        // —— 结果视图（便于使用）——
        String text = response.text();          // 按 Content-Type 字符集解码（缺省 UTF-8），已自动解压
        byte[] body = response.bytes();         // 自动解压后的响应体
        boolean ok = response.isSuccessful();   // 2xx
        String contentType = response.header("Content-Type");

        // —— 真实视图（线上原样，自动解压不会使其失真）——
        Header[] rawHeaders = response.rawHeaders();    // Content-Length / Content-Encoding 都保留
        byte[] rawBody = response.rawBytes();           // 保留压缩态的线上原始字节
        long length = response.contentLength();         // 线上 Content-Length，chunked 时为 -1
        String encoding = response.contentEncoding();   // gzip 等，未压缩时为 null

        // —— 完整交换记录（链路信息与逐跳报文都从这里取）——
        //    逐跳：重定向、自动重试各为一跳
        Exchange exchange = response.exchange();
        URI finalUri = exchange.finalUri();             // 最终实际请求 URI（含 query、重定向后地址）
        List<URI> redirects = exchange.redirects();     // 重定向链
        HttpHost target = exchange.target();            // 实际目标主机
        HttpHost proxy = exchange.proxy();              // 实际使用的代理
        Timings timings = exchange.timings();           // connect / ttfb / body / total
        for (Attempt attempt : exchange.attempts()) {
            attempt.wireHeaders();   // 发送前一刻的完整请求头：Host/Content-Length/Connection/User-Agent/
                                     //   Accept-Encoding/Cookie 等客户端补全的头全都在
            attempt.wireBody();      // 线上真实发送的请求体（multipart 上传同样是线上字节）
            attempt.statusCode();    // 该跳的响应状态码（302 也会被记录）
            attempt.rawHeaders();    // 该跳的线上原样响应头（含 Location）
            attempt.rawBody();       // 该跳的线上原始响应体
        }
        // 类 Charles/mitmproxy 风格的完整报文，可直接打日志
        System.out.println(exchange.toWireString());
    }
}
```

#### 请求失败时同样能拿到已采集的信息

```java
import xyz.migoo.simplehttp.Exchange;
import xyz.migoo.simplehttp.HttpExecutionException;
import xyz.migoo.simplehttp.Request;

public class Demo {
    static void main(String[] args) throws Exception {
        try {
            Request.get("http://127.0.0.1:1/").execute();
        } catch (HttpExecutionException e) {
            Exchange exchange = e.exchange();   // 原始请求快照、已完成的跳、耗时
            Exchange.Stage stage = e.stage();   // DNS / CONNECT / TLS / TIMEOUT / RESPONSE / UNKNOWN
            Throwable cause = e.getCause();     // 底层原因（连接拒绝、超时等）始终保留
        }
    }
}
```

#### 交换监听器与采集配置

```java
import xyz.migoo.simplehttp.Exchange;
import xyz.migoo.simplehttp.ExchangeListener;
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;
import xyz.migoo.simplehttp.SimpleHttp;

import java.nio.file.Path;

public class Demo {
    static void main(String[] args) throws Exception {
        ExchangeListener listener = new ExchangeListener() {
            @Override
            public void onComplete(Exchange exchange) {
                System.out.println(exchange.toWireString());
            }

            @Override
            public void onFailure(Exchange exchange, Throwable cause) {
                System.out.println(exchange.stage() + " -> " + cause);
            }
        };

        try (SimpleHttp client = SimpleHttp.builder()
                .exchangeListener(listener)
                .maxCaptureBytes(8 * 1024 * 1024)              // 线上原始报文采集上限，超出即截断；-1 表示不限
                .captureToFile(Path.of("/tmp/http-capture"))   // 原始响应体完成后落盘并释放内存
                .captureEnabled(true)                          // false 则完全不装采集链路，开销同裸 HttpClient
                .build()) {

            // 也可以按请求覆盖监听器与采集上限（优先级高于客户端级配置）
            Response response = client.execute(Request.get("https://httpbin.org/gzip")
                    .exchangeListener(listener)
                    .maxCaptureBytes(1024 * 1024));
        }
    }
}
```

## 4. 客户端生命周期

`SimpleHttp` 持有**单个长生命周期**的 `CloseableHttpClient` 与连接池，可跨请求、跨线程复用
（不会因为单次请求结束而关闭连接池）。用完后 `close()`（幂等）：

```java
try (var client = SimpleHttp.builder().build()) {
    client.execute(Request.get("https://httpbin.org/get"));
}
```

- `SimpleHttp.getDefault()` 返回全局单例，无需关闭；
- 关闭后再执行请求会抛出 `IllegalStateException`；
- 超时语义：`Builder#connectTimeout` 为连接建立（含 TLS 握手）超时，`readTimeout` 为读响应超时，
  `Request#connectionRequestTimeout` 为从连接池获取连接的超时（原 `socketTimeout` 已废弃）。

## 5. 注意事项

- `bytes()` / `text()` 是**自动解压后**的结果视图；`rawBytes()` / `rawHeaders()` 是**线上原样**的真实视图。
  gzip 响应在解压时会被客户端移除 `Content-Length`/`Content-Encoding`，因此判断压缩与线上长度请用 raw 系列 API。
- `exchange.request()` 是执行前的请求快照（`RequestSnapshot`），表示用户的**原始意图**：
  query 参数不会回写，最终 URI 请取 `exchange.finalUri()`；客户端自动补全的头请取 `attempt.wireHeaders()`。
- `exchange.request().body()` 对 multipart 上传返回的是描述性 JSON，线上真实字节请取 `attempt.wireBody()`。
- 采集不影响发送：请求体不回读，流式（不可重复读）请求体同样按线上发送逐字节 tee 记录。
- `Builder#captureEnabled(false)` 关闭报文采集：不安装采集拦截器，不缓冲请求体/响应体、不复制请求头，
  运行时开销与裸 HttpClient 相当。代价是 `exchange().attempts()` 为空、`rawHeaders()`/`rawBytes()` 为空数组，
  `exchange()` 只保留耗时、状态行、重定向链与失败阶段。
- `Response` 只暴露响应自身（状态码、头、体、Cookie、起止耗时）；链路信息（最终 URI、重定向链、
  实际路由、耗时明细、逐跳原始报文）统一从 `response.exchange()` 取。
- `response.cookies()` 无 Cookie 时返回空列表（2.3.0 起不再返回 `null`）。
- HTTPS 站点的 `rawBytes()` 为 TLS 解密后的应用层字节（即 HTTP 报文体），与抓包工具看到的明文一致。