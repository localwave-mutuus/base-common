package ai.mutuus.common.core;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;

/** Stage B 저장 owner가 업무 변경과 같은 트랜잭션으로 보관하는 불변 추적 봉투.
 * <p>인증/발행 권한이 검증된 저장소만 복원한다. HTTP 입력의 신원 주장이나 baggage/JWT를 받지 않는다.
 * 잘못된 봉투/전파 설정/현재 span 부재는 예외로 거부하며 추적 ID를 만들어 보완하지 않는다.
 */
public record TelemetryEnvelope(int version, String traceparent, String tracestate,
        String initiatorId, String realm, String source, String eventId, Instant publishedAt,
        String operationId, String attemptId) {
    public static final int VERSION = 1;
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}");
    private static final Pattern REALM = Pattern.compile("[a-z][a-z0-9._-]{0,63}");
    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override public Iterable<String> keys(Map<String, String> carrier) { return carrier.keySet(); }
        @Override public String get(Map<String, String> carrier, String key) { return carrier.get(key); }
    };

    public TelemetryEnvelope {
        if (version != VERSION) throw new IllegalArgumentException("Unsupported telemetry envelope version");
        bounded(traceparent, "traceparent", 512, false);
        bounded(tracestate, "tracestate", 512, true);
        bounded(initiatorId, "initiatorId", 256, true);
        if ((initiatorId == null) != (realm == null) || (initiatorId == null) != (source == null)
                || (realm != null && (!REALM.matcher(realm).matches() || !"verified_initiator".equals(source)))) {
            throw new IllegalArgumentException("Verified initiator ID/realm/source must be paired");
        }
        try {
            if (!UUID.fromString(eventId).toString().equals(eventId)) throw new IllegalArgumentException("Invalid eventId");
        } catch (NullPointerException ex) { throw new IllegalArgumentException("Missing eventId", ex); }
        Objects.requireNonNull(publishedAt, "publishedAt");
        token(operationId, "operationId", true);
        token(attemptId, "attemptId", false);
        validatedParent(traceparent, tracestate);
    }

    /** 제출 시점의 실제 OTel Context/검증 사용자만 캡처한다. SDK/provider를 설치하지 않는다. */
    public static TelemetryEnvelope captureCurrentVerified(String operationId, String attemptId) {
        if (!Span.current().getSpanContext().isValid()) throw new IllegalStateException("Current valid span required");
        Map<String, String> carrier = inject(Context.current());
        var identity = VerifiedIdentityContext.current();
        return new TelemetryEnvelope(VERSION, carrier.get("traceparent"), carrier.get("tracestate"),
                identity.userId(), identity.realm(), identity.userId() == null ? null : "verified_initiator",
                UUID.randomUUID().toString(), Instant.now(), operationId, attemptId);
    }

    /** 저장 owner의 신뢰/권한 검증 후에만 호출한다. 주변 스레드/baggage는 복원 부모에 섞이지 않는다. */
    public Context extractFromOwnerValidatedStorage() {
        Context parent = validatedParent(traceparent, tracestate);
        var identity = initiatorId == null ? new VerifiedIdentityContext.Identity(null, null, "workload")
                : new VerifiedIdentityContext.Identity(initiatorId, realm, "authenticated", source);
        // makeCurrent 범위는 즉시 복원하고 불변 Context만 반환한다.
        try (Scope ignored = parent.makeCurrent(); Scope user = VerifiedIdentityContext.makeCurrent(identity)) {
            return Context.current();
        }
    }

    public TelemetryEnvelope withAttemptId(String nextAttemptId) {
        return new TelemetryEnvelope(version, traceparent, tracestate, initiatorId, realm, source,
                eventId, publishedAt, operationId, nextAttemptId);
    }

    /** owner가 직접 계측하는 큐 경계용. 이미 agent가 CONSUMER를 만드는 경계에 중복 적용하지 않는다. */
    public ConsumerScope openConsumerFromOwnerValidatedStorage(String spanName) {
        bounded(spanName, "spanName", 128, false);
        Context parent = extractFromOwnerValidatedStorage();
        Span span = GlobalOpenTelemetry.getTracer("ai.mutuus.common.envelope")
                .spanBuilder(spanName).setSpanKind(SpanKind.CONSUMER).setParent(parent).startSpan();
        String executionId = UUID.randomUUID().toString();
        span.setAttribute("golmok.message.event.id", eventId).setAttribute("golmok.attempt.id", attemptId)
                .setAttribute("execution.id", executionId);
        if (operationId != null) span.setAttribute("operation.id", operationId);
        span.setAttribute("golmok.auth.state", initiatorId == null ? "workload" : "authenticated");
        if (initiatorId != null) span.setAttribute("user.id", initiatorId)
                .setAttribute("golmok.identity.realm", realm).setAttribute("golmok.identity.source", source);
        return new ConsumerScope(span, TelemetryMetadata.withValues(TelemetryScope.with(parent.with(span), "job"),
                new TelemetryMetadata.Values(operationId, attemptId, eventId, executionId)).makeCurrent());
    }

    /** finally/try-with-resources에서 반드시 닫는다. 실패는 명시 표시하고 토큰/원문 오류 본문을 기록하지 않는다. */
    public static final class ConsumerScope implements AutoCloseable {
        private final Span span;
        private final Scope scope;
        private boolean closed;
        private ConsumerScope(Span span, Scope scope) { this.span = span; this.scope = scope; }
        public Span span() { return span; }
        public void fail() { span.setStatus(StatusCode.ERROR); }
        @Override public void close() {
            if (!closed) {
                closed = true;
                try { scope.close(); } finally { span.end(); }
            }
        }
    }

    private static Context validatedParent(String traceparent, String tracestate) {
        Map<String, String> carrier = new LinkedHashMap<>();
        carrier.put("traceparent", traceparent);
        if (tracestate != null) carrier.put("tracestate", tracestate);
        var w3c = W3CTraceContextPropagator.getInstance();
        Context expected = w3c.extract(Context.root(), carrier, GETTER);
        var expectedSpan = Span.fromContext(expected).getSpanContext();
        if (!expectedSpan.isValid()) throw new IllegalArgumentException("Invalid W3C traceparent");
        Map<String, String> canonical = new LinkedHashMap<>();
        w3c.inject(expected, canonical, Map::put);
        if (!Objects.equals(traceparent, canonical.get("traceparent"))
                || !Objects.equals(tracestate, canonical.get("tracestate"))) {
            throw new IllegalArgumentException("Noncanonical W3C envelope");
        }
        Context extracted = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), carrier, GETTER);
        if (!Span.fromContext(extracted).getSpanContext().equals(expectedSpan)) {
            throw new IllegalStateException("Global W3C propagator required");
        }
        // 사용자 정의 전파기가 추가한 baggage 등도 제거하고 검증된 W3C 부모만 남긴다.
        return Context.root().with(Span.wrap(Span.fromContext(extracted).getSpanContext()));
    }

    private static Map<String, String> inject(Context context) {
        Map<String, String> carrier = new LinkedHashMap<>();
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, carrier, (map, key, value) -> {
            if ("traceparent".equals(key) || "tracestate".equals(key)) map.put(key, value);
        });
        return carrier;
    }
    private static void token(String value, String name, boolean optional) {
        if (value == null && optional) return;
        if (value == null || !TOKEN.matcher(value).matches()) throw new IllegalArgumentException("Invalid " + name);
    }
    private static void bounded(String value, String name, int max, boolean optional) {
        if (value == null && optional) return;
        if (value == null || value.isBlank() || value.length() > max || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid " + name);
    }
}