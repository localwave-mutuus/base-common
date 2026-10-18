package ai.mutuus.common.logging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** 배포자가 설정한 OTel Resource의 허용 필드만 읽는다. 업무 코드로 서비스명을 추정하지 않는다. */
final class CommonLogResource {
    private static final Set<String> KEYS = Set.of("service.name", "service.instance.id", "service.version",
            "deployment.environment.name");
    private CommonLogResource() { }

    static Map<String, Object> current() { return fields(System::getProperty, System::getenv); }

    static Map<String, Object> fields(Function<String, String> properties, Function<String, String> environment) {
        Map<String, Object> fields = new LinkedHashMap<>();
        parse(environment.apply("OTEL_RESOURCE_ATTRIBUTES"), fields);
        parse(properties.apply("otel.resource.attributes"), fields);
        put(fields, "service.name", first(properties.apply("otel.service.name"), environment.apply("OTEL_SERVICE_NAME"),
                text(fields.get("service.name")), properties.apply("SERVICE_NAME"), "unknown-service"));
        put(fields, "service.instance.id", first(properties.apply("otel.service.instance.id"), text(fields.get("service.instance.id"))));
        put(fields, "service.version", first(properties.apply("otel.service.version"), text(fields.get("service.version"))));
        String deployment = first(text(fields.get("deployment.environment.name")), properties.apply("mutuus.environment"), "local");
        fields.put("deployment.environment.name", switch (deployment) {
            case "dev" -> "development"; case "stage" -> "staging"; case "prod" -> "production"; default -> deployment;
        });
        return Map.copyOf(fields);
    }
    private static void parse(String attributes, Map<String, Object> fields) {
        if (attributes == null || attributes.length() > 16384) return;
        for (String entry : attributes.split(",")) {
            int equal = entry.indexOf('=');
            if (equal < 1) continue;
            String key = entry.substring(0, equal).trim();
            if (KEYS.contains(key)) put(fields, key, entry.substring(equal + 1).trim());
        }
    }
    private static String text(Object value) { return value instanceof String text ? text : null; }
    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank() && value.length() <= 512) return value;
        return null;
    }
    private static void put(Map<String, Object> fields, String key, String value) {
        if (value != null && !value.isBlank() && value.length() <= 512) fields.put(key, value);
    }
}
