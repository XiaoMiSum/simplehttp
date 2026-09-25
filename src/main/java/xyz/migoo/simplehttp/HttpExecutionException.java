package xyz.migoo.simplehttp;

import java.io.Serial;

/**
 * HTTP 执行失败异常：包装底层原因（保留 {@link #getCause()}），并携带本次交换已采集到的
 * {@link Exchange} 记录（原始请求快照、已完成的跳、失败阶段、耗时）。
 * <p>
 * 因此即使请求失败（连接拒绝、超时、TLS 失败等），集成方依然可以拿到结构化的请求信息用于排障与审计。
 *
 * @author xiaomi
 */
public class HttpExecutionException extends Exception {

    @Serial
    private static final long serialVersionUID = 5841193272284376967L;

    private final transient Exchange exchange;

    HttpExecutionException(String message, Throwable cause, Exchange exchange) {
        super(message, cause);
        this.exchange = exchange;
    }

    /**
     * @return 本次交换的采集记录，可能为 {@code null}（构造时未提供）
     */
    public Exchange exchange() {
        return exchange;
    }

    /**
     * @return 失败阶段，未采集到时为 {@code null}
     */
    public Exchange.Stage stage() {
        return exchange == null ? null : exchange.stage();
    }
}
