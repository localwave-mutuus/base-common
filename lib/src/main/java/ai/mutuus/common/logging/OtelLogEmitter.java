package ai.mutuus.common.logging;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/** agent가 소유한 LogsBridge로 동일 snapshot을 보낸다. SDK/provider/exporter를 새로 만들지 않는다. */
final class OtelLogEmitter {
    private OtelLogEmitter() { }

    static void emit(Map<String, Object> snapshot) {
        var builder = GlobalOpenTelemetry.get().getLogsBridge()
                .loggerBuilder(String.valueOf(snapshot.get("log.logger"))).setInstrumentationVersion("0.4.0").build()
                .logRecordBuilder().setTimestamp(Instant.parse(snapshot.get("@timestamp").toString()))
                .setBody(String.valueOf(snapshot.get("message")))
                .setSeverity(severity(((Number) snapshot.get("log.severity_number")).intValue()))
                .setSeverityText(String.valueOf(snapshot.get("log.level")));
        // 완료 callback의 스레드에는 원 span scope가 이미 없을 수 있다. 새 span은 만들지 않는다.
        Context context = Context.root();
        Object trace = snapshot.get("trace.id"), span = snapshot.get("span.id");
        if (CanonicalLogSnapshot.validId(trace, 32) && CanonicalLogSnapshot.validId(span, 16)) {
            int flags = snapshot.get("trace.flags") instanceof Number number ? number.intValue() : 0;
            context = context.with(Span.wrap(SpanContext.create(trace.toString(), span.toString(),
                    TraceFlags.fromByte((byte) flags), TraceState.getDefault())));
        }
        builder.setContext(context);
        snapshot.forEach((key, value) -> {
            if (key.equals("@timestamp") || key.equals("message") || key.equals("trace.id") || key.equals("span.id")) return;
            attribute(builder, key, value);
        });
        builder.emit();
    }
    private static void attribute(io.opentelemetry.api.logs.LogRecordBuilder builder, String key, Object value) {
        if (value instanceof Map<?, ?> nested) {
            nested.forEach((name, child) -> attribute(builder, key + "." + name, child));
        } else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
            builder.setAttribute(AttributeKey.longKey(key), ((Number) value).longValue());
        else if (value instanceof Number number) builder.setAttribute(AttributeKey.doubleKey(key), number.doubleValue());
        else if (value instanceof Boolean bool) builder.setAttribute(AttributeKey.booleanKey(key), bool);
        else if (value instanceof Collection<?> items)
            builder.setAttribute(AttributeKey.stringArrayKey(key), items.stream().map(String::valueOf).toList());
        else if (value != null) builder.setAttribute(AttributeKey.stringKey(key), value.toString());
    }
    private static Severity severity(int number) {
        return switch (number) { case 17 -> Severity.ERROR; case 13 -> Severity.WARN; case 9 -> Severity.INFO;
            case 5 -> Severity.DEBUG; default -> Severity.TRACE; };
    }
}
