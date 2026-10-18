package ai.mutuus.common.logging;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Clock;
import java.util.*;

/** 단일 JSON 정본을 제한된 크기/깊이로 읽는다. 외부 참조·다형성·원격 로딩을 지원하지 않는다. */
public final class TelemetryFieldRegistry {
    public static final String RESOURCE = "common-platform/log-fields.v2.json";
    private static final Set<String> TYPES = Set.of("keyword", "text", "integer", "long", "boolean", "date_nanos", "object");
    private static final Set<String> TIERS = Set.of("required", "conditional", "optional");
    private static final Set<String> SCOPES = Set.of("request", "job", "lifecycle", "collector");
    private static final Set<String> CONDITIONS = Set.of("all", "request", "authenticated", "http", "failure", "exception", "job",
            "payload", "audit", "method", "domain_event", "message", "screen", "owner", "supplied");
    private static final Set<String> FIELD_KEYS = Set.of("name", "meaning", "type", "tier", "scope", "aliases", "alias_until",
            "input_accept_until", "sensitivity", "owner", "structuredInput", "protected", "metadata", "array", "when", "discardAliases", "mdcInput");
    private final int version;
    private final Clock clock;
    private final Map<String, Field> fields;
    private final Map<String, Alias> aliases;
    private final Set<String> structuredInputFields, protectedFields, metadataFields, discardedAliases, mdcInputFields;
    private final Map<String, String> normalizationAliases;

    public record Field(String name, String meaning, String type, String tier, String when, String owner,
                        boolean structuredInput, boolean protectedField, boolean metadata, boolean array,
                        List<String> scope, String sensitivity, List<String> aliases, String aliasUntil, boolean mdcInput, String inputAcceptUntil) {
        public Field { scope = List.copyOf(scope); aliases = List.copyOf(aliases); }
    }
    public record Alias(String name, String canonicalName, String mode, String removeAfter, String inputAcceptUntil) { }

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

    private TelemetryFieldRegistry(int version, Map<String, Field> fields, Map<String, Alias> aliases, Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.version = version;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        this.aliases = Collections.unmodifiableMap(new LinkedHashMap<>(aliases));
        LinkedHashSet<String> input = new LinkedHashSet<>(), protect = new LinkedHashSet<>(), metadata = new LinkedHashSet<>(), discard = new LinkedHashSet<>(), mdc = new LinkedHashSet<>();
        LinkedHashMap<String, String> normalize = new LinkedHashMap<>();
        fields.values().forEach(field -> {
            if (field.structuredInput()) input.add(field.name());
            if (field.protectedField()) protect.add(field.name());
            if (field.metadata()) metadata.add(field.name());
            if (field.mdcInput()) mdc.add(field.name());
        });
        aliases.values().forEach(alias -> {
            if ("normalize".equals(alias.mode())) normalize.put(alias.name(), alias.canonicalName());
            else discard.add(alias.name());
        });
        this.structuredInputFields = Collections.unmodifiableSet(input);
        this.protectedFields = Collections.unmodifiableSet(protect);
        this.metadataFields = Collections.unmodifiableSet(metadata);
        this.discardedAliases = Collections.unmodifiableSet(discard);
        // 소비자가 static final Map으로 보관해도 get/순회 때 현재 날짜를 검사한다.
        this.normalizationAliases = Collections.unmodifiableMap(new AbstractMap<>() {
            @Override public String get(Object key) {
                return key instanceof String name && legacyInputAccepted(name) ? normalize.get(name) : null;
            }
            @Override public Set<Entry<String, String>> entrySet() {
                Map<String, String> accepted = new LinkedHashMap<>();
                normalize.forEach((name, canonical) -> { if (legacyInputAccepted(name)) accepted.put(name, canonical); });
                return Collections.unmodifiableMap(accepted).entrySet();
            }
        });
        this.mdcInputFields = Collections.unmodifiableSet(mdc);
    }

    public int version() { return version; }
    public Map<String, Field> fields() { return fields; }
    public Map<String, Alias> aliases() { return aliases; }
    public Set<String> structuredInputFields() { return structuredInputFields; }
    public Set<String> protectedFields() { return protectedFields; }
    public Set<String> metadataFields() { return metadataFields; }
    public Set<String> discardedAliases() { return discardedAliases; }
    public Set<String> mdcInputFields() { return mdcInputFields; }
    public Map<String, String> normalizationAliases() { return normalizationAliases; }

    public static TelemetryFieldRegistry from(InputStream input) { return from(input, Clock.systemUTC()); }
    public static TelemetryFieldRegistry from(InputStream input, Clock clock) {
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
            LinkedHashMap<String, Alias> aliases = new LinkedHashMap<>();
            for (Object item : (List<?>) root.get("fields")) {
                Map<String, Object> source = object(item);
                keys(source, FIELD_KEYS);
                String name = text(source, "name");
                require(name.matches("@timestamp|[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)*"), "Invalid canonical field name");
                List<String> scope = strings(source, "scope", false), names = strings(source, "aliases", true);
                String until = text(source, "alias_until");
                require(LocalDate.parse(until).toString().equals(until), "Invalid alias output cutover date");
                require(source.containsKey("input_accept_until"), "Missing input_accept_until");
                String inputUntil = source.get("input_accept_until") == null ? null : text(source, "input_accept_until");
                if (inputUntil != null) {
                    LocalDate end = LocalDate.parse(inputUntil);
                    require(end.toString().equals(inputUntil) && !end.isBefore(LocalDate.parse(until)), "Invalid legacy input acceptance date");
                }
                String when = source.containsKey("when") ? text(source, "when") : "supplied";
                Field field = new Field(name, text(source, "meaning"), text(source, "type"), text(source, "tier"), when,
                        text(source, "owner"), bool(source, "structuredInput"), bool(source, "protected"), bool(source, "metadata"),
                        bool(source, "array"), scope, text(source, "sensitivity"), names, until, bool(source, "mdcInput"), inputUntil);
                require(TYPES.contains(field.type()) && TIERS.contains(field.tier()) && CONDITIONS.contains(field.when())
                        && Set.of("common", "adapter", "collector").contains(field.owner()) && SCOPES.containsAll(scope)
                        && Set.of("none", "mask", "opt_in").contains(field.sensitivity()), "Invalid telemetry field classification");
                require(!field.metadata() || (field.structuredInput() && field.protectedField()), "Metadata must be protected structured input");
                require(!field.mdcInput() || ("keyword".equals(field.type()) && !field.protectedField()),
                        "MDC input requires an unprotected technical keyword field");
                require(fields.putIfAbsent(name, field) == null && fields.size() <= 512, "Duplicate or excess telemetry fields");
                List<String> discard = source.containsKey("discardAliases") ? strings(source, "discardAliases", true) : List.of();
                require(names.containsAll(discard), "discardAliases must be a subset of aliases");
                for (String alias : names) addAlias(aliases, alias, name, discard.contains(alias) ? "discard" : "normalize", until, inputUntil);
            }
            require(!fields.isEmpty(), "Empty telemetry registry");
            for (String name : fields.keySet()) {
                require(!aliases.containsKey(name), "Canonical field cannot be an alias");
                for (String other : fields.keySet()) require(!other.startsWith(name + "."), "Canonical scalar/object prefix collision");
            }
            return new TelemetryFieldRegistry((Integer) root.get("version"), fields, aliases, clock);
        } catch (IllegalArgumentException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid or unsafe telemetry field registry", ex); }
    }
    private static void addAlias(Map<String, Alias> aliases, String name, String target, String mode, String until, String inputUntil) {
        require(name.matches("@timestamp|[A-Za-z_][A-Za-z0-9_.-]{0,127}"), "Invalid alias name");
        require(aliases.putIfAbsent(name, new Alias(name, target, mode, until, inputUntil)) == null && aliases.size() <= 1024,
                "Duplicate or excess aliases");
    }

    /** null은 승인된 legacy 입력 수용 중이다. 종료일 당일까지 허용하고 다음 UTC 날짜부터 거부한다. */
    public boolean legacyInputAccepted(String name) {
        Alias alias = aliases.get(name);
        return alias != null && (alias.inputAcceptUntil() == null
                || !LocalDate.now(clock).isAfter(LocalDate.parse(alias.inputAcceptUntil())));
    }
    /** 빌드/adapter 경계에서 만료 입력의 수용을 명시 검증한다. */
    public void requireLegacyInputAccepted(String name) {
        require(legacyInputAccepted(name), "Expired or unknown legacy telemetry input: " + name);
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
            if ("attributes".equals(name)) { validateMarkers(value); return; }
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
    private static void validateMarkers(Object value) {
        require(value instanceof Map<?, ?>, "Unknown MDC attributes must be an object");
        Map<?, ?> markers = (Map<?, ?>) value;
        require(markers.size() <= 16, "Unknown MDC marker limit exceeded");
        markers.forEach((name, marker) -> require(name instanceof String text && text.matches("[A-Za-z0-9_.-]{1,64}")
                && "REDACTED_UNREVIEWED".equals(marker), "Invalid unknown MDC marker"));
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
