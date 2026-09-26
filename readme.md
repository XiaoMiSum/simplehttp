# simplehttp

[![License](http://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/XiaoMiSum/simplehttp/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.migoo/simplehttp)](https://central.sonatype.com/artifact/xyz.migoo/simplehttp)
[![MiGoo Author](https://img.shields.io/badge/Author-xiaomi-yellow.svg)](https://github.com/XiaoMiSum)
[![GitHub release](https://img.shields.io/github/release/XiaoMiSum/simplehttp.svg)](https://github.com/XiaoMiSum/simplehttp/releases)
[![Java CI](https://github.com/XiaoMiSum/simplehttp/actions/workflows/ci.yml/badge.svg)](https://github.com/XiaoMiSum/simplehttp/actions/workflows/ci.yml)
[![Publish to Maven Central](https://github.com/XiaoMiSum/simplehttp/actions/workflows/publish.yml/badge.svg)](https://github.com/XiaoMiSum/simplehttp/actions/workflows/publish.yml)

## 目录

- [1. 介绍](#1-介绍)
- [2. 引用](#2-引用)
- [3. 使用示例](#3-使用示例)
  - [3.1 基本 GET 请求](#31-基本-get-请求)
  - [3.2 POST 请求](#32-post-请求)
  - [3.3 添加请求头](#33-添加请求头)
  - [3.4 其他 HTTP 方法](#34-其他-http-方法)
  - [3.5 设置代理](#35-设置代理)
  - [3.6 处理响应](#36-处理响应)
  - [3.7 获取完整且真实的请求响应信息](#37-获取完整且真实的请求响应信息)
- [4. 客户端生命周期](#4-客户端生命周期)
- [5. 注意事项](#5-注意事项)
- [6. 从 2.2.7 升级](#6-从-227-升级)

## 1. 介绍

一个简单的httpclient，基于Apache HttpClient

### ✨ 特性

- 🚀 简洁的 API 设计，易于使用
- 🔥 支持所有 HTTP 方法（GET、POST、PUT、DELETE、HEAD、PATCH、TRACE、OPTIONS）
- 📦 支持 JSON、表单、文本、字节、文件上传等请求体类型（`RequestEntity` 15 个静态工厂）
- 🔒 支持 HTTPS 与逐请求代理（同一客户端可对不同请求使用不同代理）
- 🎯 支持 Cookie 管理和超时控制
- 🔄 可复用的 HTTP 客户端（`SimpleHttp` 单实例长连接池，可并发复用）
- 🔬 **完整且真实的请求响应信息**：线上真实请求头/请求体、线上原样响应头/响应体、
  重定向链、最终 URI、实际路由（target/proxy）、逐跳链路与耗时
- ⚖️ **意图与事实分轨**：你配的（`RequestSnapshot`）和线上发的（`Attempt`）互不覆盖，谁也不会改写谁
- 🧾 一键输出类 Charles/mitmproxy 风格的完整报文（`Exchange#toWireString()`）
- ⚠️ 请求失败（连接拒绝、超时、TLS 失败）同样携带已采集的交换记录（`HttpExecutionException#exchange()`）
- ✅ 190+ 测试用例，含真实 HTTP 链路集成测试（本地起服务端/代理验证线上报文）
- 📖 **本文档所有 Java 示例都参与编译**（`ReadmeCompilationTest`），示例不会与代码脱节
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
    <version>2.3.0</version>
</dependency>
```

### Gradle 引入

在您的 `build.gradle` 中添加以下依赖：

```gradle
implementation 'xyz.migoo:simplehttp:2.3.0'
```

---

## 3. 使用示例

> 本章每个代码块都是**完整可编译的类**，可直接复制运行。

### 3.1 基本 GET 请求

```java
import xyz.migoo.simplehttp.Form;
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;

public class Demo {
    static void main(String[] args) throws Exception {
        // 发送基本 GET 请求
        Response response = Request.get("https://httpbin.org/get").execute();
        System.out.println(response.text());

        // 带查询参数的 GET 请求
        Response response2 = Request.get("https://httpbin.org/get")
                .query(Form.create().add("key", "value").add("page", "1"))
                .execute();
        System.out.println(response2.text());

        // query 只是参数，不会改写原始 URI；线上最终地址见 response.exchange().finalUri()
    }
}
```

### 3.2 POST 请求

#### 发送JSON数据

```java
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.RequestEntity;
import xyz.migoo.simplehttp.Response;

import java.util.HashMap;
import java.util.Map;

public class Demo {
    static void main(String[] args) throws Exception {
        // 准备JSON数据
        Map<String, Object> data = new HashMap<>();
        data.put("username", "testuser");
        data.put("password", "secret");

        // 发送POST请求
        Response response = Request.post("https://httpbin.org/post")
                .body(RequestEntity.json(data))
                .execute();
        System.out.println(response.text());

        // 三种等价入口：Map / 原始字符串 / Customizer 逐字段构造
        RequestEntity json1 = RequestEntity.json(data);
        RequestEntity json2 = RequestEntity.json("{\"id\":1,\"name\":\"simplehttp\"}");
        RequestEntity json3 = RequestEntity.json(map -> {
            map.put("id", 1);
            map.put("name", "simplehttp");
        });
    }
}
```

#### 发送表单数据

```java
import xyz.migoo.simplehttp.Form;
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.RequestEntity;
import xyz.migoo.simplehttp.Response;

import java.util.HashMap;
import java.util.Map;

public class Demo {
    static void main(String[] args) throws Exception {
        // 发送POST请求
        Response response = Request.post("https://httpbin.org/post")
                .body(RequestEntity.form(Form.create()
                        .add("username", "testuser")
                        .add("password", "secret")))
                .execute();
        System.out.println(response.text());

        // 三种等价入口：Form / Customizer / Map
        Map<String, Object> data = new HashMap<>();
        data.put("username", "testuser");

        RequestEntity form1 = RequestEntity.form(Form.create().add("username", "testuser"));
        RequestEntity form2 = RequestEntity.form(form -> form.add("username", "testuser").add("password", "secret"));
        RequestEntity form3 = RequestEntity.form(data);
    }
}
```

#### 其他请求体类型

```java
import org.apache.hc.core5.http.message.BasicNameValuePair;
import xyz.migoo.simplehttp.RequestEntity;

import java.util.Map;

public class Demo {
    static void main(String[] args) {
        byte[] payload = "simplehttp".getBytes();

        // 纯文本
        RequestEntity text = RequestEntity.text("plain text");

        // 原始字节（指定 MIME 类型）
        RequestEntity raw = RequestEntity.bytes(payload, "application/octet-stream");

        // 二进制流
        RequestEntity proto = RequestEntity.proto(payload);

        // 文件上传（multipart/form-data），可叠加表单字段
        RequestEntity file = RequestEntity.binary(new BasicNameValuePair("file", "/tmp/a.txt"));
        RequestEntity fileWithForm = RequestEntity.binary(
                new BasicNameValuePair("file", "/tmp/a.txt"),
                Map.of("username", "testuser"));

        // 其余重载（List<NameValuePair> 批量上传等）见 RequestEntity，共 15 个
    }
}
```

### 3.3 添加请求头

```java
import org.apache.hc.core5.http.message.BasicHeader;
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;

public class Demo {
    static void main(String[] args) throws Exception {
        // 添加单个请求头
        Response response = Request.get("https://httpbin.org/headers")
                .addHeader("Authorization", "Bearer your-token")
                .addHeader("User-Agent", "MyApp/1.0")
                .execute();
        System.out.println(response.text());

        // 或者一次性设置多个请求头
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
import xyz.migoo.simplehttp.RequestEntity;
import xyz.migoo.simplehttp.Response;

public class Demo {
    static void main(String[] args) throws Exception {
        // PUT请求
        Response putResponse = Request.put("https://httpbin.org/put")
                .body(RequestEntity.json("{\"id\": 1, \"name\": \"updated\"}"))
                .execute();

        // DELETE请求
        Response deleteResponse = Request.delete("https://httpbin.org/delete").execute();

        // HEAD请求（无响应体，不会因体为空而抛错）
        Response headResponse = Request.head("https://httpbin.org/get").execute();

        // PATCH请求
        Response patchResponse = Request.patch("https://httpbin.org/patch")
                .body(RequestEntity.json("{\"name\": \"patched\"}"))
                .execute();

        // TRACE / OPTIONS / 自定义方法
        Response traceResponse = Request.trace("https://httpbin.org/trace").execute();
        Response optionsResponse = Request.options("https://httpbin.org/get").execute();
        Response customResponse = Request.create("FOO", "https://httpbin.org/get").execute();
    }
}
```

### 3.5 设置代理

```java
import xyz.migoo.simplehttp.HttpProxy;
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;

public class Demo {
    static void main(String[] args) throws Exception {
        // 逐请求代理：同一客户端可对不同请求使用不同代理
        Response response = Request.get("https://httpbin.org/get")
                .proxy("127.0.0.1", 8080)
                .execute();

        // 带认证的代理
        Response response2 = Request.get("https://httpbin.org/get")
                .proxy("http", "127.0.0.1", 8080, "username", "password")
                .execute();

        // HttpProxy 是不可变对象（2.3.0 起），只有构造器、没有 setter
        HttpProxy proxy = new HttpProxy("http", "127.0.0.1", 8080, "username", "password");
        Response response3 = Request.get("https://httpbin.org/get").proxy(proxy).execute();

        // 客户端级默认代理：SimpleHttp.builder().proxy(proxy).build()
        // 逐请求代理优先级更高；实际用了哪个看 response.exchange().proxy()
    }
}
```

### 3.6 处理响应

```java
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;

public class Demo {
    static void main(String[] args) throws Exception {
        Response response = Request.get("https://httpbin.org/get").execute();

        // 状态码
        int statusCode = response.statusCode();
        System.out.println("Status Code: " + statusCode);

        // 响应头
        String contentType = response.header("Content-Type");
        System.out.println("Content-Type: " + contentType);

        // 响应体（按 Content-Type 字符集解码，缺省 UTF-8，已自动解压）
        String body = response.text();
        System.out.println("Response Body: " + body);

        // 同名头会返回全部，如多个 Set-Cookie
        response.headers("Set-Cookie").forEach(header -> System.out.println(header));

        // 保存响应体到文件，返回文件路径
        String path = response.save("/tmp/response.txt");

        // Cookie、耗时
        System.out.println("Cookies: " + response.cookies());   // 无 Cookie 时是空列表，不是 null
        System.out.println("Duration: " + response.duration() + "ms");

        // 是否成功
        if (response.isSuccessful()) {
            System.out.println("Request successful!");
        }
    }
}
```

### 3.7 获取完整且真实的请求响应信息

集成与排障需要的不只是「结果」，而是线上真实发生的报文与链路信息。
`Response` 同时提供三类视图，刻意并存：

```java
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import xyz.migoo.simplehttp.Attempt;
import xyz.migoo.simplehttp.Exchange;
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.RequestSnapshot;
import xyz.migoo.simplehttp.Response;
import xyz.migoo.simplehttp.Timings;

import java.net.URI;
import java.util.List;

public class Demo {
    static void main(String[] args) throws Exception {
        Response response = Request.get("https://httpbin.org/gzip").execute();
        Exchange exchange = response.exchange();

        // —— 意图视图（执行前定格的原始配置，不会被线上结果改写）——
        RequestSnapshot intent = exchange.request();
        String method = intent.method();            // GET
        URI intentUri = intent.uri();               // 原始 URI，不含 query
        String query = intent.query();              // key=value&page=1
        Header[] intentHeaders = intent.headers();  // 只有你显式设置的头
        byte[] intentBody = intent.body();          // multipart 上传时是描述性 JSON

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

        // —— 链路与逐跳报文（都挂在 exchange 上）——
        //    逐跳：重定向、自动重试各为一跳
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
import xyz.migoo.simplehttp.Request;
import xyz.migoo.simplehttp.Response;
import xyz.migoo.simplehttp.SimpleHttp;

public class Demo {
    static void main(String[] args) throws Exception {
        try (SimpleHttp client = SimpleHttp.builder()
                .connectTimeout(5)                     // 建连（含 TLS 握手）超时，秒
                .readTimeout(10)                       // 读响应超时，秒
                .maxConnections(20)                    // 连接池总量
                .maxConnectionsPerRoute(10)            // 单路由上限
                .build()) {

            Response response = client.execute(Request.get("https://httpbin.org/get"));
            System.out.println(response.statusCode());
        }
        // 关闭后再执行请求会抛 IllegalStateException
    }
}
```

- `SimpleHttp.getDefault()` 返回全局单例，**不要对它调用 `close()`**（进程内所有
  `Request#execute()` 都在复用它）；
- 超时语义：`Builder#connectTimeout` 为连接建立（含 TLS 握手）超时，`Builder#readTimeout`
  为读响应超时，`Request#connectionRequestTimeout` 为从连接池获取连接的超时（`Request#readTimeout`
  可按请求覆盖读超时）。原 `Request#socketTimeout` 已 `@Deprecated`——名字与实际语义不符，
  它写的就是取连接超时那个字段，请直接改用 `Request#connectionRequestTimeout`。

## 5. 注意事项

- **读取口只有三个**：请求信息 → `response.exchange().request()`（`RequestSnapshot`，你的原始意图）；
  逐跳报文 → `response.exchange().attempts()`（`Attempt`，线上事实）；
  响应自身 → `response`。
  `Request` 的链式方法全是「写」，唯一能读的是 `exchange()`（最近一次执行的交换记录，未执行过时为 `null`）；
  执行前想保留自己的配置，请自行存变量。
- `bytes()` / `text()` 是**自动解压后**的结果视图；`rawBytes()` / `rawHeaders()` 是**线上原样**的真实视图。
  gzip 响应解压后，结果视图会主动移除 `Content-Length`/`Content-Encoding`（由 `Response` 归一，
  与 httpclient5 版本无关，否则会出现「头说 33 字节、体却 40 字节」的自相矛盾），
  因此判断压缩与线上长度请用 `contentEncoding()` / `contentLength()` / raw 系列 API。
- `text()` 按 `Content-Type` 中的 `charset` 解码，缺省 UTF-8；需要覆盖用 `text(Charset)`。
- query 参数不会回写 `exchange().request().uri()`，最终地址请取 `exchange().finalUri()`；
  客户端自动补全的头请取 `attempt.wireHeaders()`。
- `exchange().request().body()` 对 multipart 上传返回的是描述性 JSON，线上真实字节请取 `attempt.wireBody()`。
- 采集不影响发送：请求体不回读，流式（不可重复读）请求体同样按线上发送逐字节 tee 记录。
- `Builder#captureEnabled(false)` 关闭报文采集：不安装采集拦截器，不缓冲请求体/响应体、不复制请求头，
  运行时开销与裸 HttpClient 相当。代价是 `exchange().attempts()` 为空、`rawHeaders()`/`rawBytes()` 为空数组，
  `exchange()` 只保留耗时、状态行、重定向链与失败阶段。
- `Response` 只暴露响应自身（状态码、头、体、Cookie、起止耗时）；链路信息（最终 URI、重定向链、
  实际路由、耗时明细、逐跳原始报文）统一从 `response.exchange()` 取。
- `response.cookies()` 无 Cookie 时返回空列表（2.3.0 起不再返回 `null`）。
- `HttpProxy` 是不可变对象，只有构造器没有 setter。
- HTTPS 站点的 `rawBytes()` 为 TLS 解密后的应用层字节（即 HTTP 报文体），与抓包工具看到的明文一致。

## 6. 从 2.2.7 升级

2.3.0 重构了采集与读取模型，**存在破坏性变更**：

- **23 个** public 成员被删除 —— `Request` 8 个读取口、`RequestEntity` 8 个、`HttpProxy` 6 个、`Form` 1 个；
- **5 个** public 类型降为包私有 —— `HttpRequest` 与 4 个 `RequestEntity` 子类；
- **5 项**行为变更 —— `Response.cookies()` 返回空列表、gzip 解压后结果视图不再携带 `Content-Length`/`Content-Encoding`、
  `Request#socketTimeout` 废弃、`HttpProxy` 不可变、连接池不再随单次请求关闭。

重构背景、三层架构、双轨模型、逐成员**旧 → 新**迁移对照表与代码示例，见
**[docs/refactoring-2.3.0.md](docs/refactoring-2.3.0.md)**。
