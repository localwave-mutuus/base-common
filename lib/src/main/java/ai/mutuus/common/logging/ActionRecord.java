package ai.mutuus.common.logging;

import java.util.List;
import java.util.Objects;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;

/** owner가 확인한 업무 결과·식별 snapshot. 원문 개인정보/예외/업무 payload는 받지 않는다. */
public record ActionRecord(String action, Outcome outcome, String errorCode, String requestId,
                           String operationId, String actorId, List<String> relatedMemberIds,
                           String traceId) {
    public enum Outcome { SUCCESS, FAILURE }

    public ActionRecord {
        if (action == null || !action.matches("[a-z][a-z0-9]*(?:[._-][a-z0-9]+){1,15}") || action.length() > 128)
            throw new IllegalArgumentException("fixed action code required");
        Objects.requireNonNull(outcome, "outcome required");
        if (outcome == Outcome.SUCCESS && errorCode != null)
            throw new IllegalArgumentException("SUCCESS errorCode must be null");
        if (outcome == Outcome.FAILURE && (errorCode == null || !errorCode.matches("[A-Z][A-Z0-9_]{0,79}")))
            throw new IllegalArgumentException("fixed FAILURE errorCode required");
        checkId(requestId); checkId(operationId); checkId(actorId); checkId(traceId);
        relatedMemberIds = List.copyOf(Objects.requireNonNull(relatedMemberIds, "relatedMemberIds required"));
        relatedMemberIds.forEach(ActionRecord::checkRequiredId);
    }

    /** 호출 스레드의 requestId/traceId를 캡처한다. actorId는 owner가 검증한 memberId만 전달한다. */
    public static ActionRecord capture(String action, Outcome outcome, String errorCode,
                                       String operationId, String actorId, List<String> relatedMemberIds) {
        return new ActionRecord(action, outcome, errorCode, RequestIds.current(), operationId,
                actorId, relatedMemberIds, TraceContext.traceId());
    }

    ActionRecord rolledBack() {
        return outcome == Outcome.FAILURE ? this : new ActionRecord(action, Outcome.FAILURE,
                "TRANSACTION_ROLLED_BACK", requestId, operationId, actorId, relatedMemberIds, traceId);
    }

    private static void checkRequiredId(String id) {
        Objects.requireNonNull(id, "memberId required"); checkId(id);
    }

    private static void checkId(String id) {
        if (id != null && !id.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}"))
            throw new IllegalArgumentException("opaque identifier required");
    }
}
