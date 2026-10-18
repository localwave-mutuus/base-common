package ai.mutuus.common.logging;

import ai.mutuus.common.core.VerifiedIdentityContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class OtelFieldRegistryContractTest {
    @Test void canonicalOnlyEmitterPreservesTypedBusinessFieldsAndVerifiedIdentity() {
        var registry = TelemetryFieldRegistry.load();
        var logback = new LoggerContext();
        try (var traced = Span.wrap(SpanContext.create("1234567890abcdef1234567890abcdef", "1234567890abcdef",
                    TraceFlags.getSampled(), TraceState.getDefault())).makeCurrent();
             var identity = VerifiedIdentityContext.makeCurrent(new VerifiedIdentityContext.Identity("member-42", "member", "authenticated"))) {
            LoggingEvent event = event(logback);
            pair(event, "event.dataset", "mutuus.security_audit"); pair(event, "event.action", "security.authz.denied");
            pair(event, "event.category", List.of("authentication")); pair(event, "event.type", List.of("denied"));
            pair(event, "client.ip", "192.168.20.42"); pair(event, "golmok.actor.id", "actor-42");
            pair(event, "golmok.device.pseudo_id", "rotating-pseudonym"); pair(event, "golmok.related_member.ids", List.of("member-10"));
            pair(event, "http.request.body.bytes", 123L); pair(event, "mutuus.payload.truncated", true);
            pair(event, "http.request.headers", Map.of("content-type", "application/json"));
            pair(event, "event.id", "must-not-override-immutable-id"); pair(event, "user.id", "forged-user");
            event.setMDCPropertyMap(Map.of("golmok.merchant.id", "01J00000000000000000000001", "unknown_secret", "RAW_SECRET"));
            Map<String, Object> snapshot = CanonicalLogSnapshot.capture(event);
            registry.validateSnapshot(snapshot);
            assertThat(snapshot).containsEntry("client.ip", "192.168.20.0").containsEntry("golmok.actor.id", "actor-42")
                    .containsEntry("golmok.device.pseudo_id", "rotating-pseudonym")
                    .containsEntry("golmok.related_member.ids", List.of("member-10"))
                    .containsEntry("golmok.merchant.id", "01J00000000000000000000001")
                    .containsEntry("user.id", "member-42").containsEntry("golmok.attributes.unmapped_count", 1)
                    .doesNotContainKey("attributes");
            assertThat(snapshot.get("event.id")).isNotEqualTo("must-not-override-immutable-id");
            assertThat(snapshot.toString()).doesNotContain("RAW_SECRET", "unknown_secret", "forged-user");

            LoggingEvent mdc = event(logback); pair(mdc, "event.dataset", "mutuus.security_audit");
            mdc.setMDCPropertyMap(Map.of("client.ip", "192.168.20.43", "golmok.actor.id", "actor-mdc",
                    "golmok.device.pseudo_id", "rotating-mdc-pseudonym", "golmok.related_member.ids", "[\"related-mdc\"]"));
            Map<String, Object> canonicalMdc = CanonicalLogSnapshot.capture(mdc);
            registry.validateSnapshot(canonicalMdc);
            assertThat(canonicalMdc).containsEntry("client.ip", "192.168.20.0").containsEntry("golmok.actor.id", "actor-mdc")
                    .containsEntry("golmok.device.pseudo_id", "rotating-mdc-pseudonym")
                    .containsEntry("golmok.related_member.ids", List.of("related-mdc"));

            LoggingEvent unknown = event(logback);
            pair(unknown, "unregistered.input", "MUST_NOT_APPEAR");
            unknown.setMDCPropertyMap(Map.of("unregistered.input", "MUST_NOT_APPEAR"));
            Map<String, Object> rejected = CanonicalLogSnapshot.capture(unknown);
            registry.validateSnapshot(rejected);
            assertThat(rejected).doesNotContainKeys("unregistered.input", "attributes")
                    .containsEntry("golmok.attributes.unmapped_count", 1);
            assertThat(rejected.toString()).doesNotContain("MUST_NOT_APPEAR");
        } finally { logback.stop(); }
    }

    @Test void buildGateRejectsUnknownNamesAndWrongTypes() {
        var registry = TelemetryFieldRegistry.load();
        for (String name : List.of("unregistered.output", "unregistered_input"))
            assertThatThrownBy(() -> registry.validateSnapshot(Map.of(name, "rejected")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unregistered");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("golmok.schema.version", "2")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("type mismatch");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("event.category", "web")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("array");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("http.response.status_code", Long.MAX_VALUE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("type mismatch");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("attributes", Map.of("anything", "REDACTED_UNREVIEWED"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not accepted");
        registry.validateSnapshot(Map.of("golmok.schema.version", 2, "event.duration", 1L));
    }

    @Test void changedRegistryBreaksOutputGateAndCompatibilityMetadataIsRejected() {
        String field = field("different.field", "keyword");
        var changed = TelemetryFieldRegistry.from(input(registryJson(field)));
        assertThatThrownBy(() -> changed.validateSnapshot(Map.of("message", "real emitter output")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
        for (String property : List.of("aliases", "alias_until", "input_accept_until", "discardAliases")) {
            String withCompatibility = field.replace("\"name\":", "\"" + property + "\":[],\"name\":");
            assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(registryJson(withCompatibility))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown registry property");
        }
        assertThat(TelemetryFieldRegistry.load().version()).isEqualTo(3);
        assertThat(TelemetryFieldRegistry.load().fields()).hasSize(89);
    }

    @Test void unsafeOrAmbiguousJsonFailsClosed() {
        String minimal = registryJson(field("canonical.name", "keyword"));
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(minimal.replace("\"version\":3", "\"version\":3,\"version\":4"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(minimal.replace("\"version\":3", "\"version\":3,\"generated_by\":\"forbidden\""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown");
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(minimal + "{}"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input("<!DOCTYPE x SYSTEM 'file:///never-read'><x/>"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(" ".repeat(1_048_577)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(registryJson(field("invalid", "unmapped-type")))))
                .isInstanceOf(IllegalArgumentException.class);
    }
    private static LoggingEvent event(LoggerContext context) {
        return new LoggingEvent(OtelFieldRegistryContractTest.class.getName(), context.getLogger("registry.contract"), Level.INFO,
                "registry output contract", null, null);
    }
    private static void pair(LoggingEvent event, String name, Object value) { event.addKeyValuePair(new KeyValuePair(name, value)); }
    private static ByteArrayInputStream input(String json) { return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)); }
    private static String registryJson(String field) { return "{\"version\":3,\"fields\":[" + field + "]}"; }
    private static String field(String name, String type) {
        return "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"tier\":\"optional\",\"meaning\":\"테스트 필드\","
                + "\"scope\":[\"request\"],\"sensitivity\":\"none\",\"owner\":\"common\",\"structuredInput\":true}";
    }
}
