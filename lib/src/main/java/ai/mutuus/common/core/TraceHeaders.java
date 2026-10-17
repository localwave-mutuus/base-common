package ai.mutuus.common.core;

import java.net.http.HttpRequest;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Function;

/** Servlet/Spring 의존 없이 HTTP 상관키를 해석하고 java.net.http 호출에 전파한다. */
public final class TraceHeaders {
    public static final String TRACEPARENT = "traceparent";
    public static final String W3C_ENABLED = "mutuus.common.tracing.w3c";
    public static final String W3C_RUNTIME_PROPERTY = "mutuus.tracingW3c";
    public static final String REQUEST_ATTRIBUTE = TraceHeaders.class.getName() + ".inbound";

    private TraceHeaders() { }

    /** HTTP 요청에서 한 번 만들고 두 진입 필터가 같은 값을 공유한다. */
    public record Inbound(String traceId, String spanId, String parentId, String flags) { }

    public static Inbound inbound(Function<String, String> headers, boolean w3c) {
        String[] parent = w3c ? parse(headers.apply(TRACEPARENT)) : null;
        String trace = text(headers.apply(HeaderNames.TRACE_ID));
        String customParent = text(headers.apply(HeaderNames.SPAN_ID));
        // 커스텀 trace가 우선이다. 서로 다른 traceparent의 parent를 혼합하지 않는다.
        boolean compatible = parent != null && (trace == null || trace.equals(parent[0]));
        if (trace == null) trace = parent == null ? IdGenerator.newTraceId() : parent[0];
        return new Inbound(trace, IdGenerator.newSpanId(),
                customParent != null ? customParent : compatible ? parent[1] : null,
                compatible ? parent[2] : "01");
    }

    /** 현재 서비스 span을 다음 서비스의 parent로 보낸다. 레거시 trace는 X-*로만 유지한다. */
    public static Map<String, String> outbound(boolean w3c) {
        Map<String, String> headers = new LinkedHashMap<>();
        String trace = text(TraceContext.traceId());
        String span = TraceContext.get(HeaderNames.SPAN_ID).map(TraceHeaders::text).orElse(null);
        if (span == null) span = IdGenerator.newSpanId();
        if (trace != null) headers.put(HeaderNames.TRACE_ID, trace);
        headers.put(HeaderNames.SPAN_ID, span);
        if (w3c && validHex(trace, 32) && validHex(span, 16)) {
            String flags = TraceContext.get(TRACEPARENT).map(TraceHeaders::parse)
                    .map(parts -> parts[2]).orElse("01");
            headers.put(TRACEPARENT, "00-" + trace + "-" + span + "-" + flags);
        }
        return headers;
    }

    /** TraceFilter가 보관한 토글을 따른다. 비웹 호출자는 명시적 boolean 오버로드를 쓸 수 있다. */
    public static HttpRequest.Builder apply(HttpRequest.Builder builder) {
        return apply(builder, TraceContext.get(W3C_ENABLED).map(Boolean::parseBoolean)
                .orElseGet(() -> Boolean.parseBoolean(System.getProperty(W3C_RUNTIME_PROPERTY, "true"))));
    }

    public static HttpRequest.Builder apply(HttpRequest.Builder builder, boolean w3c) {
        outbound(w3c).forEach(builder::setHeader);
        return builder;
    }

    private static String[] parse(String value) {
        if (value == null || value.length() != 55 || !value.startsWith("00-")) return null;
        String[] parts = value.split("-", -1);
        if (parts.length != 4 || !validHex(parts[1], 32) || !validHex(parts[2], 16)
                || !parts[3].matches("[0-9a-f]{2}")) return null;
        return new String[] {parts[1], parts[2], parts[3]};
    }

    private static boolean validHex(String value, int length) {
        return value != null && value.matches("[0-9a-f]{" + length + "}")
                && !value.equals("0".repeat(length));
    }

    private static String text(String value) { return value == null || value.isBlank() ? null : value; }
}
