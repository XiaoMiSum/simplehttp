package xyz.migoo.simplehttp;

/**
 * 交换记录监听器：每次 HTTP 交换结束（成功或失败）后回调。
 * <p>
 * 回调发生在<b>发起请求的调用线程</b>上，实现方如需做耗时处理请自行异步化，避免阻塞调用方。
 *
 * @author xiaomi
 */
public interface ExchangeListener {

    /**
     * 交换成功完成
     *
     * @param exchange 完整交换记录
     */
    void onComplete(Exchange exchange);

    /**
     * 交换失败
     *
     * @param exchange 已采集到的部分交换记录（含原始请求快照、已完成的跳、耗时）
     * @param cause    失败原因
     */
    void onFailure(Exchange exchange, Throwable cause);
}
