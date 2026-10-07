package ai.mutuus.common.logging;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;

/** 업무 결과 전용 writer. 자동구성 빈은 현재 TX 완료까지 기다리며 순수 생성자는 no-TX 전용이다. */
public final class ActionLogger {
    public static final String LOGGER_NAME = "ai.mutuus.common.action";
    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private final ActionCompletionHandler completion;

    /** Spring TX 없는 소비자/확정 결과용. TX 소비자는 자동구성 빈을 주입받는다. */
    public ActionLogger() { this(callback -> callback.accept(ActionCompletionHandler.Status.NO_TRANSACTION)); }

    public ActionLogger(ActionCompletionHandler completion) {
        this.completion = Objects.requireNonNull(completion);
    }

    /** no-TX는 실제 결과 확인 후, TX는 업무 owner 경계에서 한 번 호출한다. */
    public void record(ActionRecord record) {
        Objects.requireNonNull(record);
        completion.complete(status -> finish(record, status));
    }

    /** owner의 최외곽 TX에 진입한 즉시 바인딩한다. REQUIRES_NEW 내부에서 새로 바인딩하지 않는다. */
    public BoundAction bind() { return bind(RequestIds.current(), TraceContext.traceId()); }

    /** 배치는 실제 전달된 원 요청 ID 또는 명시적인 invocation ID를 공급한다. */
    public BoundAction bind(String requestId, String traceId) {
        var bound = new BoundAction(requestId, traceId);
        completion.complete(bound::completed);
        return bound;
    }

    private void finish(ActionRecord record, ActionCompletionHandler.Status status) {
        if (status == ActionCompletionHandler.Status.UNKNOWN) diagnostic(record.requestId(), record.operationId(), record.traceId());
        else write(status == ActionCompletionHandler.Status.ROLLED_BACK ? record.rolledBack() : record);
    }

    private void diagnostic(String requestId, String operationId, String traceId) {
        log.atWarn()
                .addKeyValue("event.dataset", "mutuus.action_diagnostic")
                .addKeyValue("event.action", "action.completion.unknown")
                .addKeyValue("requestId", requestId)
                .addKeyValue("operation.id", operationId)
                .addKeyValue(EcsFields.TRACE_ID, traceId)
                .log("action completion unconfirmed");
    }

    /** TX callback과 async 확정 결과의 도착 순서에 의존하지 않고 한 번만 기록한다. */
    public final class BoundAction {
        private final String requestId, traceId;
        private ActionRecord result;
        private ActionCompletionHandler.Status status;
        private boolean emitted;

        private BoundAction(String requestId, String traceId) { this.requestId = requestId; this.traceId = traceId; }

        public synchronized void record(ActionRecord confirmedResult) {
            Objects.requireNonNull(confirmedResult);
            if (result != null) throw new IllegalStateException("action result already supplied");
            result = new ActionRecord(confirmedResult.action(), confirmedResult.outcome(), confirmedResult.errorCode(),
                    requestId, confirmedResult.operationId(), confirmedResult.actorId(), confirmedResult.relatedMemberIds(), traceId);
            emitIfReady();
        }

        private synchronized void completed(ActionCompletionHandler.Status completedStatus) {
            if (status != null) return;
            status = completedStatus;
            emitIfReady();
        }

        private void emitIfReady() {
            if (!emitted && result != null && status != null) {
                emitted = true;
                finish(result, status);
            }
        }
    }

    private void write(ActionRecord record) {
        var event = record.outcome() == ActionRecord.Outcome.SUCCESS ? log.atInfo() : log.atWarn();
        event.addKeyValue("action", record.action()).addKeyValue("outcome", record.outcome().name())
                .addKeyValue("errorCode", record.errorCode()).addKeyValue("requestId", record.requestId())
                .addKeyValue("operationId", record.operationId()).addKeyValue("actorId", record.actorId())
                .addKeyValue("relatedMemberIds", record.relatedMemberIds())
                .addKeyValue(EcsFields.TRACE_ID, record.traceId())
                .addKeyValue(EcsFields.EVENT_DATASET, "mutuus.action")
                .addKeyValue(EcsFields.EVENT_ACTION, record.action())
                .addKeyValue(EcsFields.EVENT_OUTCOME, record.outcome() == ActionRecord.Outcome.SUCCESS ? "success" : "failure")
                .addKeyValue(EcsFields.ERROR_CODE, record.errorCode())
                .addKeyValue("operation.id", record.operationId()).log(record.action());
    }
}
