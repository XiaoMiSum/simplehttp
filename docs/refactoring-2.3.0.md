# simplehttp 2.3.0 重构文档

> 本文面向两类读者：**从 2.2.7 升级的集成方**（重点看第 6 章），以及**需要理解内部结构的维护者**（重点看第 2–5 章）。
> 日常使用方法见 [readme](../readme.md)，本文不重复示例，只回答三件事：**改了什么、为什么这么改、怎么迁移**。

---

## 0. 概览

| | 2.2.7 | 2.3.0 |
|---|---|---|
| public 顶层类型 | 13 | 14 |
| public 成员声明 | 123 | **168** |
| 可触及 public 成员 | — | **162** |
| 测试用例 | 180 | 199 |
| 能拿到的信息 | 只有结果视图 | 意图快照 + 逐跳线上报文 + 链路/耗时 |
| `HttpProxy` | 可变 POJO | 不可变 |
| `Request` 读取口 | 8 个 | **1 个**（`exchange()`） |

public 成员**净增 45** = 新增的 6 个采集/视图类型贡献 **57**（`Exchange` 16、`Attempt` 26、`RequestSnapshot` 7、`Timings` 6、`HttpExecutionException` 2、`ExchangeListener` 0）
**−** 基线类型净减 **12**。净减的来源是：砍掉 **23 个冗余 public 成员**、**5 个不该 public 的类型**降级带走 8 个成员，
同期又在 `Response` / `SimpleHttp` / `Request` 上补了 19 个新方法。净结果 +45，总量压在 **168**。

---

## 1. 动机

对 2.2.7 的评估结论是：**它无法满足「外部集成方需要完整且真实的 HTTP 请求响应信息」**。已确认的问题：

| 级别 | 问题 | 后果 |
|---|---|---|
| P0 | 每次 `execute()` 都关闭共享连接池 | 第二次请求直接抛 `Connection pool shut down` |
| P0 | HEAD / 204 响应体为空 | NPE |
| P0 | gzip 自动解压后客户端移除 `Content-Length` / `Content-Encoding` | 判断压缩与线上长度失真 |
| P0 | query 参数被回写进 `request.uri()` | 用户原始意图被污染，"我配的"和"线上发的"分不清 |
| P1 | multipart 上传 `body()` 返回描述性 JSON | 拿不到线上真实发送字节 |
| P1 | readme 引用 `Response` 上不存在的 `string()` / `header()` / `isSuccessful()` | 文档不可用 |

结论落到方案上就是两条主线：**修数据正确性**（M0–M4）与**收敛 public API 表面积**（后续 4 轮）。

---

## 2. 架构：三层

| 层 | 类 | 职责 |
|---|---|---|
| **执行层** | `SimpleHttp`、`RequestExecutor`、`ContextProxyRoutePlanner` | 持有**单个**长生命周期 `CloseableHttpClient`；组装超时/重定向/expect-continue、代理与代理认证、Cookie；失败归档 |
| **采集层** | `ExchangeRecorder`、`WireRequestInterceptor`、`TeeEntity` | 两个插入点：`addExecInterceptorLast`（响应侧收口）与 `addRequestInterceptorLast`（请求发送侧），都经 `TeeEntity` 边发/边收边复制 |
| **视图层** | `Response`、`Exchange`、`Attempt`、`RequestSnapshot`、`Timings` | 全部为**不可变快照**，执行一结束就定格，不随底层对象变化 |

请求对象的传递链：

```
Request（链式配置，只写）
   │  execute()
   ▼
RequestSnapshot.of(request)  ─────────────►  Exchange.request()      意图：执行前定格，永不回写
   │
   ▼
RequestExecutor ──► SimpleHttp（单 client + 连接池）
   │                      │
   │                      └─ WireRequestInterceptor（addRequestInterceptorLast）
   │                            └─ TeeEntity ──► Attempt.wireHeaders() / wireBody()
   ▼
ExchangeRecorder（addExecInterceptorLast，每跳收口）
   ├─► Attempt.statusCode() / rawHeaders() / rawBody()
   └─► Exchange.finalUri() / redirects() / target() / proxy() / timings() / stage()
```

包私有的实现适配层（**不是 public API**）：

- `HttpRequest` —— 继承 HC 的 `HttpUriRequestBase`。它对外可见会连带暴露继承链上 **60 个方法**，而外部根本没有把它接回 `Request` 的入口，因此 2.3.0 起降为包私有。
- `RequestJsonEntity` / `RequestFormEntity` / `RequestBytesEntity` / `RequestBinaryEntity` —— 四个 `RequestEntity` 子类，对外只经静态工厂创建。
- `TeeEntity` / `ExchangeRecorder` / `WireRequestInterceptor` / `RequestExecutor` / `ContextProxyRoutePlanner`。

---

## 3. 双轨模型：意图 vs 事实

**核心原则：用户配的（意图）和线上发生的（事实）永远分开放，互不覆盖。**

| 关注点 | 意图侧（执行前定格） | 事实侧（线上原样） |
|---|---|---|
| URI | `exchange().request().uri()` | `attempt.wireUri()`、`exchange().finalUri()` |
| query | `exchange().request().query()` | 合入 `finalUri()`，**不回写**意图 |
| 请求头 | `exchange().request().headers()`（用户显式设置的） | `attempt.wireHeaders()`（含 Host / User-Agent / Accept-Encoding / Connection / Cookie 等客户端补全的头） |
| 请求体 | `exchange().request().body()`（multipart 时是描述性 JSON） | `attempt.wireBody()`（线上真实字节） |
| 响应 | — | `response.rawHeaders()` / `rawBytes()`（线上原样）**与** `response.text()` / `bytes()`（自动解压后） |
| 路由 | `request.proxy(...)` 配置动作 | `exchange().target()` / `proxy()` / `isTunnelled()` |
| 逐跳状态 | — | `attempt.statusCode()` / `reason()` / `location()` |

### 为什么要两套并存

- gzip 解压会**改写响应头**；
- query 合入会**改写 URI**；
- 客户端会**补全请求头**；
- multipart 的"请求体"在用户侧是一段描述，在线上是一串字节。

任何"事后把线上值写回用户对象"的做法，都会让集成方分不清哪些是自己配的、哪些是库补的。因此：

- `bytes()` / `text()` 保留**解压后**语义（好用）；
- `rawBytes()` / `rawHeaders()` 保留**线上原样**（真实）；
- 两者刻意并存，readme 第 3.7 章明确写了这个取舍。

---

## 4. 关键设计决策

1. **单 client 生命周期**
   `SimpleHttp` 持有单个 `CloseableHttpClient` 与连接池，跨请求、跨线程复用，`close()` 幂等。`getDefault()` 返回的全局单例**不可关闭**（已写进 javadoc 与 readme）。这是对 P0「每次关池」的修复。

2. **请求体不回读，随发送 tee**
   流式（不可重复读）的 `HttpEntity` 只能 `getContent()` 一次，回读会破坏发送。因此 `WireRequestInterceptor` 在发送路径上装 `TeeEntity`，边发边复制字节，**采集不影响发送**。

3. **`BodySink.begin()` / `complete()` 门控**
   响应读完后 `EntityUtils.consume()` 还会调一次 `close()`，没有这层门控会把已采集的缓冲二次清空。

4. **`maxCaptureBytes` 默认 8 MiB，`-1` 不限**
   超出即截断并置 truncation 标记；`captureToFile(dir)` 让原始响应体完成后落盘并释放内存。

5. **`Builder.captureEnabled(boolean)`**
   `false` 时不安装两个采集拦截器——不缓冲请求体/响应体、不复制请求头，运行时开销与裸 HttpClient 相当。代价是 `attempts()` 为空、`rawHeaders()`/`rawBytes()` 为空数组，`exchange()` 只保留耗时、状态行、重定向链与失败阶段。
   配套 `Exchange.rememberSentUri()`，保证关闭采集时 `finalUri()` 仍然含派生出的 query。

6. **失败同样归档**
   `HttpExecutionException.exchange()` 给出已采集的快照与已完成的跳，`stage()` 定位失败阶段（DNS / CONNECT / TLS / TIMEOUT / RESPONSE / UNKNOWN），底层 `getCause()` 始终保留。

7. **`Request` 只写，`RequestSnapshot` 只读**
   URI 派生（`scheme://host:port/path`，不带 query）放在 `RequestSnapshot.of()`；`Request` 上不再有任何信息读取口——它现在只剩 `exchange()` 一个 public 读取方法。

---

## 5. public API 表面积收敛

### 四条原则

1. **实现细节不外泄** —— 继承 HC 类型的类一律不 public。
2. **语义重复只留一个** —— 两处能读到同一份数据时，删掉语义较弱的那个。
3. **每项删除都能用现有 API 一行拼出** —— 不引入新东西来换旧东西。
4. **不牺牲易用性** —— 链式 setter、静态工厂、`Customizer` 全部保留。

### 逐轮数据

| 提交 | 说明 | public 声明 | 可触及 | 顶层类型 |
|---|---|---|---|---|
| `d8ac71c` | 2.2.7 基线 | 123 | — | 13 |
| `53ab6cc` | M0–M4 落地 | 213 | 207 | 19 |
| `8b36cf1` | 16 项收敛 | 197 | 191 | 14 |
| `3fd1512` | 内部类型降级 + 读取口统一 | 184 | 178 | 14 |
| `fb7016e` | 删 `Request#proxy` | 183 | 177 | 14 |
| `e4d2ded` | 删 `Request#cookies` | 182 | 176 | 14 |
| `3f70538` | 三类收敛 | **168** | **162** | **14** |

> **计数口径**：源码里声明的 `public` 成员，**不含继承**。"可触及"再减去 `Attempt` 内 2 个**私有**嵌套类（`ResponseBodySink` / `WireBodySink`）上因实现 `TeeEntity.BodySink` 接口而被强制声明为 `public` 的 6 个方法。

### `8b36cf1` 的 16 项

只处理"实现泄漏与语义重叠"，不碰易用性：

- **降包私有 / 私有（5）**：`Exchange.ATTRIBUTE`（集成方拿不到 `HttpClientContext`）、`HttpExecutionException` 构造器（只由库内部抛出）、`Attempt.index()`（`attempts()` 的顺序已表达）、`Response.charset()`（只被 `text()` 调用）、`Response.contentType()`（只被 `charset()` 调用）
- **降包私有（2）**：`Exchange.rawHeaders()` / `rawBody()`（对外统一走 `lastAttempt()` 与 `Response.rawHeaders()`/`rawBytes()`）
- **删除（9）**：`Exchange.isCompleted()` / `isFailed()` → `isSuccess()` + `failure()`；`Exchange.route()` → `target()` / `proxy()` / `isTunnelled()`；`Timings.endTime()` → `startTime()` + `totalMillis()`；`Attempt.declaredContentLength()` / `isChunked()` → `wireHeaders()` 里的 `Content-Length` / `Transfer-Encoding`；`Attempt.header(String)` → 保留更常用的 `wireHeader(String)`；`Attempt.wireBodyTruncated()` → `requestBodyState() == TRUNCATED`；`RequestSnapshot.header(String)` → `headers()` 数组

这 16 项都是 2.3.0 新增的成员，**对 2.2.7 用户不构成破坏**。

### 当前分布（168）

| 类型 | public | 类型 | public |
|---|---|---|---|
| `Request` | 38 | `HttpProxy` | 10 |
| `Attempt` | 26 | `RequestSnapshot` | 7 |
| `Response` | 25 | `Timings` | 6 |
| `SimpleHttp` | 18 | `Form` | 5 |
| `Exchange` | 16 | `HttpExecutionException` | 2 |
| `RequestEntity` | 15 | `Customizer` / `ExchangeListener` / `HttpMethod` | 0（纯接口/枚举） |

`Request` 的 38 = 10 个静态工厂 + 25 个链式 setter + 2 个 `execute()` 重载 + **1 个读取口 `exchange()`**。

---

## 6. 2.3.0 破坏性变更与迁移

> 以下清单以 `d8ac71c`（2.2.7）为基准逐成员比对得出，**共 23 个删除的 public 成员 + 5 个降级的 public 类型 + 5 项行为变更**。

### 6.1 删除的 public 成员（23）

#### `Request` —— 8 个读取口全部改走 `exchange().request()`

| 旧（2.2.7） | 新（2.3.0） |
|---|---|
| `request.method()` | `exchange().request().method()` |
| `request.uri()` | 意图：`exchange().request().uri()`<br>线上最终：`exchange().finalUri()` |
| `request.query()` | `exchange().request().query()` |
| `request.headers()` | `exchange().request().headers()` |
| `request.body()` | `exchange().request().body()` |
| `request.version()` | `exchange().request().version()` |
| `request.proxy()` | 见下方**语义变更**说明 |
| `request.cookies()` | 无直接替代（见下） |

```java
// 2.2.7
Request request = Request.get("https://httpbin.org/get").query(Form.create().add("page", "1"));
System.out.println(request.uri());       // 读回自己刚配的东西

// 2.3.0
Response response = request.execute();
System.out.println(response.exchange().request().uri());   // 执行前的意图（不含 query）
System.out.println(response.exchange().finalUri());        // 线上最终 URI（含 query、含重定向）
```

**⚠️ 时序变化**：`RequestSnapshot` 只在**开始执行时**定格。执行前无法再读回 `Request` 上的配置——如果你的代码在 `execute()` 之前读 `request.uri()` 做日志或分支判断，需要改成自己保存变量。失败场景不受影响：`HttpExecutionException.exchange()` 同样带快照。

**`request.proxy()`**：它原本只回显"你在这个 Request 上配的"，且只在 `SimpleHttp.Builder` 上配默认代理时返回 `null`（实际生效的代理反而读不到）。2.3.0 直接删除，事实侧统一用 `exchange().proxy()`（返回 `HttpHost`，未走代理时为 `null`）。

**`request.cookies()`**：与包私有的 `getCookies()` 实现完全重复，且是 `Response.cookies()` 在 2.3.0 改为空列表后遗留的不一致面（未设置时返回 `null`）。配置动作 `cookies(...)` / `addCookie(...)` **不变**；线上实际发出的 Cookie 在 `attempt.wireHeaders()` 的 `Cookie` 头里。

#### `RequestEntity` —— 8 个

| 旧（2.2.7） | 新（2.3.0） |
|---|---|
| `RequestEntity.json(Supplier<String>)` | `json(String)` / `json(Map)` / `json(Customizer<Map>)` |
| `RequestEntity.proto(Supplier<byte[]>)` | `proto(byte[])` |
| `RequestEntity.binary(Supplier<NameValuePair>)` | `binary(NameValuePair)` |
| `RequestEntity.binary(Supplier<NPV>, Customizer<Map>)` | `binary(NameValuePair, Map)` |
| `RequestEntity.form(Customizer<Map<String,Object>>)` | `form(Map)` |
| **`RequestEntity.form2(Customizer<Form>)`** | **`RequestEntity.form(Customizer<Form>)`** —— 改名，见下 |
| `entity.getEntity()` | 包私有。它返回 HC 的 `HttpEntity`，本就是类型泄漏；实体只需传给 `Request.body(...)` |
| `entity.getContent()` | `exchange().request().body()` |

**`form2` 的由来与修复**：它存在纯粹是因为 `form(Customizer<Form>)` 与 `form(Customizer<Map>)` 擦除后签名冲突，被迫加了后缀。2.3.0 删掉后者（`form(Map)` 已覆盖），`form` 这个名字就腾出来了。

```java
// 2.2.7
.body(RequestEntity.form2(form -> form.add("username", "test").add("password", "secret")))

// 2.3.0
.body(RequestEntity.form(form -> form.add("username", "test").add("password", "secret")))
```

#### `HttpProxy` —— 5 个 setter + 1 个无参构造

| 旧（2.2.7） | 新（2.3.0） |
|---|---|
| `new HttpProxy()` | **无参构造已删除** |
| `new HttpProxy(); p.setHost(h); p.setPort(x);` | `new HttpProxy(h, x)` |
| `... p.setScheme(s)` | `new HttpProxy(s, h, x)` |
| `... p.setUsername(u); p.setPassword(pw)` | `new HttpProxy(s, h, x, u, pw)` |

5 个 setter 与无参构造在 main 源码中**零引用**（只在 `HttpProxyTest` 里被调用），剩余 3 个构造器（`host:port` / `scheme:host:port` / 含认证全参）已覆盖实际使用方式。字段改 `final`，`HttpProxy` 从可变 POJO 变为**不可变**，与 `Request` / `RequestSnapshot` / `Attempt` 的快照语义一致。

保留：3 个构造器、5 个 getter、`hasUsernameAndPassword()`、`toString()`。

#### `Form` —— 1 个

| 旧（2.2.7） | 新（2.3.0） |
|---|---|
| `form.build()` | 包私有。它返回 HC 的 `List<NameValuePair>`，属类型泄漏 |

对外仍走 `Request.query(form)` 与 `RequestEntity.form(form)`。

### 6.2 降级为包私有（5 个 public 类型）

| 类型 | 对外替代 |
|---|---|
| `HttpRequest` | 无——它从未在 readme 出现，`Request.httpRequest()` 本就包私有，外部无法构造或接入 |
| `RequestJsonEntity` | `RequestEntity.json(...)` |
| `RequestFormEntity` | `RequestEntity.form(...)` |
| `RequestBytesEntity` | `RequestEntity.text(...)` / `proto(...)` / `bytes(...)` |
| `RequestBinaryEntity` | `RequestEntity.binary(...)` |

这 5 个类型的 `new` 只出现在测试里，且测试与实现同包，**不受影响**。

### 6.3 行为变更（5 项）

| 变更 | 影响与对策 |
|---|---|
| `Response.cookies()` 无 Cookie 时返回**空列表**而非 `null` | 2.2.7 里 `if (response.cookies() != null)` 的判空可以删；`response.cookies().isEmpty()` 更直接 |
| gzip 响应解压后，**结果视图**移除 `Content-Length` / `Content-Encoding`（2.3.0 由 `Response` 自行归一，与 httpclient5 版本无关；线上原样由 `rawHeaders()` 保留） | 判断压缩与线上长度改用 `response.rawHeaders()` / `response.contentLength()` / `response.contentEncoding()`；`bytes()` / `text()` 仍是解压后语义 |
| `Request#socketTimeout` 废弃 | 改用 `Request#connectionRequestTimeout`（从连接池取连接的超时）；建连超时用 `Builder#connectTimeout`，读响应超时用 `Builder#readTimeout` |
| `HttpProxy` 变为不可变 | 见 6.1 的 setter 迁移 |
| 连接池不再随单次请求关闭 | 这是 P0 修复。长生命周期 `SimpleHttp` 现在可正常复用；用完请 `close()`（幂等），全局单例 `getDefault()` 不可关闭 |

### 6.4 未变更的部分

- `Request` 的**全部链式 setter**（`body` / `query` / `headers` / `addHeader` / `cookies` / `addCookie` / `proxy` / 超时 / 重定向 / `maxCaptureBytes` / `exchangeListener`）与 **10 个静态工厂**；
- `RequestEntity` 保留下来的 15 个静态工厂；
- `Response` 在 2.2.7 已有的全部读取方法（`statusCode()` / `headers()` / `bytes()` / `text()` / `text(Function)` / `save()` / `cookies()` / `version()` / `message()` / `startTime()` / `endTime()` / `duration()`）与 `ResponseHandler`；
- `Form` 的 `create()` / `create(Map)` / `add(String,String)` / `add(Map)` / `toString()`；
- `Customizer` / `HttpMethod` / `ExchangeListener` 接口与枚举。

---

## 7. 验证

```bash
mvn -B verify
```

- **199 tests, Failures 0, Errors 0**（基线 180）
- javadoc 构建**零告警**
- `ReadmeCompilationTest` 用 `javax.tools` 真编译 readme 里的每一个 java 代码块，文档示例与实现不会脱节
- 集成测试覆盖真实链路：本地起 HTTP 服务端 + 自建 TinyProxy，逐项断言线上的请求头/请求体/响应头/响应体、重定向链、路由、超时截断、`captureEnabled(false)`、失败归档

覆盖缺口（尚未做）：HTTPS/TLS 链路（需证书夹具）、代理认证与 CONNECT 隧道、HTTP/2。

---

## 8. 提交历史

| 提交 | 内容 |
|---|---|
| `d8ac71c` | **2.2.7 基线** |
| `ad2ccb9` | 升级 httpclient5 至 5.6.4（安全修复）与 testng 7.12、改用 `maven.compiler.release`——两条开发线的并入点 |
| `53ab6cc` | M0–M4：三层采集架构、双轨视图、单 client 生命周期、21 个真实链路测试、readme 3.7/4/5 章节、版本升 2.3.0 |
| `8b36cf1` | 修正嵌套类切分遗留缩进；16 项 public 收敛 |
| `3fd1512` | `HttpRequest` + 4 个 `RequestEntity` 子类降包私有；`Request` 6 个读取口删除，`RequestSnapshot` 新增 `query()` 成为唯一读取入口 |
| `fb7016e` | 删 `Request#proxy` |
| `e4d2ded` | 删 `Request#cookies`，`Request` 只剩 `exchange()` 一个读取口 |
| `3f70538` | `HttpProxy` / `RequestEntity` / `Form` 三类收敛，`form2` 改名 `form` |
| `87ff4f3` | 补本篇重构文档 |
| `93f7bbd` | 重构 readme，新增 `ReadmeCompilationTest` 保证文档示例可编译 |
| `eaf970f` | 归一结果视图压缩元数据、Builder 参数校验、`Response` 耗时空安全；删除 8 个断言已否决设计的用例 |

> 历史为单条直线，无合并提交。原 `d8fb89f`（另一台机器、作者 `unknown`）经 `git commit-tree`
> 以相同树与日期重放为 `ad2ccb9`，仅修正作者/提交者为 `xiaomi <mi_xiao@qq.com>`；
> 上表其余 SHA 均由 rebase 产生。重写前的备份在 `backup/pre-linearize`。
