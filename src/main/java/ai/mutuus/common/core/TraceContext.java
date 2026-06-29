package ai.mutuus.common.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 요청 단위 e2e 추적 컨텍스트.
 * <p>ThreadLocal 기반으로 추적ID/화면ID/이벤트ID/단말정보/사용자 등을 보관한다.
 * 가상 스레드 및 비동기 전파 시에는 {@code common-web}의 컨텍스트 전파 유틸과 함께 사용한다.
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
