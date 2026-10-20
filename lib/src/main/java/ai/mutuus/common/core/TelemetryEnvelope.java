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

    /** Store this string and VERSION in the same transaction as the single reservation.
     * The storage contract is 8192 characters, distinct from payload UTF-8 limits. */
    public String toStorageString() {
        try {
            var out=new java.io.StringWriter();
            try(var json=new com.fasterxml.jackson.core.JsonFactory().createGenerator(out)) {
                json.writeStartObject(); json.writeNumberField("version",version);
                String[] keys={"traceparent","tracestate","initiatorId","realm","source","eventId","publishedAt","operationId","attemptId"};
                String[] values={traceparent,tracestate,initiatorId,realm,source,eventId,publishedAt.toString(),operationId,attemptId};
                for(int i=0;i<keys.length;i++){if(values[i]==null)json.writeNullField(keys[i]);else json.writeStringField(keys[i],values[i]);}
                json.writeEndObject();
            }
            String result=out.toString();
            if(result.length()>8192)throw new IllegalArgumentException("Envelope storage limit exceeded");
            return result;
        }catch(java.io.IOException ex){throw new IllegalArgumentException("Envelope serialization failed");}
    }
    public static TelemetryEnvelope fromStorageString(String stored) {
        if(stored==null || stored.length()>8192)throw new IllegalArgumentException("Invalid stored envelope");
        try(var parser=com.fasterxml.jackson.core.JsonFactory.builder().enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .streamReadConstraints(com.fasterxml.jackson.core.StreamReadConstraints.builder().maxNestingDepth(2).maxStringLength(8192).build()).build().createParser(stored)) {
            if(parser.nextToken()!=com.fasterxml.jackson.core.JsonToken.START_OBJECT)throw new IllegalArgumentException("Invalid stored envelope");
            Map<String,String> values=new LinkedHashMap<>();
            var keys=java.util.Set.of("version","traceparent","tracestate","initiatorId","realm","source","eventId","publishedAt","operationId","attemptId");
            while(parser.nextToken()!=com.fasterxml.jackson.core.JsonToken.END_OBJECT){
                if(parser.currentToken()!=com.fasterxml.jackson.core.JsonToken.FIELD_NAME || !keys.contains(parser.currentName()))throw new IllegalArgumentException("Invalid stored envelope field");
                String key=parser.currentName();var token=parser.nextToken();
                if(key.equals("version") ? token!=com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT : token!=com.fasterxml.jackson.core.JsonToken.VALUE_STRING && token!=com.fasterxml.jackson.core.JsonToken.VALUE_NULL)
                    throw new IllegalArgumentException("Invalid stored envelope type");
                values.put(key,token==com.fasterxml.jackson.core.JsonToken.VALUE_NULL?null:parser.getText());
            }
            if(parser.nextToken()!=null || !values.keySet().equals(keys))throw new IllegalArgumentException("Invalid stored envelope schema");
            return new TelemetryEnvelope(Integer.parseInt(values.get("version")),values.get("traceparent"),values.get("tracestate"),values.get("initiatorId"),values.get("realm"),values.get("source"),values.get("eventId"),Instant.parse(values.get("publishedAt")),values.get("operationId"),values.get("attemptId"));
        }catch(Exception ex){throw new IllegalArgumentException("Invalid stored envelope");}
    }

    /** validatedOwner must come from authenticated storage authorization, never an ingress header.
     * Retry reuses the original ingress parent but must supply a new attempt ID. */
    public static StoredConsumer resumeStored(String stored,int schemaVersion,String storageOwner,String validatedOwner,String nextAttemptId) {
        String reason=null;TelemetryEnvelope envelope=null;
        if(validatedOwner==null || !TOKEN.matcher(validatedOwner).matches() || storageOwner==null || !validatedOwner.equals(storageOwner)) reason="ENVELOPE_OWNER_MISMATCH";
        else if(stored==null) reason="ENVELOPE_MISSING";
        else try {
            if(schemaVersion!=VERSION)throw new IllegalArgumentException();
            var original=fromStorageString(stored);
            if(original.attemptId().equals(nextAttemptId))throw new IllegalArgumentException("New attempt ID required");
            envelope=original.withAttemptId(nextAttemptId);
        }catch(RuntimeException invalid){reason="ENVELOPE_INVALID";}
        if(envelope!=null)return new StoredConsumer(envelope.openConsumerFromOwnerValidatedStorage("single.email"),null);
        token(nextAttemptId,"attemptId",false);
        Span span=GlobalOpenTelemetry.getTracer("ai.mutuus.common.envelope").spanBuilder("single.email").setSpanKind(SpanKind.CONSUMER).setNoParent().startSpan();
        String execution=UUID.randomUUID().toString();span.setAttribute("execution.id",execution).setAttribute("golmok.attempt.id",nextAttemptId).setAttribute("error.code",reason);
        Context context=TelemetryMetadata.withValues(TelemetryScope.with(Context.root().with(span),"job"),new TelemetryMetadata.Values(null,nextAttemptId,null,execution));
        return new StoredConsumer(new ConsumerScope(span,context.makeCurrent()),reason);
    }
    public record StoredConsumer(ConsumerScope scope,String reason) implements AutoCloseable {
        public ai.mutuus.common.logging.JobPairLogger.Scope beginJob(String jobName,String operationId){return new ai.mutuus.common.logging.JobPairLogger().beginInCurrentSpan(jobName,operationId);}
        @Override public void close(){scope.close();}
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