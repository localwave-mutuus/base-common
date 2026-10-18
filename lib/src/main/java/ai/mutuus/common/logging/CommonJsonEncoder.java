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
    @Override public byte[] encode(ILoggingEvent event) {
        try {
            Map<String, Object> nested = new LinkedHashMap<>();
            CanonicalLogSnapshot.snapshot(event).forEach((key, value) -> nest(nested, key, value));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (JsonGenerator json = JSON.createGenerator(out)) { write(json, nested); json.writeRaw('\n'); }
            return out.toByteArray();
        } catch (IOException ex) { throw new IllegalStateException("Cannot encode schema v2 log", ex); }
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
