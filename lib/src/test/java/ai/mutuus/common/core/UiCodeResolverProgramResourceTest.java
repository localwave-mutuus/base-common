package ai.mutuus.common.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UiCodeResolverProgramResourceTest {
    @TempDir Path root;
    private void withResource(String content, Runnable check) throws Exception {
        if (content != null) {
            Path file = root.resolve("META-INF/ui-catalog/aliases.json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        }
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            Thread.currentThread().setContextClassLoader(loader);
            check.run();
        } finally { Thread.currentThread().setContextClassLoader(original); }
    }
    @Test void resourcePresent() throws Exception {
        withResource("[{\"system\":\"BO\",\"kind\":\"ROUTE\",\"value\":\"/program\",\"target\":\"BO-NEW-001\"}]", () -> {
            assertEquals(Optional.of("BO-NEW-001"), UiCodeResolver.screen("BO", "/program", null));
            assertEquals(Optional.of("BO-NEW-001"), UiCodeResolver.resolve("BO", "ROUTE", "/program", "any-version"));
            Map<String,String> headers = Map.of(HeaderNames.SCREEN_ID, "/program", HeaderNames.UI_ELEMENT_ID, "BO-NEW-001-SAVE");
            var metadata = ScreenMetadata.fromHeaders("BO", headers::get);
            assertEquals("BO-NEW-001", metadata.get(HeaderNames.SCREEN_ID));
            assertEquals("/program", metadata.get(ScreenMetadata.SCREEN_ALIAS));
            assertEquals("BO-NEW-001-SAVE", metadata.get(HeaderNames.UI_ELEMENT_ID));
        });
    }
    @Test void resourceAbsent() throws Exception {
        withResource(null, () -> {
            assertEquals(Optional.of("BO-NEW-002"), UiCodeResolver.screen("BO", "BO-NEW-002", null));
            assertTrue(UiCodeResolver.screen("BO", null, null).isEmpty());
            assertTrue(UiCodeResolver.screen("BO", "bad\nheader", null).isEmpty());
        });
    }
    @Test void unregisteredAndMalformedResource() throws Exception {
        withResource("[]", () -> {
            assertEquals(Optional.of("FutureScreen"), UiCodeResolver.screen("APP", "FutureScreen", null));
            assertEquals(Optional.of("FutureScreen"), UiCodeResolver.resolve("APP", "LEGACY", "FutureScreen", null));
        });
        withResource("[{", () -> {
            assertEquals(Optional.of("BO-NEW-003"), UiCodeResolver.screen("BO", "BO-NEW-003", null));
            assertTrue(UiCodeResolver.screen("BO", "", null).isEmpty());
        });
    }
}
