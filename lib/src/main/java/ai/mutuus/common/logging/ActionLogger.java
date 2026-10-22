package ai.mutuus.common.logging;

import org.slf4j.LoggerFactory;
import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;

/** 업무 결과 전용 writer. 자동구성 빈은 현재 TX 완료까지 기다리며 순수 생성자는 no-TX 전용이다. */
public final class ActionLogger {
    static final String VALIDATED_ACTION_KEY = "__common_validated_action";
    static final Object VALIDATED_ACTION = new Object();
    public static final String LOGGER_NAME = "ai.mutuus.common.action";
    private final ActionCompletionHandler completion;

    /** Spring TX 없는 소비자/확정 결과용. TX 소비자는 자동구성 빈을 주입받는다. */
    public ActionLogger() { this(callback -> callback.accept(ActionCompletionHandler.Status.NO_TRANSACTION)); }

    public ActionLogger(ActionCompletionHandler completion) {
        this.completion = completion == null ? callback -> callback.accept(ActionCompletionHandler.Status.UNKNOWN) : completion;
    }

    /** no-TX는 실제 결과 확인 후, TX는 업무 owner 경계에서 한 번 호출한다. */
    public void record(ActionRecord record) {
        try {
            if (!valid(record)) return;
            var snapshot = CanonicalLogSnapshot.context();
            completion.complete(status -> finish(record, status, snapshot));
        } catch (Throwable ignored) { diagnostic("ACTION_LOG_FAILED", record == null ? null : record.requestId()); }
    }

    /** owner의 최외곽 TX에 진입한 즉시 바인딩한다. REQUIRES_NEW 내부에서 새로 바인딩하지 않는다. */
    public BoundAction bind() {
        String requestId = null, traceId = null;
        try { requestId = RequestIds.current(); traceId = TraceContext.traceId(); }
        catch (Throwable ignored) { diagnostic("ACTION_CONTEXT_UNAVAILABLE", requestId); }
        return bind(requestId, traceId);
    }

    /** 배치는 실제 전달된 원 요청 ID 또는 명시적인 invocation ID를 공급한다. */
    public BoundAction bind(String requestId, String traceId) {
        var bound = new BoundAction(requestId, traceId);
        try { completion.complete(bound::completed); }
        catch (Throwable ignored) {
            bound.completed(ActionCompletionHandler.Status.UNKNOWN);
            diagnostic("ACTION_LOG_FAILED", bound.requestId);
        }
        return bound;
    }

    private void finish(ActionRecord record, ActionCompletionHandler.Status status, java.util.Map<String, Object> snapshot) {
        try {
            if (status == null || status == ActionCompletionHandler.Status.UNKNOWN)
                diagnostic("ACTION_COMPLETION_UNCONFIRMED", record.requestId());
            else write(status == ActionCompletionHandler.Status.ROLLED_BACK ? record.rolledBack() : record, snapshot);
        } catch (Throwable ignored) { diagnostic("ACTION_LOG_FAILED", record == null ? null : record.requestId()); }
    }

    private void diagnostic(String code, String requestId) {
        try {
            LoggerFactory.getLogger(LOGGER_NAME).atWarn()
                .addKeyValue("event.dataset", "mutuus.action_diagnostic")
                .addKeyValue("event.action", "action.logging.diagnostic")
                .addKeyValue("golmok.diagnostic.code", code)
                .addKeyValue("golmok.request.id", ActionRecord.safeId(requestId))
                .log("action logging diagnostic");
        } catch (Throwable ignored) { /* 진단 writer도 실패하면 중단한다. raw 값/예외를 재출력하지 않는다. */ }
    }

    private boolean valid(ActionRecord record) {
        if (record != null && record.validForLogging()) return true;
        diagnostic("ACTION_RECORD_INVALID", record == null ? null : record.requestId());
        return false;
    }

    static void completionFailed() { new ActionLogger().diagnostic("ACTION_COMPLETION_FAILED", null); }

    /** TX callback과 async 확정 결과의 도착 순서에 의존하지 않고 한 번만 기록한다. */
    public final class BoundAction {
        private final String requestId, traceId;
        private final java.util.Map<String, Object> snapshot = CanonicalLogSnapshot.context();
        private ActionRecord result;
        private ActionCompletionHandler.Status status;
        private boolean emitted;

        private BoundAction(String requestId, String traceId) {
            this.requestId = ActionRecord.safeId(requestId); this.traceId = ActionRecord.safeTraceId(traceId);
        }

        public synchronized void record(ActionRecord confirmedResult) {
            try {
                if (result != null) { diagnostic("ACTION_RESULT_DUPLICATE", requestId); return; }
                if (!valid(confirmedResult)) return;
                result = new ActionRecord(confirmedResult.action(), confirmedResult.outcome(), confirmedResult.errorCode(),
                        requestId, confirmedResult.operationId(), confirmedResult.actorId(), confirmedResult.relatedMemberIds(), traceId);
                emitIfReady();
            } catch (Throwable ignored) { diagnostic("ACTION_LOG_FAILED", requestId); }
        }

        private synchronized void completed(ActionCompletionHandler.Status completedStatus) {
            try {
                if (status != null) return;
                status = completedStatus == null ? ActionCompletionHandler.Status.UNKNOWN : completedStatus;
                emitIfReady();
            } catch (Throwable ignored) { diagnostic("ACTION_LOG_FAILED", requestId); }
        }

        private void emitIfReady() {
            if (!emitted && result != null && status != null) {
                emitted = true;
                finish(result, status, snapshot);
            }
        }
    }

    private void write(ActionRecord record, java.util.Map<String, Object> snapshot) {
        var log = LoggerFactory.getLogger(LOGGER_NAME);
        var event = record.outcome() == ActionRecord.Outcome.SUCCESS ? log.atInfo() : log.atWarn();
        event.addKeyValue(VALIDATED_ACTION_KEY, VALIDATED_ACTION)
                .addKeyValue(CanonicalLogSnapshot.CONTEXT_KEY, snapshot)
                .addKeyValue("golmok.request.id", record.requestId())
                .addKeyValue("golmok.actor.id", record.actorId())
                .addKeyValue("golmok.related_member.ids", record.relatedMemberIds())
                .addKeyValue(EcsFields.EVENT_DATASET, "mutuus.action")
                .addKeyValue(EcsFields.EVENT_ACTION, record.action())
                .addKeyValue(EcsFields.EVENT_OUTCOME, record.outcome() == ActionRecord.Outcome.SUCCESS ? "success" : "failure")
                .addKeyValue(EcsFields.ERROR_CODE, record.errorCode())
                .addKeyValue("operation.id", record.operationId()).log(record.action());
    }
}
