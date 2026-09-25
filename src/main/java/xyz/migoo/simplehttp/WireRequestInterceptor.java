package xyz.migoo.simplehttp;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * 最终请求头采集器，插在 {@code MainClientExec} 的协议处理器<b>末尾</b>。
 * <p>
 * 此时标准处理器（RequestContent / RequestTargetHost / RequestClientConnControl / RequestUserAgent /
 * RequestExpectContinue）均已执行完毕，即将写入网络，因此采集到的就是<b>线上真实发出的请求头</b>，
 * 包括客户端自动补全的 Host、Content-Length、Connection、User-Agent、Accept-Encoding、Cookie 等。
 * <p>
 * 请求体不做任何回读：把实体包装成 {@link TeeEntity}，由传输层在真实发送时逐字节记录，
 * 因此流式（不可重复读）请求体也不会被影响；重定向/自动重试再次发送时，采集汇会被重新绑定到新的一跳。
 *
 * @author xiaomi
 */
final class WireRequestInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(final HttpRequest request, final EntityDetails entityDetails, final HttpContext context) {
        var attempt = (Attempt) context.getAttribute(Exchange.ATTEMPT_ATTRIBUTE);
        if (attempt == null) {
            return;
        }
        attempt.markRequest(request);
        if (request instanceof ClassicHttpRequest classic) {
            prepareBody(classic, attempt);
        } else {
            attempt.markRequestBody(Attempt.BodyState.SKIPPED);
        }
    }

    /**
     * 包装请求实体以便随发送记录，或按采集上限跳过记录
     */
    private static void prepareBody(final ClassicHttpRequest request, final Attempt attempt) {
        var entity = request.getEntity();
        if (entity == null) {
            attempt.markRequestBody(Attempt.BodyState.EMPTY);
            return;
        }
        var maxCaptureBytes = attempt.maxCaptureBytes();
        if (maxCaptureBytes >= 0 && entity.getContentLength() > maxCaptureBytes) {
            // 超出采集上限：不包装，避免无谓的内存开销
            attempt.markRequestBody(Attempt.BodyState.TOO_LARGE);
            return;
        }
        // 状态待发送完成后由 finishWireBody 回填
        attempt.markRequestBody(Attempt.BodyState.SKIPPED);
        if (entity instanceof TeeEntity tee) {
            // 重定向/自动重试：同一实体再次发送，改记到新的一跳
            tee.sink(attempt.wireBodySink());
        } else {
            request.setEntity(new TeeEntity(entity, attempt.wireBodySink()));
        }
    }
}
