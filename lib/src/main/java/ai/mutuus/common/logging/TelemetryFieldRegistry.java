package ai.mutuus.common.logging;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

/** 단일 JSON 정본을 제한된 크기/깊이로 읽는다. 외부 참조·다형성·원격 로딩을 지원하지 않는다. */
public final class TelemetryFieldRegistry {
    public static final String RESOURCE = "common-platform/log-fields.v2.json";
    private static final Set<String> TYPES = Set.of("keyword", "text", "integer", "long", "boolean", "date_nanos", "object");
    private static final Set<String> TIERS = Set.of("required", "conditional", "optional");
    private static final Set<String> SCOPES = Set.of("request", "job", "lifecycle", "collector");
    private static final Set<String> CONDITIONS = Set.of("all", "request", "authenticated", "http", "failure", "exception", "job",
            "payload", "audit", "method", "domain_event", "message", "screen", "owner", "supplied");
    private static final Set<String> FIELD_KEYS = Set.of("name", "meaning", "type", "tier", "scope",
            "sensitivity", "owner", "structuredInput", "protected", "metadata", "array", "when", "mdcInput");
    private final int version;
    private final Map<String, Field> fields;
    private final Set<String> structuredInputFields, protectedFields, metadataFields, mdcInputFields;

    public record Field(String name, String meaning, String type, String tier, String when, String owner,
                        boolean structuredInput, boolean protectedField, boolean metadata, boolean array,
                        List<String> scope, String sensitivity, boolean mdcInput) {
        public Field { scope = List.copyOf(scope); }
    }
    private static final class DefaultRegistry {
        static final TelemetryFieldRegistry INSTANCE = readDefault();
        private static TelemetryFieldRegistry readDefault() {
            try (InputStream input = TelemetryFieldRegistry.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                if (input == null) throw new IllegalStateException("Missing telemetry field registry resource");
                return from(input);
            } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot read telemetry registry", ex); }
        }
    }
    public static TelemetryFieldRegistry load() { return DefaultRegistry.INSTANCE; }

    private TelemetryFieldRegistry(int version, Map<String, Field> fields) {
        this.version = version;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        LinkedHashSet<String> input = new LinkedHashSet<>(), protect = new LinkedHashSet<>(), metadata = new LinkedHashSet<>(), mdc = new LinkedHashSet<>();
        fields.values().forEach(field -> {
            if (field.structuredInput()) input.add(field.name());
            if (field.protectedField()) protect.add(field.name());
            if (field.metadata()) metadata.add(field.name());
            if (field.mdcInput()) mdc.add(field.name());
        });
        this.structuredInputFields = Collections.unmodifiableSet(input);
        this.protectedFields = Collections.unmodifiableSet(protect);
        this.metadataFields = Collections.unmodifiableSet(metadata);
        this.mdcInputFields = Collections.unmodifiableSet(mdc);
    }

    public int version() { return version; }
    public Map<String, Field> fields() { return fields; }
    public Set<String> structuredInputFields() { return structuredInputFields; }
    public Set<String> protectedFields() { return protectedFields; }
    public Set<String> metadataFields() { return metadataFields; }
    public Set<String> mdcInputFields() { return mdcInputFields; }

    public static TelemetryFieldRegistry from(InputStream input) {
        Objects.requireNonNull(input, "registry input");
        try {
            byte[] data = input.readNBytes(1_048_577);
            require(data.length <= 1_048_576, "Telemetry registry exceeds size limit");
            JsonFactory json = JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(16).maxStringLength(4096).maxNumberLength(16).build()).build();
            Map<String, Object> root;
            try (JsonParser parser = json.createParser(data)) {
                require(parser.nextToken() == JsonToken.START_OBJECT, "Expected JSON registry object");
                root = object(read(parser));
                require(parser.nextToken() == null, "Trailing registry content");
            }
            keys(root, Set.of("version", "fields"));
            require(root.get("version") instanceof Integer number && number > 0, "Invalid registry version");
            require(root.get("fields") instanceof List<?>, "Missing registry fields");
            LinkedHashMap<String, Field> fields = new LinkedHashMap<>();

            for (Object item : (List<?>) root.get("fields")) {
                Map<String, Object> source = object(item);
                keys(source, FIELD_KEYS);
                String name = text(source, "name");
                require(name.matches("@timestamp|[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)*"), "Invalid canonical field name");
                List<String> scope = strings(source, "scope", false);
                String when = source.containsKey("when") ? text(source, "when") : "supplied";
                Field field = new Field(name, text(source, "meaning"), text(source, "type"), text(source, "tier"), when,
                        text(source, "owner"), bool(source, "structuredInput"), bool(source, "protected"), bool(source, "metadata"),
                        bool(source, "array"), scope, text(source, "sensitivity"), bool(source, "mdcInput"));
                require(TYPES.contains(field.type()) && TIERS.contains(field.tier()) && CONDITIONS.contains(field.when())
                        && Set.of("common", "adapter", "collector").contains(field.owner()) && SCOPES.containsAll(scope)
                        && Set.of("none", "mask", "opt_in").contains(field.sensitivity()), "Invalid telemetry field classification");
                require(!field.metadata() || (field.structuredInput() && field.protectedField()), "Metadata must be protected structured input");
                require(!field.mdcInput() || ("keyword".equals(field.type()) && !field.protectedField()),
                        "MDC input requires an unprotected technical keyword field");
                require(fields.putIfAbsent(name, field) == null && fields.size() <= 512, "Duplicate or excess telemetry fields");
            }
            require(!fields.isEmpty(), "Empty telemetry registry");
            for (String name : fields.keySet()) {

                for (String other : fields.keySet()) require(!other.startsWith(name + "."), "Canonical scalar/object prefix collision");
            }
            if ((Integer)root.get("version") == 5) {
                for (String name : List.of("golmok.service.invocation.id", "golmok.payload.policy.id", "golmok.payload.omitted.reason", "golmok.payload.truncated")) {
                    Field field=fields.get(name);
                    require(field!=null && field.protectedField() && !field.mdcInput() && !field.structuredInput(), "v5 typed protection required");
                    require(field.type().equals(name.endsWith("truncated") ? "boolean" : "keyword"), "v5 field type mismatch");
                }
            }
            return new TelemetryFieldRegistry((Integer) root.get("version"), fields);
        } catch (IllegalArgumentException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid or unsafe telemetry field registry", ex); }
    }
    /** 발생 경로의 이름 관문. 타입·충족률 검증은 빌드 시험/생성 규칙으로 수행한다. */
    public void validateNames(Map<String, ?> snapshot) {
        for (String name : snapshot.keySet()) require(fields.containsKey(name), "Unregistered canonical telemetry output: " + name);
    }
    /** 빌드 관문에서 실제 emitter snapshot의 이름·JSON 타입을 대조한다. 빈 값을 만들어 채우지 않는다. */
    public void validateSnapshot(Map<String, ?> snapshot) {
        validateNames(snapshot);
        snapshot.forEach((name, value) -> {
            Field field = fields.get(name);
            if (name.equals("golmok.payload.omitted.reason")) require(ai.mutuus.common.payload.SafePayloadPolicy.REASONS.contains(value), "Invalid payload omission reason");
            if (name.equals("golmok.service.invocation.id")) require(value instanceof String id && java.util.UUID.fromString(id).toString().equals(id), "Invalid invocation ID");
            if ("attributes".equals(name)) { validateReservedAttributes(value); return; }
            if (field.array()) {
                require(value instanceof Collection<?>, "Telemetry field must be an array: " + name);
                for (Object item : (Collection<?>) value) validateType(field, item);
            } else validateType(field, value);
        });
    }
    private static void validateType(Field field, Object value) {
        boolean valid = switch (field.type()) {
            case "keyword", "text" -> value instanceof String;
            case "boolean" -> value instanceof Boolean;
            case "integer" -> integral(value, 31);
            case "long" -> integral(value, 63);
            case "date_nanos" -> isInstant(value);
            case "object" -> value instanceof Map<?, ?>;
            default -> false;
        };
        require(valid, "Telemetry JSON type mismatch: " + field.name() + " expected " + field.type());
    }
    private static boolean integral(Object value, int bits) {
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof BigInteger)) return false;
        return new BigInteger(value.toString()).bitLength() <= bits;
    }
    private static boolean isInstant(Object value) {
        if (!(value instanceof String text)) return false;
        try { return Instant.parse(text).toString().endsWith("Z"); } catch (java.time.DateTimeException ex) { return false; }
    }
    private static void validateReservedAttributes(Object value) {
        require(value instanceof Map<?, ?> map && map.isEmpty(), "Unreviewed telemetry attributes are not accepted");
    }
    private static Object read(JsonParser parser) throws java.io.IOException {
        return switch (parser.currentToken()) {
            case START_OBJECT -> {
                Map<String, Object> object = new LinkedHashMap<>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    require(parser.currentToken() == JsonToken.FIELD_NAME, "Invalid registry object");
                    String key = parser.currentName(); parser.nextToken(); object.put(key, read(parser));
                }
                yield object;
            }
            case START_ARRAY -> {
                List<Object> values = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) values.add(read(parser));
                yield values;
            }
            case VALUE_STRING -> parser.getText();
            case VALUE_NUMBER_INT -> parser.getIntValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            default -> throw new IllegalArgumentException("Unsupported registry JSON token");
        };
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        require(value instanceof Map<?, ?>, "Expected registry object");
        return (Map<String, Object>) value;
    }
    private static void keys(Map<String, ?> source, Set<String> accepted) {
        require(accepted.containsAll(source.keySet()), "Unknown registry property");
    }
    private static String text(Map<String, Object> source, String name) {
        require(source.get(name) instanceof String value && !value.isBlank() && value.length() <= 2048, "Missing or excess registry text: " + name);
        return (String) source.get(name);
    }
    private static boolean bool(Map<String, Object> source, String name) {
        if (!source.containsKey(name)) return false;
        require(source.get(name) instanceof Boolean, "Invalid registry boolean");
        return (Boolean) source.get(name);
    }
    private static List<String> strings(Map<String, Object> source, String name, boolean empty) {
        require(source.get(name) instanceof List<?>, "Missing registry array: " + name);
        List<?> raw = (List<?>) source.get(name);
        require((empty || !raw.isEmpty()) && raw.size() <= 128, "Invalid registry array size");
        List<String> values = new ArrayList<>();
        for (Object value : raw) { require(value instanceof String && !((String) value).isBlank(), "Invalid registry array entry"); values.add((String) value); }
        require(new HashSet<>(values).size() == values.size(), "Duplicate registry array entry");
        return List.copyOf(values);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
