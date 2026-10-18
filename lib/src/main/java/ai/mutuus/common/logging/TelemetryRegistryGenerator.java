package ai.mutuus.common.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/** 단일 필드 정본에서 canonical 매핑·필드 설명·채움률 규칙을 결정적으로 생성한다. 외부 접속은 하지 않는다. */
public final class TelemetryRegistryGenerator {
    private TelemetryRegistryGenerator() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Usage: TelemetryRegistryGenerator <output-directory>");
        generate(Path.of(args[0]));
    }

    public static void generate(Path outputDirectory) throws IOException {
        var registry = TelemetryFieldRegistry.load();
        Files.createDirectories(outputDirectory);
        Map<String, Object> properties = new LinkedHashMap<>();
        List<Map<String, Object>> rules = new ArrayList<>();
        StringBuilder docs = new StringBuilder("# 로그 필드 정본\n\n")
                .append("log-fields.v2.json에서 생성했다. canonical 이름만 출력하며 별칭은 입력 정규화/폐기 전용이다. ")
                .append("배열도 OpenSearch에서는 원소 타입으로 매핑한다. attributes는 런타임에서 제한하는 비색인 사전이다.\n\n")
                .append("| 필드 | 의미 | 타입 | 배열 | 등급 | 분모 조건 | scope | 보호 | 채우는 owner |\n")
                .append("|---|---|---|---|---|---|---|---|---|\n");
        for (var field : registry.fields().values()) {
            addMapping(properties, field.name(), field.type());
            docs.append("| ").append(escape(field.name())).append(" | ").append(escape(field.meaning()))
                    .append(" | ").append(field.type()).append(" | ").append(field.array())
                    .append(" | ").append(field.tier()).append(" | ").append(field.when())
                    .append(" | ").append(String.join(",", field.scope())).append(" | ").append(field.sensitivity())
                    .append(" | ").append(field.owner()).append(" |\n");
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("field", field.name());
            rule.put("tier", field.tier());
            rule.put("when", field.when());
            rule.put("owner", field.owner());
            rule.put("type", field.type());
            rule.put("array", field.array());
            rule.put("required", "required".equals(field.tier()));
            rule.put("scope", field.scope());
            rule.put("sensitivity", field.sensitivity());
            rule.put("denominator", denominator(field.when(), field.scope()));
            rule.put("numerator", Map.of("present_and_type_valid", field.name()));
            rule.put("synthetic_values_allowed", false);
            rules.add(rule);
        }
        docs.append("\n실제 OpenSearch 템플릿/수집기 생성은 700 gen_from_registry.py 소유다. 이 디렉터리의 매핑은 비배포 preview다.\n");
        docs.append("\n## 입력 별칭\n\n출력 매핑에는 별칭을 만들지 않는다. alias_until은 정본 출력 전환일이다. input_accept_until=null은 승인된 구 입력 수용 중이며 날짜가 정해지면 그 날짜 다음 날부터 수용하지 않는다.\n\n")
                .append("| 입력 별칭 | canonical 필드 | 처리 | 출력 전환일 | 입력 수용 종료일 |\n|---|---|---|---|---|\n");
        List<Map<String, Object>> legacyInputs = new ArrayList<>();
        for (var alias : registry.aliases().values()) {
            docs.append("| ").append(escape(alias.name())).append(" | ").append(escape(alias.canonicalName()))
                    .append(" | ").append(alias.mode()).append(" | ").append(alias.removeAfter())
                    .append(" | ").append(alias.inputAcceptUntil() == null ? "수용 중(null)" : alias.inputAcceptUntil()).append(" |\n");
            legacyInputs.add(ordered("name", alias.name(), "canonical", alias.canonicalName(), "mode", alias.mode(),
                    "alias_until", alias.removeAfter(), "input_accept_until", alias.inputAcceptUntil()));
        }
        docs.append("\n## 채움률 판정\n\nall은 생성 로그 전체, request는 golmok.scope=request, authenticated는 golmok.auth.state=authenticated를 분모로 쓴다. ")
                .append("나머지 조건은 owner의 독립 실행 manifest를 분모로 사용한다. 필드가 존재하는 문서만 세어 분모를 줄이지 않는다. ")
                .append("optional은 필수 100% 판정 대상이 아니다. 값이 없으면 가짜 값으로 채우지 않는다. ")
                .append("HTTP 헤더 정의·설정 키·열거/i18n 값은 필드 목록에 포함하지 않는다.\n");
        Map<String, Object> mappings = new LinkedHashMap<>();
        mappings.put("dynamic", "strict");
        mappings.put("properties", properties);
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("index_patterns", List.of("logs-v2-*"));
        template.put("_meta", ordered("registry_version", registry.version(), "generated_from", "common-platform/log-fields.v2.json", "purpose", "preview_only_700_owns_deployable_template"));
        template.put("template", Map.of("mappings", mappings));
        writeJson(outputDirectory.resolve("opensearch-template.preview.json"), template);
        writeJson(outputDirectory.resolve("fill-rate-rules.json"), ordered("registry_version", registry.version(), "rules", rules, "legacy_inputs", legacyInputs));
        Files.writeString(outputDirectory.resolve("fields.md"), docs.toString(), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> denominator(String when, List<String> scopes) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("scope_in", scopes);
        if ("all".equals(when)) rule.put("source", "emitter_manifest");
        else if ("request".equals(when) || "authenticated".equals(when)) {
            rule.put("source", "emitter_manifest");
            rule.put("equals", Map.of("request".equals(when) ? "golmok.scope" : "golmok.auth.state",
                    "request".equals(when) ? "request" : "authenticated"));
        } else {
            rule.put("source", "owner_manifest");
            rule.put("condition", when);
        }
        return rule;
    }

    @SuppressWarnings("unchecked")
    private static void addMapping(Map<String, Object> properties, String name, String type) {
        String[] segments = name.split("\\.");
        Map<String, Object> cursor = properties;
        for (int i = 0; i < segments.length - 1; i++) {
            Map<String, Object> parent = (Map<String, Object>) cursor.computeIfAbsent(segments[i], ignored -> {
                Map<String, Object> object = new LinkedHashMap<>();
                object.put("type", "object"); object.put("dynamic", "strict");
                object.put("properties", new LinkedHashMap<String, Object>());
                return object;
            });
            if (!"object".equals(parent.get("type")) || !(parent.get("properties") instanceof Map))
                throw new IllegalArgumentException("Registry field has scalar/object collision: " + name);
            cursor = (Map<String, Object>) parent.get("properties");
        }
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("type", type);
        if ("object".equals(type)) {
            if ("attributes".equals(name)) mapping.put("dynamic", false);
            else mapping.put("enabled", false); // 요청 헤더 등 동적 키 사전은 원문 보관만 하고 임의 매핑을 만들지 않는다.
        }
        if (cursor.putIfAbsent(segments[segments.length - 1], mapping) != null)
            throw new IllegalArgumentException("Duplicate/colliding registry mapping: " + name);
    }

    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i].toString(), pairs[i + 1]);
        return result;
    }
    private static String escape(String value) { return value.replace("|", "\\|").replace("\r", " ").replace("\n", " "); }

    private static void writeJson(Path path, Object value) throws IOException {
        try (JsonGenerator json = new JsonFactory().createGenerator(Files.newOutputStream(path))) {
            json.useDefaultPrettyPrinter(); write(json, value); json.writeRaw('\n');
        }
    }
    private static void write(JsonGenerator json, Object value) throws IOException {
        if (value instanceof Map<?, ?> map) {
            json.writeStartObject();
            for (var field : map.entrySet()) { json.writeFieldName(field.getKey().toString()); write(json, field.getValue()); }
            json.writeEndObject();
        } else if (value instanceof Iterable<?> values) {
            json.writeStartArray(); for (Object item : values) write(json, item); json.writeEndArray();
        } else if (value == null) json.writeNull();
        else if (value instanceof Boolean flag) json.writeBoolean(flag);
        else if (value instanceof Integer number) json.writeNumber(number);
        else json.writeString(value.toString());
    }
}