package ai.mutuus.common.logging;

import java.util.List;
import java.util.ArrayList;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;

/** owner가 확인한 업무 결과·식별 snapshot. 원문 개인정보/예외/업무 payload는 받지 않는다. */
public record ActionRecord(String action, Outcome outcome, String errorCode, String requestId,
                           String operationId, String actorId, List<String> relatedMemberIds,
                           String traceId) {
    public enum Outcome { SUCCESS, FAILURE }

    public ActionRecord {
        requestId = safeId(requestId); operationId = safeId(operationId); actorId = safeId(actorId);
        traceId = safeTraceId(traceId);
        relatedMemberIds = safeMembers(relatedMemberIds);
    }

    /** 호출 스레드의 requestId/traceId를 캡처한다. actorId는 owner가 검증한 memberId만 전달한다. */
    public static ActionRecord capture(String action, Outcome outcome, String errorCode,
                                       String operationId, String actorId, List<String> relatedMemberIds) {
        String requestId = null, traceId = null;
        try { requestId = RequestIds.current(); traceId = TraceContext.traceId(); }
        catch (Throwable ignored) { /* 로깅 컨텍스트 실패는 업무에 전파하지 않는다. */ }
        return new ActionRecord(action, outcome, errorCode, requestId, operationId,
                actorId, relatedMemberIds, traceId);
    }

    ActionRecord rolledBack() {
        return outcome == Outcome.FAILURE ? this : new ActionRecord(action, Outcome.FAILURE,
                "TRANSACTION_ROLLED_BACK", requestId, operationId, actorId, relatedMemberIds, traceId);
    }

    boolean validForLogging() {
        return action != null && action.length() <= 128 && action.matches("[a-z][a-z0-9]*(?:[._-][a-z0-9]+){1,15}")
                && outcome != null && (outcome == Outcome.SUCCESS ? errorCode == null
                : errorCode != null && errorCode.length() <= 80 && errorCode.matches("[A-Z][A-Z0-9_]{0,79}"));
    }

    static String safeId(String id) {
        return id != null && id.length() <= 128 && id.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}") ? id : null;
    }

    /** common 32 hex 또는 batch UUID 형식만 허용한다. 불신 헤더 원문은 버린다. */
    static String safeTraceId(String id) {
        return id != null && (id.length() == 32 && id.matches("[a-fA-F0-9]{32}")
                || id.length() == 36 && id.matches("[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")) ? id : null;
    }

    private static List<String> safeMembers(List<String> members) {
        if (members == null) return List.of();
        try {
            var safe = new ArrayList<String>();
            for (String id : members) if (safeId(id) != null) safe.add(id);
            return List.copyOf(safe);
        } catch (Throwable ignored) { return List.of(); }
    }
}
