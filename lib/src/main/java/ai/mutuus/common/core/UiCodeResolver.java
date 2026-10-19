package ai.mutuus.common.core;

import ai.mutuus.common.uicatalog.UiCodeAliases;
import java.util.Optional;

/** Exact, versioned alias boundary. Never normalizes URLs or guesses ambiguous targets. */
public final class UiCodeResolver {
    private UiCodeResolver() {}

    public static Optional<String> resolve(String system, String kind, String value, String version) {
        if (system == null || kind == null || value == null || version == null || value.length() > 128)
            return Optional.empty();
        return UiCodeAliases.resolve(system, kind, value, version);
    }

    public static Optional<String> screen(String value, String version) {
        if (value == null || !UiCodeAliases.CATALOG_VERSION.equals(version)) return Optional.empty();
        if (value.matches("BO-[A-Z]{3}-[0-9]{3}") && UiCodeAliases.ALIASES.stream().anyMatch(a ->
                a.system().equals("BO") && a.version().equals(version) && a.status().equals("RESOLVED")
                        && value.equals(a.target()))) return Optional.of(value);
        String kind = value.startsWith("/") ? "ROUTE" : value.matches("[A-Z]-[0-9]{2}") ? "BO_ID" : "INTERNAL_ID";
        return resolve("BO", kind, value, version);
    }
}
