package ai.mutuus.common.logging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** 정본 일치·새 형식 전용 출력·독립 분모를 한 번의 생성 계약으로 검증한다. */
class OtelRegistryGenerationContractTest {
    @TempDir Path temp;

    @Test
    @SuppressWarnings("unchecked")
    void registryAloneDefinesStrictMappingsDocsAndConditionalFillRateRules() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        TelemetryRegistryGenerator.generate(first);
        TelemetryRegistryGenerator.main(new String[]{second.toString()});
        for (String artifact : List.of("opensearch-template.preview.json", "fields.md", "fill-rate-rules.json")) {
            assertThat(Files.readAllBytes(first.resolve(artifact))).isEqualTo(Files.readAllBytes(second.resolve(artifact)));
        }
        var registry = TelemetryFieldRegistry.load();
        Map<String, Object> template = object(first.resolve("opensearch-template.preview.json"));
        Map<String, Object> mappings = (Map<String, Object>) ((Map<?, ?>) template.get("template")).get("mappings");
        assertThat(mappings).containsEntry("dynamic", "strict");
        Map<String, Object> properties = (Map<String, Object>) mappings.get("properties");
        Map<String, Object> rates = object(first.resolve("fill-rate-rules.json"));
        List<Map<String, Object>> rules = (List<Map<String, Object>>) rates.get("rules");
        assertThat(rules).hasSize(registry.fields().size());
        assertThat(rates).containsEntry("registry_version", registry.version());
        assertThat(registry.fields().values().stream().filter(field -> "required".equals(field.tier())).map(TelemetryFieldRegistry.Field::name).toList())
                .containsExactlyInAnyOrder("@timestamp", "event.id", "trace.id", "span.id", "service.name",
                        "service.instance.id", "deployment.environment.name", "log.level", "message", "golmok.scope",
                        "golmok.schema.version", "user.id", "golmok.identity.realm", "error.type", "golmok.request.id");
        String docs = Files.readString(first.resolve("fields.md"));
        for (var field : registry.fields().values()) {
            Map<String, Object> mapping = mapping(properties, field.name());
            assertThat(mapping).as(field.name()).containsEntry("type", field.type());
            assertThat(docs).contains("| " + field.name() + " | ");
            var rule = rules.stream().filter(item -> field.name().equals(item.get("field"))).findFirst().orElseThrow();
            assertThat(rule).containsEntry("tier", field.tier()).containsEntry("when", field.when())
                    .containsEntry("owner", field.owner()).containsEntry("type", field.type())
                    .containsEntry("array", field.array()).containsEntry("synthetic_values_allowed", false)
                    .containsEntry("required", "required".equals(field.tier()))
                    .containsEntry("scope", field.scope()).containsEntry("sensitivity", field.sensitivity());
            Map<String, Object> denominator = (Map<String, Object>) rule.get("denominator");
            assertThat(denominator).doesNotContainKeys("exists", "field_present");
            if ("all".equals(field.when())) assertThat(denominator).containsEntry("source", "emitter_manifest");
            else if (!List.of("request", "authenticated").contains(field.when()))
                assertThat(denominator).containsEntry("source", "owner_manifest").containsEntry("condition", field.when());
        }
        assertThat(rates).doesNotContainKeys("legacy_inputs", "aliases", "alias_until", "input_accept_until");
        assertThat(docs).doesNotContain("alias_until", "input_accept_until", "입력 별칭");
        assertThat(registry.version()).isEqualTo(5);
        assertThat(registry.fields()).hasSize(97);
        for (String field : List.of("trace.id", "span.id", "user.id")) {
            var rule = rules.stream().filter(item -> field.equals(item.get("field"))).findFirst().orElseThrow();
            boolean user = "user.id".equals(field);
            assertThat(rule).containsEntry("required", true).containsEntry("when", user ? "authenticated" : "request");
            Map<String, Object> denominator = (Map<String, Object>) rule.get("denominator");
            assertThat(denominator).containsEntry("source", "emitter_manifest")
                    .containsEntry("equals", Map.of(user ? "golmok.auth.state" : "golmok.scope", user ? "authenticated" : "request"));
        }
        assertThat(registry.fields().keySet()).allMatch(name -> !name.startsWith("X-")
                && !name.startsWith("mutuus.common.") && !name.startsWith("spring.") && !name.startsWith("management."));
        assertThat(mapping(properties, "attributes")).containsEntry("type", "object").containsEntry("dynamic", false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapping(Map<String, Object> properties, String field) {
        String[] segments = field.split("\\.");
        Map<String, Object> cursor = properties;
        for (int i = 0; i < segments.length; i++) {
            Object value = cursor.get(segments[i]);
            if (!(value instanceof Map<?, ?>)) return null;
            Map<String, Object> found = (Map<String, Object>) value;
            if (i == segments.length - 1) return found;
            if (!(found.get("properties") instanceof Map<?, ?>)) return null;
            cursor = (Map<String, Object>) found.get("properties");
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Path path) throws Exception {
        try (JsonParser parser = new JsonFactory().createParser(path.toFile())) {
            assertThat(parser.nextToken()).isEqualTo(JsonToken.START_OBJECT);
            return (Map<String, Object>) read(parser);
        }
    }
    private static Object read(JsonParser parser) throws Exception {
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            Map<String, Object> result = new LinkedHashMap<>();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String name = parser.currentName(); parser.nextToken(); result.put(name, read(parser));
            }
            return result;
        }
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            List<Object> result = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) result.add(read(parser));
            return result;
        }
        if (parser.currentToken() == JsonToken.VALUE_TRUE) return true;
        if (parser.currentToken() == JsonToken.VALUE_FALSE) return false;
        if (parser.currentToken().isNumeric()) return parser.getIntValue();
        return parser.getValueAsString();
    }
}