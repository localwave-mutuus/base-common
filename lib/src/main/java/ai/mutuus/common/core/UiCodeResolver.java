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
        return screen(System.getProperty("mutuus.ui.system"), value, version);
    }
    public static Optional<String> screen(String system, String value, String version) {
        if (!java.util.Set.of("APP","MEM","BO").contains(system == null ? "" : system) || value == null) return Optional.empty();
        if (value.matches(system+"-[A-Z]{3}-[0-9]{3}") && UiCodeAliases.ALIASES.stream().anyMatch(a ->
                a.system().equals(system) && a.version().equals(version) && a.status().equals("RESOLVED")
                        && value.equals(a.target()))) return Optional.of(value);
        String kind = value.startsWith("/") ? "ROUTE" : value.matches("[A-Z]-[0-9]{2}") ? "BO_ID" : "INTERNAL_ID";
        return resolve(system, kind, value, version)
                .or(() -> safeIdentifier(value) ? Optional.of(value) : Optional.empty());
    }
    static boolean safeIdentifier(String value) {
        return value != null && value.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");
    }
}
