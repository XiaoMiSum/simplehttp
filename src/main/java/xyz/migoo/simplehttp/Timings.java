package xyz.migoo.simplehttp;

/**
 * 一次 HTTP 交换的耗时记录。
 * <p>
 * 所有耗时均基于 {@link System#nanoTime()} 差值计算（不受系统时钟调整影响），
 * 墙钟时间 {@link #startTime()} 仅用于展示与对齐日志，结束时间可由
 * {@code startTime() + totalMillis()} 得到。
 * <p>
 * 口径说明：
 * <ul>
 *     <li>{@link #connectMillis()}：进入执行链到连接（含 TLS 握手）建立完成的耗时，仅首个请求跳有效，其余返回 {@code -1}</li>
 *     <li>{@link #ttfbMillis()}：从开始到收到响应状态行/响应头的耗时（TTFB）</li>
 *     <li>{@link #bodyMillis()}：从收到响应头到读完响应体的耗时</li>
 *     <li>{@link #totalMillis()}：整次交换总耗时，含响应体读取</li>
 * </ul>
 * 尚未完成的阶段返回 {@code -1}。
 *
 * @author xiaomi
 */
public final class Timings {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    /**
     * 交换开始的墙钟时间（毫秒）
     */
    private final long startTime;

    /**
     * 交换开始的纳秒时间戳
     */
    private final long startNanos;

    private long connectNanos;
    private long headersNanos;
    private long completedNanos;

    Timings(long startTime) {
        this.startTime = startTime;
        this.startNanos = System.nanoTime();
    }

    void markConnect() {
        if (connectNanos == 0) {
            connectNanos = System.nanoTime();
        }
    }

    /**
     * 标记收到响应头。多跳（重定向/自动重试）时以最后一跳为准，
     * 因此 {@link #ttfbMillis()} 表示「从开始到收到最终响应头」的耗时。
     */
    void markHeaders() {
        headersNanos = System.nanoTime();
    }

    void markCompleted() {
        completedNanos = System.nanoTime();
    }

    boolean isCompleted() {
        return completedNanos != 0;
    }

    /**
     * @return 交换开始的墙钟时间（毫秒）
     */
    public long startTime() {
        return startTime;
    }

    /**
     * @return 连接建立耗时（毫秒），未采集到时返回 {@code -1}
     */
    public long connectMillis() {
        return connectNanos == 0 ? -1 : millisBetween(startNanos, connectNanos);
    }

    /**
     * @return 首字节（响应头）耗时（毫秒），未采集到时返回 {@code -1}
     */
    public long ttfbMillis() {
        return headersNanos == 0 ? -1 : millisBetween(startNanos, headersNanos);
    }

    /**
     * @return 响应体下载耗时（毫秒），未完成时返回 {@code -1}
     */
    public long bodyMillis() {
        if (headersNanos == 0 || completedNanos == 0) {
            return -1;
        }
        return millisBetween(headersNanos, completedNanos);
    }

    /**
     * @return 总耗时（毫秒），未完成时返回 {@code -1}
     */
    public long totalMillis() {
        return completedNanos == 0 ? -1 : millisBetween(startNanos, completedNanos);
    }

    @Override
    public String toString() {
        return "Timings{connect=" + connectMillis() + "ms, ttfb=" + ttfbMillis()
                + "ms, body=" + bodyMillis() + "ms, total=" + totalMillis() + "ms}";
    }

    private static long millisBetween(long from, long to) {
        return (to - from) / NANOS_PER_MILLI;
    }
}
