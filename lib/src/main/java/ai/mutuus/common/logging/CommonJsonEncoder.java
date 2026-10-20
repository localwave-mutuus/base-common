package ai.mutuus.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/** schema v2의 단일 JSON 인코더. 점 표기 키를 JSON 객체로 한 번만 변환한다. */
public final class CommonJsonEncoder extends EncoderBase<ILoggingEvent> {
    private static final JsonFactory JSON = new JsonFactory();
    @Override public byte[] headerBytes() { return new byte[0]; }
    @Override public byte[] footerBytes() { return new byte[0]; }
    private static volatile int configuredMaxEventBytes=32768;
    private int maxEventBytes = 32768;
    public static void configureSafeLimit(int value){if(value<2048 || value>32768)throw new IllegalArgumentException("Invalid event byte limit");configuredMaxEventBytes=value;}
    public void setMaxEventBytes(int value) {
        if (value < 2048 || value > 32768) throw new IllegalArgumentException("Invalid event byte limit");
        maxEventBytes = value;
    }
    @Override public byte[] encode(ILoggingEvent event) {
        return encodeSnapshot(CanonicalLogSnapshot.snapshot(event));
    }
    byte[] encodeSnapshot(Map<String,Object> snapshot) {
        try {
            int maxEventBytes=Math.min(this.maxEventBytes,configuredMaxEventBytes);
            var flat = new LinkedHashMap<String,Object>(snapshot);
            byte[] result = serialize(flat);
            if (result.length <= maxEventBytes) return result;
            flat.keySet().removeIf(key -> key.contains(".body.") || key.equals("mutuus.method.args") || key.equals("mutuus.method.return"));
            flat.put("golmok.payload.omitted.reason", "EVENT_TOO_LARGE");
            flat.put("golmok.payload.truncated", true);
            result = serialize(flat);
            if (result.length <= maxEventBytes) return result;
            Set<String> required = Set.of("@timestamp", "event.id", "golmok.schema.version", "message", "log.level", "log.severity_number", "log.logger",
                "service.name", "service.version", "service.instance.id", "error.code", "error.type", "user.id", "golmok.identity.realm", "golmok.identity.source", "service.environment", "deployment.environment", "deployment.environment.name", "host.name", "process.pid",
                "trace.id", "span.id", "parent.id", "golmok.scope", "golmok.auth.state", "event.action", "event.dataset", "event.outcome", "event.duration",
                "golmok.request.id", "golmok.service.invocation.id", "execution.id", "golmok.payload.omitted.reason", "golmok.payload.truncated");
            flat.keySet().retainAll(required);
            flat.replaceAll((key,value) -> {
                if (!(value instanceof String text)) return value;
                int bound = Set.of("event.id","golmok.request.id","golmok.service.invocation.id","execution.id").contains(key)?36:
                    key.equals("trace.id")?32:key.equals("@timestamp")?30:24;
                return bounded(text, bound);
            });
            result = serialize(flat);
            if (result.length > maxEventBytes) throw new IllegalStateException("Required envelope exceeds byte limit");
            return result;
        } catch (IOException ex) { throw new IllegalStateException("Cannot encode schema v2 log", ex); }
    }
    private static String bounded(String value, int bytes) {
        int end=0, size=0;
        while(end<value.length()) { int cp=value.codePointAt(end); int n=cp<32?6:cp==34 || cp==92?2:cp<=127?1:cp<=2047?2:cp<=65535?3:4;
            if(size+n>bytes)break; size+=n; end+=Character.charCount(cp); }
        return value.substring(0,end);
    }
    private static byte[] serialize(Map<String,Object> flat) throws IOException {
        Map<String,Object> nested = new LinkedHashMap<>();
        flat.forEach((key,value) -> nest(nested,key,value));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(JsonGenerator json=JSON.createGenerator(out)){write(json,nested);json.writeRaw('\n');}
        return out.toByteArray();
    }
    @SuppressWarnings("unchecked")
    private static void nest(Map<String, Object> root, String key, Object value) {
        String[] segments = key.split("\\.");
        Map<String, Object> cursor = root;
        for (int i = 0; i < segments.length - 1; i++)
            cursor = (Map<String, Object>) cursor.computeIfAbsent(segments[i], ignored -> new LinkedHashMap<>());
        cursor.put(segments[segments.length - 1], value);
    }
    private static void write(JsonGenerator json, Object value) throws IOException {
        if (value instanceof Map<?, ?> map) {
            json.writeStartObject();
            for (var item : map.entrySet()) { json.writeFieldName(item.getKey().toString()); write(json, item.getValue()); }
            json.writeEndObject();
        } else if (value instanceof Collection<?> items) {
            json.writeStartArray(); for (Object item : items) write(json, item); json.writeEndArray();
        } else if (value instanceof Integer number) json.writeNumber(number);
        else if (value instanceof Long number) json.writeNumber(number);
        else if (value instanceof Number number) json.writeNumber(number.toString());
        else if (value instanceof Boolean bool) json.writeBoolean(bool);
        else if (value == null) json.writeNull();
        else json.writeString(value.toString());
    }
}
