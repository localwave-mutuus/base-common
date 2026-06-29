package ai.mutuus.common.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 요청 단위 e2e 추적 컨텍스트.
 * <p>ThreadLocal 기반으로 추적ID/화면ID/이벤트ID/단말정보/사용자 등을 보관한다.
 * 가상 스레드 및 비동기({@code @Async}) 전파 시에는
 * {@code ai.mutuus.common.async.TraceContextPropagation} 와 함께 사용한다.
 */
public final class TraceContext {

    private static final ThreadLocal<Map<String, String>> HOLDER =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private TraceContext() {
    }

    public static void put(String key, String value) {
        if (value != null) {
            HOLDER.get().put(key, value);
        }
    }

    public static Optional<String> get(String key) {
        return Optional.ofNullable(HOLDER.get().get(key));
    }

    public static Map<String, String> snapshot() {
        return Map.copyOf(HOLDER.get());
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
        return get(HeaderNames.TRACE_ID).orElse(null);
    }

    public static String userId() {
        return get(HeaderNames.USER_ID).orElse(null);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
