package ai.mutuus.common.core;

import ai.mutuus.common.uicatalog.UiCodeAliases;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class UiCodeResolverTest {
    private static final String VERSION = UiCodeAliases.CATALOG_VERSION;
    @Test void runtimeCopyIsByteIdenticalToBo8() throws Exception {
        Path source = Path.of("../../generated/ui-catalog/UiCodeAliases.java");
        Path runtime = Path.of("../../lib/src/main/java/ai/mutuus/common/uicatalog/UiCodeAliases.java");
        assertThat(Files.readAllBytes(runtime)).isEqualTo(Files.readAllBytes(source));
        assertThat(VERSION).isEqualTo("1.0.0-bo.8");
        assertThat(UiCodeAliases.CATALOG_DIGEST).isEqualTo("5a86338c011c0c2baeb482752354dcfaf3008de7a28128dd170594fb28c3ea1e");
    }
    @Test void exactKeysAndResolvedStatusAreRequired() {
        assertThat(UiCodeResolver.resolve("BO", "BO_ID", "I-01", VERSION)).contains("BO-ISS-010");
        assertThat(UiCodeResolver.resolve("APP", "BO_ID", "I-01", VERSION)).isEmpty();
        assertThat(UiCodeResolver.resolve("BO", "ROUTE", "I-01", VERSION)).isEmpty();
        assertThat(UiCodeResolver.resolve("BO", "BO_ID", "I-01", "other")).isEmpty();
        assertThat(UiCodeResolver.resolve("BO", "LEGACY", "I-10", VERSION)).isEmpty();
        assertThat(UiCodeResolver.screen("I-10", VERSION)).isEmpty();
        assertThat(UiCodeResolver.screen("/home", VERSION)).isEmpty();
        assertThat(UiCodeResolver.screen("/i-10", VERSION)).isEmpty();
        assertThat(UiCodeResolver.resolve("BO", "INTERNAL_ID", "login", VERSION)).contains("BO-SHL-010");
        assertThat(UiCodeResolver.screen("BO-ISS-010", VERSION)).contains("BO-ISS-010");
        assertThat(UiCodeResolver.screen("/i-01", VERSION)).contains("BO-ISS-010");
        for (String value : new String[]{"/i-01?q=secret", "https://host/i-01", "/i-01#secret", "/I-01"})
            assertThat(UiCodeResolver.screen(value, VERSION)).isEmpty();
    }
    @Test void screenElementVersionAndRootIdAreValidatedTogether() {
        var headers = new java.util.HashMap<>(Map.of(HeaderNames.SCREEN_ID, "/i-01",
                HeaderNames.SCREEN_CATALOG_VERSION, VERSION, HeaderNames.UI_ELEMENT_ID, "BO-ISS-010-BTN-SEARCH",
                HeaderNames.UI_ACTION, "submit", HeaderNames.EVENT_ID, "abcdef0123456789"));
        assertThat(ScreenMetadata.fromHeaders(headers::get)).containsEntry(HeaderNames.SCREEN_ID, "BO-ISS-010")
                .containsEntry(HeaderNames.EVENT_ID, "abcdef0123456789")
                .containsEntry(HeaderNames.UI_ACTION, "submit").containsKey(HeaderNames.UI_ELEMENT_ID);
        headers.put(HeaderNames.UI_ELEMENT_ID, "BO-ISS-020-BTN-SEARCH");
        headers.put(HeaderNames.UI_ACTION, "search text");
        headers.put(HeaderNames.EVENT_ID, "0000000000000000");
        headers.put(HeaderNames.INTERNAL_SCREEN_ID, "GM-SCR-123456");
        assertThat(ScreenMetadata.fromHeaders(headers::get)).doesNotContainKeys(HeaderNames.UI_ELEMENT_ID,
                HeaderNames.UI_ACTION, HeaderNames.EVENT_ID);
        assertThat(ScreenMetadata.fromHeaders(headers::get)).containsEntry(HeaderNames.INTERNAL_SCREEN_ID, "GM-SCR-123456");
        headers.put(HeaderNames.SCREEN_CATALOG_VERSION, "wrong");
        assertThat(ScreenMetadata.fromHeaders(headers::get)).containsEntry(HeaderNames.SCREEN_CATALOG_VERSION, "wrong")
                .doesNotContainKey(HeaderNames.SCREEN_ID);
    }
}
