package ai.mutuus.common.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import io.opentelemetry.api.trace.Span;

/**
 * OTel 현재 span 및 검증된 사용자 문맥의 호환 facade.
 * <p>업무 힌트만 ThreadLocal로 보관하며 추적/사용자 식별자는 임의 값으로 덮어쓸 수 없다.
 * 가상 스레드 및 비동기({@code @Async}) 전파 시에는
 * {@code ai.mutuus.common.async.TraceContextPropagation} 와 함께 사용한다.
 */
public final class TraceContext {

    private static final ThreadLocal<Map<String, String>> HOLDER =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private TraceContext() {
    }

    public static void put(String key, String value) {
        if (value != null && !HeaderNames.TRACE_ID.equals(key) && !HeaderNames.SPAN_ID.equals(key)
                && !HeaderNames.USER_ID.equals(key)) {
            HOLDER.get().put(key, value);
        }
    }

    public static Optional<String> get(String key) {
        if (HeaderNames.TRACE_ID.equals(key)) return Optional.ofNullable(traceId());
        if (HeaderNames.SPAN_ID.equals(key)) return Optional.ofNullable(spanId());
        if (HeaderNames.USER_ID.equals(key)) return Optional.ofNullable(userId());
        return Optional.ofNullable(HOLDER.get().get(key));
    }

    public static void remove(String key) {
        HOLDER.get().remove(key);
    }

    public static Map<String, String> snapshot() {
        Map<String, String> result = new HashMap<>(HOLDER.get());
        if (traceId() != null) result.put(HeaderNames.TRACE_ID, traceId());
        if (spanId() != null) result.put(HeaderNames.SPAN_ID, spanId());
        if (userId() != null) result.put(HeaderNames.USER_ID, userId());
        return Map.copyOf(result);
    }

    /**
     * 현재 스레드의 컨텍스트를 주어진 스냅샷으로 통째로 교체한다(기존 값은 제거).
     * <p>스레드 경계를 넘는 컨텍스트 전파/복원에 사용한다
     * ({@code ai.mutuus.common.async.TraceContextPropagation}).
     */
    public static void replace(Map<String, String> context) {
        clear();
        if (context != null) {
            context.forEach(TraceContext::put);
        }
    }

    public static String traceId() {
        var context = Span.current().getSpanContext();
        return context.isValid() ? context.getTraceId() : null;
    }

    public static String spanId() {
        var context = Span.current().getSpanContext();
        return context.isValid() ? context.getSpanId() : null;
    }

    public static String userId() {
        return VerifiedIdentityContext.currentUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
