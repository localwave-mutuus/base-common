package ai.mutuus.common.core;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** OTel 전파 호환 facade. HTTP span과 자동 계측의 주입은 Java agent가 소유한다. */
public final class TraceHeaders {
    public static final String TRACEPARENT = "traceparent";
    public static final String W3C_ENABLED = "mutuus.common.tracing.w3c";
    public static final String W3C_RUNTIME_PROPERTY = "mutuus.tracingW3c";
    public static final String REQUEST_ATTRIBUTE = TraceHeaders.class.getName() + ".inbound";
    private TraceHeaders() { }
    public record Inbound(String traceId, String spanId, String parentId, String flags) { }

    /** 현재 SERVER span만 식별자로 사용한다. 받은 부모는 검증된 remote context로만 참조한다. */
    public static Inbound inbound(Function<String, String> headers, boolean w3c) {
        var current = Span.current().getSpanContext();
        Context remote = w3c ? GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), headers, new TextMapGetter<Function<String, String>>() {
                    public Iterable<String> keys(Function<String, String> carrier) {
                        return java.util.List.of(TRACEPARENT, "tracestate");
                    }
                    public String get(Function<String, String> carrier, String key) {
                        return TRACEPARENT.equalsIgnoreCase(key) || "tracestate".equalsIgnoreCase(key)
                                ? carrier.apply(key) : null;
                    }
                }) : Context.root();
        var parent = Span.fromContext(remote).getSpanContext();
        return new Inbound(current.isValid() ? current.getTraceId() : null,
                current.isValid() ? current.getSpanId() : null,
                parent.isValid() && parent.isRemote() && current.isValid()
                        && parent.getTraceId().equals(current.getTraceId()) ? parent.getSpanId() : null,
                current.getTraceFlags().asHex());
    }

    /** 자동 계측이 없는 전송 경계에서만 명시적으로 사용한다. span을 새로 생성하지 않는다. */
    public static Map<String, String> outbound(boolean w3c) {
        Map<String, String> result = new LinkedHashMap<>();
        if (w3c) GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .inject(Context.current(), result, (carrier, key, value) -> {
                    if (TRACEPARENT.equalsIgnoreCase(key) || "tracestate".equalsIgnoreCase(key)) carrier.put(key, value);
                });
        return Map.copyOf(result);
    }

    /** JDK HTTP client의 CLIENT span/주입은 agent가 담당한다. */
    public static HttpRequest.Builder apply(HttpRequest.Builder builder) { return builder; }
    public static HttpRequest.Builder apply(HttpRequest.Builder builder, boolean w3c) { return builder; }
}
