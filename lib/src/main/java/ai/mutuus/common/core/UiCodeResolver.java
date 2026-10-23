package ai.mutuus.common.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Program-owned aliases; absent or invalid configuration never rejects requests. */
public final class UiCodeResolver {
    private UiCodeResolver() {}
    private static final String RESOURCE = "META-INF/ui-catalog/aliases.json";
    private record Key(String system, String kind, String value) {}
    private static final Map<ClassLoader, Map<Key, String>> CACHE = new WeakHashMap<>();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static Optional<String> resolve(String system, String kind, String value, String version) {
        if (!Set.of("APP", "MEM", "BO").contains(system == null ? "" : system)
                || kind == null || value == null || value.length() > 128) return Optional.empty();
        String target = aliases().get(new Key(system, kind, value));
        return Optional.ofNullable(target != null ? target : safeIdentifier(value) ? value : null);
    }

    private static synchronized Map<Key, String> aliases() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = UiCodeResolver.class.getClassLoader();
        return CACHE.computeIfAbsent(loader, UiCodeResolver::load);
    }

    private static Map<Key, String> load(ClassLoader loader) {
        Map<Key, String> result = new HashMap<>();
        try {
            var resources = loader.getResources(RESOURCE);
            while (resources.hasMoreElements()) {
                try (var input = resources.nextElement().openStream(); var parser = new JsonFactory().createParser(input)) {
                    if (parser.nextToken() != JsonToken.START_ARRAY) throw new IllegalArgumentException();
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() != JsonToken.START_OBJECT) throw new IllegalArgumentException();
                        Map<String, String> row = new HashMap<>();
                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                            if (parser.currentToken() != JsonToken.FIELD_NAME) throw new IllegalArgumentException();
                            String field = parser.currentName();
                            if (parser.nextToken() != JsonToken.VALUE_STRING || row.putIfAbsent(field, parser.getText()) != null)
                                throw new IllegalArgumentException();
                        }
                        String system = row.get("system"), kind = row.get("kind"), value = row.get("value"), target = row.get("target");
                        if (!Set.of("APP", "MEM", "BO").contains(system == null ? "" : system)
                                || !safeIdentifier(kind) || value == null || value.isBlank() || value.length() > 128
                                || !safeIdentifier(target)) throw new IllegalArgumentException();
                        Key key = new Key(system, kind, value);
                        String previous = result.putIfAbsent(key, target);
                        if (previous != null && !previous.equals(target)) throw new IllegalArgumentException();
                    }
                    if (parser.nextToken() != null) throw new IllegalArgumentException();
                }
            }
            return Map.copyOf(result);
        } catch (Exception invalid) {
            if (WARNED.compareAndSet(false, true))
                System.getLogger(UiCodeResolver.class.getName()).log(System.Logger.Level.WARNING,
                        "Invalid program UI aliases; using original identifiers (further warnings suppressed)");
            return Map.of();
        }
    }

    public static Optional<String> screen(String value, String version) {
        return screen(System.getProperty("mutuus.ui.system"), value, version);
    }
    public static Optional<String> screen(String system, String value, String version) {
        if (!Set.of("APP", "MEM", "BO").contains(system == null ? "" : system) || value == null) return Optional.empty();
        String kind = value.startsWith("/") ? "ROUTE" : value.matches("[A-Z]-[0-9]{2}") ? "BO_ID" : "INTERNAL_ID";
        String target = aliases().get(new Key(system, kind, value));
        if (target == null && "APP".equals(system)) target = aliases().get(new Key(system, "LEGACY", value));
        return Optional.ofNullable(target != null ? target : safeIdentifier(value) ? value : null);
    }
    static boolean safeIdentifier(String value) {
        return value != null && value.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");
    }
}
