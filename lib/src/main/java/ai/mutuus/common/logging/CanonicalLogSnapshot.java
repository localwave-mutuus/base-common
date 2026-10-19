package ai.mutuus.common.logging;

import ai.mutuus.common.core.VerifiedIdentityContext;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.HeaderNames;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import io.opentelemetry.api.trace.Span;
import org.slf4j.event.KeyValuePair;

import java.util.*;

/** 비동기 큐에 넘기기 전에 고정하는 schema v2 이벤트. MDC 전체나 사용자 헤더는 복제하지 않는다. */
public final class CanonicalLogSnapshot {
    static final String SNAPSHOT_KEY = "__common_platform_snapshot_v2";
    static final String CONTEXT_KEY = "__common_platform_completion_context";
    private static final TelemetryFieldRegistry REGISTRY = TelemetryFieldRegistry.load();
    private static final Set<String> CONTEXT_FIELDS = REGISTRY.protectedFields();
    private static final Set<String> METADATA_FIELDS = REGISTRY.metadataFields();
    private static final Set<String> ALLOWED = REGISTRY.structuredInputFields();
    private static final Map<String, String> HINTS = Map.of(
            HeaderNames.DEVICE_LEVEL, "golmok.device.level", HeaderNames.LOCALE, "golmok.locale");

    private CanonicalLogSnapshot() { }

    public static Map<String, Object> context() {
        Map<String, Object> result = new LinkedHashMap<>();
        var span = Span.current().getSpanContext();
        if (span.isValid()) {
            result.put("trace.id", span.getTraceId());
            result.put("span.id", span.getSpanId());
            result.put("trace.flags", Byte.toUnsignedInt(span.getTraceFlags().asByte()));
        }
        var identity = VerifiedIdentityContext.current();
        result.putAll(identity.fields());
        result.putAll(ai.mutuus.common.core.TelemetryMetadata.current());
        result.putAll(ai.mutuus.common.core.TelemetryScope.jobMetadata());
        result.put("golmok.scope", ai.mutuus.common.core.TelemetryScope.current());
        ai.mutuus.common.core.ScreenMetadata.fromHeaders(key -> TraceContext.get(key).orElse(null))
                .forEach((header, value) -> result.put(ai.mutuus.common.core.ScreenMetadata.FIELDS.get(header), value));
        HINTS.forEach((header, field) -> TraceContext.get(header).filter(CanonicalLogSnapshot::safeHint)
                .ifPresent(value -> result.put(field, value)));
        TraceContext.get(ai.mutuus.common.core.RequestIds.CONTEXT_KEY).ifPresent(value -> result.put("golmok.request.id", value));
        putNonblank(result, "golmok.application.code", System.getProperty("mutuus.appCode"));
        putNonblank(result, "golmok.instance.code", System.getProperty("mutuus.instanceCode"));
        return Map.copyOf(result);
    }

    static Map<String, Object> requestMetadata(java.util.function.Function<String, String> reader) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (String header : List.of(HeaderNames.DEVICE_LEVEL, HeaderNames.LOCALE)) {
            String value = reader.apply(header);
            if (value != null && safeHint(value)) metadata.put(HINTS.get(header), value);
        }
        ai.mutuus.common.core.ScreenMetadata.fromHeaders(reader).forEach((header, value) -> metadata.put(ai.mutuus.common.core.ScreenMetadata.FIELDS.get(header), value));
        return Map.copyOf(metadata);
    }

    /** 단말의 MDC 키는 읽지 않는다. 실제 OTel Context와 검증된 신원만 출처로 사용한다. */
    static Map<String, Object> capture(ILoggingEvent event) {
        Map<String, Object> flat = new LinkedHashMap<>();
        flat.put("@timestamp", event.getInstant().toString());
        flat.put("event.id", UUID.randomUUID().toString());
        flat.put("golmok.schema.version", 2);
        flat.put("message", event.getFormattedMessage());
        flat.put("log.level", event.getLevel().levelStr.toLowerCase(Locale.ROOT));
        flat.put("log.severity_number", severity(event.getLevel().toInt()));
        flat.put("log.logger", event.getLoggerName());
        flat.putAll(CommonLogResource.current());
        flat.putAll(context());
        // 전파된 부가 메타데이터도 명시 허용 목록만 읽는다. 신원·trace는 MDC로 덮어쓸 수 없다.
        var mdc = event.getMDCPropertyMap();
        for (String key : List.of("session.id", "golmok.device.pseudo_id"))
            putNonblank(flat, key, mdc.get(key));
        captureApprovedMdc(mdc, flat);
        captureUnknownMdc(mdc, flat);
        if (event.getKeyValuePairs() != null) {
            for (KeyValuePair pair : event.getKeyValuePairs()) {
                if (ALLOWED.contains(pair.key) && !CONTEXT_FIELDS.contains(pair.key) && pair.value != null)
                    flat.put(pair.key, freeze(pair.value));
                if (METADATA_FIELDS.contains(pair.key) && pair.value != null)
                    flat.putIfAbsent(pair.key, freeze(pair.value));
                if ("golmok.scope".equals(pair.key) && Set.of("request", "job", "lifecycle", "collector").contains(pair.value))
                    flat.put(pair.key, pair.value);
                if (CONTEXT_KEY.equals(pair.key) && pair.value instanceof Map<?, ?> snapshot) {
                    CONTEXT_FIELDS.forEach(flat::remove);
                    snapshot.forEach((key, value) -> { if (CONTEXT_FIELDS.contains(key) && value != null)
                        flat.put(key.toString(), freeze(value)); });
                }
            }
        }
        // 구 facade에서 들어온 잘못된 ID는 OTel ID인 것처럼 출력하지 않는다.
        if (!validId(flat.get("trace.id"), 32)) flat.remove("trace.id");
        if (!validId(flat.get("span.id"), 16)) flat.remove("span.id");
        if (!validId(flat.get("parent.id"), 16)) flat.remove("parent.id");
        if (event.getThrowableProxy() != null) {
            flat.put("error.type", event.getThrowableProxy().getClassName());
            flat.put("error.stack_trace", ThrowableProxyUtil.asString(event.getThrowableProxy()));
        }
        if ("mutuus.security_audit".equals(flat.get("event.dataset"))) {
            Object address = event.getKeyValuePairs() == null ? flat.get("client.ip") : event.getKeyValuePairs().stream()
                    .filter(pair -> "client.ip".equals(pair.key))
                    .map(pair -> pair.value).filter(Objects::nonNull).findFirst().orElse(flat.get("client.ip"));
            if (address instanceof String ip && ip.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))
                flat.put("client.ip", ip.substring(0, ip.lastIndexOf('.') + 1) + "0");
            else if (address instanceof String ip && ip.matches("[0-9a-fA-F:]+") && ip.contains(":"))
                flat.put("client.ip", "::");
            else flat.remove("client.ip");
        } else {
            flat.remove("client.ip");
        }
        REGISTRY.validateNames(flat);
        return Collections.unmodifiableMap(flat);
    }

    static Map<String, Object> snapshot(ILoggingEvent event) {
        if (event.getKeyValuePairs() != null) {
            for (KeyValuePair pair : event.getKeyValuePairs()) {
                if (SNAPSHOT_KEY.equals(pair.key) && pair.value instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked") Map<String, Object> snapshot = (Map<String, Object>) map;
                    return snapshot;
                }
            }
        }
        throw new IllegalStateException("CommonJsonEncoder requires SnapshotAppender before async dispatch");
    }

    private static Object freeze(Object value) {
        if (value instanceof String || value instanceof Boolean) return value;
        if (value instanceof Number number) {
            if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long
                    || number instanceof Float || number instanceof Double || number instanceof java.math.BigInteger
                    || number instanceof java.math.BigDecimal) return number;
            // AtomicLong 같은 변경 가능한 Number도 발생 시점 값으로 고정한다.
            return number.longValue();
        }
        if (value instanceof Collection<?> values) return values.stream().map(CanonicalLogSnapshot::freeze).toList();
        if (value instanceof Map<?, ?> values) {
            Map<String, Object> result = new LinkedHashMap<>();
            values.forEach((key, item) -> { if (key != null && item != null) result.put(key.toString(), freeze(item)); });
            return Collections.unmodifiableMap(result);
        }
        return String.valueOf(value);
    }
    private static boolean safeHint(String value) { return value.length() <= 64 && value.matches("[A-Za-z0-9_.:-]+"); }

    /** 레지스트리가 명시 승인한 업무 메타데이터만 MDC 입력으로 읽는다. 인증/trace/헤더에서 추정하지 않는다. */
    private static void captureApprovedMdc(Map<String, String> mdc, Map<String, Object> flat) {
        for (String canonical : REGISTRY.mdcInputFields()) {
            String raw = mdc.get(canonical);
            if (raw == null) continue;
            Object normalized = approvedMdcValue(REGISTRY.fields().get(canonical), raw);
            if (normalized != null) flat.put(canonical, normalized);
            else flat.remove(canonical);
        }
    }
    private static Object approvedMdcValue(TelemetryFieldRegistry.Field field, String raw) {
        if (!field.array()) return raw.matches("[A-Za-z0-9_.:-]{1,128}") ? raw : null;
        if (raw.length() > 16384) return null;
        try (com.fasterxml.jackson.core.JsonParser json = new com.fasterxml.jackson.core.JsonFactory().createParser(raw)) {
            if (json.nextToken() != com.fasterxml.jackson.core.JsonToken.START_ARRAY) return null;
            List<String> ids = new ArrayList<>();
            while (json.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY) {
                if (json.currentToken() != com.fasterxml.jackson.core.JsonToken.VALUE_STRING || ids.size() == 64) return null;
                String id = json.getText();
                if (!id.matches("[A-Za-z0-9_.:-]{1,128}")) return null;
                ids.add(id);
            }
            return json.nextToken() == null ? List.copyOf(ids) : null;
        } catch (java.io.IOException invalid) { return null; }
    }

    /** 정본 밖 MDC는 키·값 모두 출력하지 않는다. 미등록 키의 총수만 기록한다. */
    private static void captureUnknownMdc(Map<String, String> mdc, Map<String, Object> flat) {
        int unknown = 0;
        for (String key : mdc.keySet()) {
            if (!REGISTRY.fields().containsKey(key)) unknown++;
        }
        flat.put("golmok.attributes.unmapped_count", unknown);
    }

    static boolean validId(Object value, int length) {
        return value instanceof String id && id.matches("[0-9a-f]{" + length + "}") && !id.matches("0+");
    }
    private static void putNonblank(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
    private static int severity(int level) {
        if (level >= 40000) return 17;
        if (level >= 30000) return 13;
        if (level >= 20000) return 9;
        if (level >= 10000) return 5;
        return 1;
    }
}
