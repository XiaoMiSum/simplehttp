/*
 *
 *  * The MIT License (MIT)
 *
 *  * Copyright (c) 2025.  Lorem XiaoMiSum (mi_xiao@qq.com)
 *
 *  * Permission is hereby granted, free of charge, to any person obtaining
 *  * a copy of this software and associated documentation files (the
 *  * 'Software'), to deal in the Software without restriction, including
 *  * without limitation the rights to use, copy, modify, merge, publish,
 *  * distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to
 *  * the following conditions:
 *
 *  * The above copyright notice and this permission notice shall be
 *  * included in all copies or substantial portions of the Software.
 *
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

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

    /**
     * 一次「跳」的完整记录：一次真实发生的请求/响应（重定向、自动重试都会产生新的一跳）
     */
    public final class Attempt {

        private final int index;
        private final String method;
        private final URI uri;
        private final long maxCaptureBytes;
        private final ByteArrayOutputStream rawBodySink = new ByteArrayOutputStream();
        private final ByteArrayOutputStream wireBodyBuffer = new ByteArrayOutputStream();
        private final long startedAtNanos;
        private volatile Header[] wireHeaders = new Header[0];
        private volatile URI wireUri;
        private volatile byte[] wireBody;
        private volatile boolean wireBodyTruncated;
        private volatile long wireBodySize;
        private boolean wireBodyComplete;
        private boolean wireBodyCaptureFinished;
        private volatile BodyState requestBodyState = BodyState.SKIPPED;
        private volatile long declaredContentLength = -1;
        private volatile boolean chunked;
        private volatile int statusCode;
        private volatile String reason;
        private volatile String httpVersion;
        private volatile Header[] rawHeaders = new Header[0];
        private volatile String location;
        private volatile byte[] rawBody = new byte[0];
        private volatile Path rawBodyFile;
        private volatile boolean rawBodyTruncated;
        private volatile long rawBodySize;
        private boolean rawBodyComplete;
        private boolean captureFinished;
        private volatile long headersAtNanos;

        Attempt(int index, HttpRequest request, long maxCaptureBytes) {
            this.index = index;
            this.method = request.getMethod();
            this.uri = uriOf(request);
            this.maxCaptureBytes = maxCaptureBytes;
            this.startedAtNanos = System.nanoTime();
        }

        private static URI uriOf(HttpRequest request) {
            try {
                return request.getUri();
            } catch (URISyntaxException | RuntimeException e) {
                return null;
            }
        }

        public int index() {
            return index;
        }

        /**
         * @return 该跳的请求方法
         */
        public String method() {
            return method;
        }

        /**
         * @return 该跳开始时的请求 URI（含 query、含重定向目标）
         */
        public URI uri() {
            return uri;
        }

        /**
         * @return 发送前一刻的最终请求 URI（由请求拦截器采集）
         */
        public URI wireUri() {
            return wireUri;
        }

        /**
         * @return 发送前一刻的<b>完整</b>请求头，含 Host/Content-Length/Connection/User-Agent/Accept-Encoding/Cookie 等
         * 客户端补全的头
         */
        public Header[] wireHeaders() {
            return wireHeaders;
        }

        /**
         * @return 线上真实发送的请求体字节（multipart 上传等同样为线上真实字节）
         */
        public byte[] wireBody() {
            return wireBody;
        }

        public BodyState requestBodyState() {
            return requestBodyState;
        }

        public long declaredContentLength() {
            return declaredContentLength;
        }

        public boolean isChunked() {
            return chunked;
        }

        /**
         * @return 响应状态码，未收到响应时为 {@code 0}
         */
        public int statusCode() {
            return statusCode;
        }

        /**
         * @return 响应原因短语（reason phrase）
         */
        public String reason() {
            return reason;
        }

        /**
         * @return 响应 HTTP 版本（如 HTTP/1.1、HTTP/2）
         */
        public String httpVersion() {
            return httpVersion;
        }

        /**
         * @return 线上原样响应头（未经自动解压逻辑删改，Content-Length/Content-Encoding 均保留）
         */
        public Header[] rawHeaders() {
            return rawHeaders;
        }

        /**
         * @return 响应 Location 头（重定向目标），非重定向响应为 {@code null}
         */
        public String location() {
            return location;
        }

        /**
         * @return 线上原样响应体字节（保留压缩态）；若配置了落盘则为空，见 {@link #rawBodyFile()}
         */
        public byte[] rawBody() {
            return rawBody;
        }

        /**
         * @return 线上原始响应体落盘路径（配置 {@code captureToFile} 且有响应体时）
         */
        public Path rawBodyFile() {
            return rawBodyFile;
        }

        /**
         * @return 线上原始响应体是否因超出采集上限被截断
         */
        public boolean rawBodyTruncated() {
            return rawBodyTruncated;
        }

        /**
         * @return 线上原始响应体大小（字节），未采集到时为 {@code 0}
         */
        public long rawBodySize() {
            return rawBodySize;
        }

        /**
         * @return 该跳从发出请求到收到响应头的耗时（毫秒），未收到响应时为 {@code -1}
         */
        public long ttfbMillis() {
            if (startedAtNanos == 0 || headersAtNanos == 0) {
                return -1;
            }
            return (headersAtNanos - startedAtNanos) / 1_000_000L;
        }

        public String header(String name) {
            for (var header : rawHeaders) {
                if (header.getName().equalsIgnoreCase(name)) {
                    return header.getValue();
                }
            }
            return null;
        }

        public String wireHeader(String name) {
            for (var header : wireHeaders) {
                if (header.getName().equalsIgnoreCase(name)) {
                    return header.getValue();
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "#" + index + " " + method + " " + uri + " -> " + statusCode;
        }

    /**
     * 请求体采集状态
     */
    public enum BodyState {
        /**
         * 没有请求体（或实际发送了 0 字节）
         */
        EMPTY,
        /**
         * 完整采集：随真实发送逐字节 tee 记录，流式请求体同样适用
         */
        CAPTURED,
        /**
         * 超出采集上限，仅保留前 N 字节（真实大小见 wireBodySize()）
         */
        TRUNCATED,
        /**
         * 超出采集上限，未采集
         */
        TOO_LARGE,
        /**
         * 已包装待发送（发送完成后回填为上述状态），或未能采集
         */
        SKIPPED
    }

        // ------------------------------------------------------------ 内部状态

        void markRequest(HttpRequest request, EntityDetails entityDetails) {
            var headers = request.getHeaders();
            this.wireHeaders = headers == null ? new Header[0] : headers.clone();
            this.wireUri = uriOf(request);
            if (entityDetails != null) {
                this.declaredContentLength = entityDetails.getContentLength();
                this.chunked = entityDetails.isChunked();
            }
        }

        void markRequestBody(BodyState state) {
            this.requestBodyState = state;
        }

        void markResponse(ClassicHttpResponse response) {
            this.statusCode = response.getCode();
            this.reason = response.getReasonPhrase();
            var version = response.getVersion();
            this.httpVersion = version == null ? null : version.toString();
            var headers = response.getHeaders();
            this.rawHeaders = headers == null ? new Header[0] : headers.clone();
            var location = response.getFirstHeader(HttpHeaders.LOCATION);
            this.location = location == null ? null : location.getValue();
            this.headersAtNanos = System.nanoTime();
        }

        /**
         * 响应体采集汇：只在本跳第一次读取时开始记录，避免上层的重复读取
         * （如客户端在响应处理器之后调用的 {@code EntityUtils.consume}）把已采集数据清空。
         */
        TeeEntity.BodySink responseBodySink() {
            return new ResponseBodySink();
        }

        /**
         * 请求体采集汇：由 {@link WireRequestInterceptor} 把请求实体包装成 {@link TeeEntity} 时挂上，
         * 随真实发送逐字节记录，<b>全程不回读实体</b>，因此流式请求体同样安全。
         */
        TeeEntity.BodySink wireBodySink() {
            return new WireBodySink();
        }

        /**
         * @return 线上原始请求体是否因超出采集上限被截断
         */
        public boolean wireBodyTruncated() {
            return wireBodyTruncated;
        }

        /**
         * @return 线上原始请求体的大小（字节）
         */
        public long wireBodySize() {
            return wireBodySize;
        }

        long maxCaptureBytes() {
            return maxCaptureBytes;
        }

        /**
         * 结束请求体采集：取出随发送记录的请求体字节并回填采集状态。
         * 必须在请求发送完成之后调用。
         */
        synchronized void finishWireBody() {
            if (wireBodyCaptureFinished) {
                return;
            }
            wireBodyCaptureFinished = true;
            var bytes = wireBodyBuffer.toByteArray();
            wireBodyBuffer.reset();
            wireBody = bytes;
            if (wireBodySize < bytes.length) {
                wireBodySize = bytes.length;
            }
            if (requestBodyState == BodyState.SKIPPED) {
                if (wireBodyTruncated) {
                    requestBodyState = BodyState.TRUNCATED;
                } else if (bytes.length == 0) {
                    requestBodyState = BodyState.EMPTY;
                } else {
                    requestBodyState = BodyState.CAPTURED;
                }
            }
        }

        /**
         * 结束响应体采集：把 tee 到的线上原始响应体取出（或落盘并释放内存）。
         * 必须在响应体被完整读取之后调用。
         */
        synchronized void finishCapture(Path captureDirectory, String exchangeId) {
            if (captureFinished) {
                return;
            }
            captureFinished = true;
            var size = rawBodySink.size();
            if (size == 0) {
                return;
            }
            var bytes = rawBodySink.toByteArray();
            if (captureDirectory != null) {
                try {
                    Files.createDirectories(captureDirectory);
                    var file = captureDirectory.resolve("simplehttp-" + exchangeId + "-a" + index + ".bin");
                    Files.write(file, bytes);
                    rawBodyFile = file;
                    rawBody = new byte[0];
                    rawBodySink.reset();
                    return;
                } catch (RuntimeException | java.io.IOException e) {
                    // 落盘失败则退化为内存持有
                    rawBodyFile = null;
                }
            }
            rawBody = bytes;
            rawBodySink.reset();
        }

        /**
         * 线上原始响应体采集汇
         */
        private final class ResponseBodySink implements TeeEntity.BodySink {

            @Override
            public boolean begin() {
                synchronized (Attempt.this) {
                    if (captureFinished || rawBodyComplete) {
                        return false;
                    }
                    rawBodySink.reset();
                    rawBodyTruncated = false;
                    rawBodySize = 0;
                    return true;
                }
            }

            @Override
            public void write(byte[] buffer, int offset, int length) {
                synchronized (Attempt.this) {
                    if (captureFinished || length <= 0) {
                        return;
                    }
                    rawBodySize += length;
                    var remaining = maxCaptureBytes < 0 ? length : maxCaptureBytes - rawBodySink.size();
                    if (remaining <= 0) {
                        rawBodyTruncated = true;
                        return;
                    }
                    var toWrite = (int) Math.min(length, remaining);
                    rawBodySink.write(buffer, offset, toWrite);
                    if (toWrite < length) {
                        rawBodyTruncated = true;
                    }
                }
            }

            @Override
            public void complete() {
                rawBodyComplete = true;
            }
        }

        /**
         * 线上原始请求体采集汇
         */
        private final class WireBodySink implements TeeEntity.BodySink {

            @Override
            public boolean begin() {
                synchronized (Attempt.this) {
                    if (wireBodyCaptureFinished || wireBodyComplete) {
                        return false;
                    }
                    wireBodyBuffer.reset();
                    wireBodyTruncated = false;
                    wireBodySize = 0;
                    return true;
                }
            }

            @Override
            public void write(byte[] buffer, int offset, int length) {
                synchronized (Attempt.this) {
                    if (wireBodyCaptureFinished || length <= 0) {
                        return;
                    }
                    wireBodySize += length;
                    var remaining = maxCaptureBytes < 0 ? length : maxCaptureBytes - wireBodyBuffer.size();
                    if (remaining <= 0) {
                        wireBodyTruncated = true;
                        return;
                    }
                    var toWrite = (int) Math.min(length, remaining);
                    wireBodyBuffer.write(buffer, offset, toWrite);
                    if (toWrite < length) {
                        wireBodyTruncated = true;
                    }
                }
            }

            @Override
            public void complete() {
                wireBodyComplete = true;
            }
        }
    }
