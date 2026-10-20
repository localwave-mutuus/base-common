package ai.mutuus.common.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Log-only UI contract; invalid hints are omitted without rejecting the request. */
public final class ScreenMetadata {
    private ScreenMetadata() {}
    public static final Map<String, String> FIELDS = Map.of(
            HeaderNames.SCREEN_ID, "golmok.screen.id",
            HeaderNames.SCREEN_CATALOG_VERSION, "golmok.screen.catalog_version",
            HeaderNames.BUSINESS_SCREEN_NO, "golmok.screen.business_no",
            HeaderNames.UI_ELEMENT_ID, "golmok.ui.element.id",
            HeaderNames.UI_ACTION, "golmok.ui.action",
            HeaderNames.EVENT_ID, "golmok.ui.event.id");

    public static Map<String, String> fromHeaders(Function<String, String> reader) {
        return fromHeaders(System.getProperty("mutuus.ui.system"), reader);
    }

    public static Map<String, String> fromHeaders(String system, Function<String, String> reader) {
        Map<String, String> values = new LinkedHashMap<>();
        String version = reader.apply(HeaderNames.SCREEN_CATALOG_VERSION);
        String screen = UiCodeResolver.screen(system, reader.apply(HeaderNames.SCREEN_ID), version).orElse(null);
        if (screen == null) return Map.of();
        values.put(HeaderNames.SCREEN_ID, screen);
        values.put(HeaderNames.SCREEN_CATALOG_VERSION, version);
        String business = reader.apply(HeaderNames.BUSINESS_SCREEN_NO);
        if (business != null && business.matches("[A-Z]{3}-[0-9]{3}"))
            values.put(HeaderNames.BUSINESS_SCREEN_NO, business);
        String element = reader.apply(HeaderNames.UI_ELEMENT_ID);
        if (element != null && element.length() <= 64 && element.startsWith(screen + "-")
                && element.substring(screen.length() + 1).matches("[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*"))
            values.put(HeaderNames.UI_ELEMENT_ID, element);
        String action = reader.apply(HeaderNames.UI_ACTION);
        if (action != null && Set.of("view", "press", "submit", "change", "error").contains(action))
            values.put(HeaderNames.UI_ACTION, action);
        String event = reader.apply(HeaderNames.EVENT_ID);
        if (event != null && event.matches("[0-9a-f]{16}") && !event.equals("0000000000000000"))
            values.put(HeaderNames.EVENT_ID, event);
        return Map.copyOf(values);
    }
}
