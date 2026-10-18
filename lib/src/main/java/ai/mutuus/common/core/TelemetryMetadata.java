package ai.mutuus.common.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;

/** 저장 owner가 검증한 메시지 식별자만 전파한다. 로그별 event.id와 메시지 event ID는 서로 다르다. */
public final class TelemetryMetadata {
    private static final ContextKey<Values> KEY = ContextKey.named("common.verified-message-metadata");
    private TelemetryMetadata() { }
    public record Values(String operationId, String attemptId, String messageEventId, String executionId) {
        public Values {
            for (String token : new String[]{operationId, attemptId}) {
                if (token != null && !token.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}"))
                    throw new IllegalArgumentException("Invalid message operation/attempt metadata");
            }
            for (String id : new String[]{messageEventId, executionId}) {
                if (id != null && !UUID.fromString(id).toString().equals(id))
                    throw new IllegalArgumentException("Invalid message/execution ID");
            }
        }
    }
    public static Map<String, String> current() {
        Values value = Context.current().get(KEY);
        if (value == null) return Map.of();
        Map<String, String> fields = new LinkedHashMap<>();
        if (value.operationId() != null) fields.put("operation.id", value.operationId());
        if (value.attemptId() != null) fields.put("golmok.attempt.id", value.attemptId());
        if (value.messageEventId() != null) fields.put("golmok.message.event.id", value.messageEventId());
        if (value.executionId() != null) fields.put("execution.id", value.executionId());
        return Map.copyOf(fields);
    }
    public static Scope makeCurrent(Values values) { return withValues(Context.current(), values).makeCurrent(); }
    static Context withValues(Context context, Values values) {
        return context.with(KEY, java.util.Objects.requireNonNull(values));
    }
}