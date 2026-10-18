package ai.mutuus.common.core;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import java.util.Objects;
import java.util.Set;

/** 추적 신호의 실행 범위. 제출 시 Context에 포함되어 일반 업무 로그에도 전파된다. */
public final class TelemetryScope {
    private static final ContextKey<String> KEY = ContextKey.named("common.telemetry-scope");
    private static final Set<String> VALUES = Set.of("request", "job", "lifecycle", "collector");
    private TelemetryScope() { }

    public static String current() {
        String explicit = Context.current().get(KEY);
        return explicit != null ? explicit : Span.current().getSpanContext().isValid() ? "request" : "lifecycle";
    }

    public static Context with(Context parent, String value) {
        if (!VALUES.contains(Objects.requireNonNull(value))) throw new IllegalArgumentException("Unsupported telemetry scope");
        return Objects.requireNonNull(parent).with(KEY, value);
    }
}
