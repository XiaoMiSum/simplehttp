package xyz.migoo.simplehttp;

import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * 报文旁路记录器：包装线上原始实体，在上层读取/写出时同步记录字节，对上层完全透明
 * （所有 {@link HttpEntity} 方法均委托给原实体，绝不提前回读实体）。
 * <p>
 * 两个用途：
 * <ul>
 *     <li><b>响应</b>：位于 ContentCompressionExec 内层，tee 到的是未经解压的线上原始字节
 *     （保留 Content-Encoding 压缩态）；</li>
 *     <li><b>请求</b>：随真实发送逐字节记录线上请求体，流式（不可重复读）请求体同样安全。</li>
 * </ul>
 *
 * @author xiaomi
 */
final class TeeEntity implements HttpEntity {

    /**
     * 字节采集汇
     */
    interface BodySink {

        /**
         * 一次读取开始。返回 {@code false} 表示本次不记录（已采集完毕/已结束），直接透传。
         */
        boolean begin();

        /**
         * 记录一段字节（超过采集上限时由实现自行截断）
         */
        void write(byte[] buffer, int offset, int length);

        /**
         * 读取到流末尾
         */
        void complete();
    }

    private final HttpEntity delegate;
    private volatile BodySink sink;

    TeeEntity(HttpEntity delegate, BodySink sink) {
        this.delegate = delegate;
        this.sink = sink;
    }

    /**
     * 重新绑定采集汇（重定向/自动重试时同一实体会被再次发送，此时改记到新的一跳）
     */
    void sink(BodySink sink) {
        this.sink = sink;
    }

    @Override
    public boolean isRepeatable() {
        return delegate.isRepeatable();
    }

    @Override
    public InputStream getContent() throws IOException {
        var in = delegate.getContent();
        var current = sink;
        if (current == null || !current.begin()) {
            return in;
        }
        return new TeeInputStream(in, current);
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        var current = sink;
        if (current == null || !current.begin()) {
            delegate.writeTo(out);
            return;
        }
        delegate.writeTo(new TeeOutputStream(out, current));
    }

    @Override
    public boolean isStreaming() {
        return delegate.isStreaming();
    }

    @Override
    public Supplier<List<? extends Header>> getTrailers() {
        return delegate.getTrailers();
    }

    @Override
    public long getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public String getContentEncoding() {
        return delegate.getContentEncoding();
    }

    @Override
    public boolean isChunked() {
        return delegate.isChunked();
    }

    @Override
    public Set<String> getTrailerNames() {
        return delegate.getTrailerNames();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * 读取时旁路记录字节
     */
    private static final class TeeInputStream extends InputStream {

        private final InputStream in;
        private final BodySink sink;

        private TeeInputStream(InputStream in, BodySink sink) {
            this.in = in;
            this.sink = sink;
        }

        @Override
        public int read() throws IOException {
            int value = in.read();
            if (value == -1) {
                sink.complete();
            } else {
                sink.write(new byte[]{(byte) value}, 0, 1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = in.read(buffer, offset, length);
            if (read == -1) {
                sink.complete();
            } else if (read > 0) {
                sink.write(buffer, offset, read);
            }
            return read;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /**
     * 写出时旁路记录字节
     */
    private static final class TeeOutputStream extends OutputStream {

        private final OutputStream out;
        private final BodySink sink;

        private TeeOutputStream(OutputStream out, BodySink sink) {
            this.out = out;
            this.sink = sink;
        }

        @Override
        public void write(int value) throws IOException {
            out.write(value);
            sink.write(new byte[]{(byte) value}, 0, 1);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            out.write(buffer, offset, length);
            sink.write(buffer, offset, length);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            sink.complete();
            out.close();
        }
    }
}
