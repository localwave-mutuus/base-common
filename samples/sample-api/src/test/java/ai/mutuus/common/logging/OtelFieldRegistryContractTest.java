package ai.mutuus.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import ai.mutuus.common.core.VerifiedIdentityContext;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class OtelFieldRegistryContractTest {
    @Test void representativeEmitterMatchesRegistryNamesTypesAndInputOnlyFourAliases() {
        var registry = TelemetryFieldRegistry.load();
        LoggerContext logback = new LoggerContext();
        try (Scope traced = Span.wrap(SpanContext.create("1234567890abcdef1234567890abcdef", "1234567890abcdef",
                     TraceFlags.getSampled(), TraceState.getDefault())).makeCurrent();
             Scope identity = VerifiedIdentityContext.makeCurrent(new VerifiedIdentityContext.Identity("member-42", "member", "authenticated"))) {
            LoggingEvent event = event(logback);
            pair(event, "event.dataset", "mutuus.security_audit");
            pair(event, "event.action", "security.authz.denied");
            pair(event, "event.category", List.of("authentication")); pair(event, "event.type", List.of("denied"));
            pair(event, "clientIp", "192.168.20.42"); pair(event, "actorId", "actor-42");
            pair(event, "deviceId", "rotating-service-pseudonym"); pair(event, "relatedMemberIds", List.of("member-10"));
            pair(event, "http.request.body.bytes", 123L); pair(event, "mutuus.payload.truncated", true);
            pair(event, "http.request.headers", Map.of("content-type", "application/json"));
            pair(event, "event.id", "must-not-override-immutable-id"); pair(event, "userId", "forged-user");
            event.setMDCPropertyMap(Map.of("X-Device-Id", "RAW_HARDWARE_MUST_NOT_APPEAR", "unreviewed", "RAW_SECRET_MUST_NOT_APPEAR"));
            Map<String, Object> snapshot = CanonicalLogSnapshot.capture(event);
            registry.validateSnapshot(snapshot);
            assertThat(snapshot).containsEntry("client.ip", "192.168.20.0").containsEntry("golmok.actor.id", "actor-42")
                    .containsEntry("golmok.device.pseudo_id", "rotating-service-pseudonym")
                    .containsEntry("golmok.related_member.ids", List.of("member-10")).containsEntry("user.id", "member-42")
                    .doesNotContainKeys("clientIp", "actorId", "deviceId", "relatedMemberIds", "X-Device-Id", "userId");
            assertThat(snapshot.get("event.id")).isNotEqualTo("must-not-override-immutable-id");
            assertThat(snapshot.toString()).doesNotContain("RAW_HARDWARE", "RAW_SECRET", "forged-user");
            assertThat(registry.normalizationAliases()).containsEntry("clientIp", "client.ip").containsEntry("actorId", "golmok.actor.id")
                    .containsEntry("deviceId", "golmok.device.pseudo_id").containsEntry("relatedMemberIds", "golmok.related_member.ids");

            LoggingEvent ordinary = event(logback); pair(ordinary, "clientIp", "192.168.20.42");
            ordinary.setMDCPropertyMap(Map.of("X-Device-Id", "RAW_DEVICE_MUST_NOT_APPEAR"));
            Map<String, Object> ordinarySnapshot = CanonicalLogSnapshot.capture(ordinary);
            registry.validateSnapshot(ordinarySnapshot);
            assertThat(ordinarySnapshot).doesNotContainKeys("client.ip", "clientIp", "deviceId", "golmok.device.pseudo_id");
            LoggingEvent fromMdc = event(logback); pair(fromMdc, "event.dataset", "mutuus.security_audit");
            fromMdc.setMDCPropertyMap(Map.of("clientIp", "192.168.20.43", "actorId", "actor-mdc",
                    "deviceId", "rotating-mdc-pseudonym", "relatedMemberIds", "[\"related-mdc\"]",
                    "userId", "untrusted-user", "trace_id", "ffffffffffffffffffffffffffffffff",
                    "X-Device-Id", "RAW_HEADER_MUST_NOT_APPEAR"));
            Map<String, Object> mdcSnapshot = CanonicalLogSnapshot.capture(fromMdc);
            registry.validateSnapshot(mdcSnapshot);
            assertThat(mdcSnapshot).containsEntry("client.ip", "192.168.20.0").containsEntry("golmok.actor.id", "actor-mdc")
                    .containsEntry("golmok.device.pseudo_id", "rotating-mdc-pseudonym")
                    .containsEntry("golmok.related_member.ids", List.of("related-mdc"))
                    .containsEntry("user.id", "member-42").containsEntry("trace.id", "1234567890abcdef1234567890abcdef")
                    .doesNotContainKeys("clientIp", "actorId", "deviceId", "relatedMemberIds");
            assertThat(mdcSnapshot.toString()).doesNotContain("RAW_HEADER", "untrusted-user");
            LoggingEvent malformedMdc = event(logback);
            malformedMdc.setMDCPropertyMap(Map.of("deviceId", "k".repeat(129), "relatedMemberIds", "not-an-array"));
            Map<String, Object> rejected = CanonicalLogSnapshot.capture(malformedMdc);
            registry.validateSnapshot(rejected);
            assertThat(rejected).doesNotContainKeys("golmok.device.pseudo_id", "golmok.related_member.ids");
        } finally { logback.stop(); }
    }

    @Test void buildGateRejectsUnknownNamesAliasOutputAndWrongJsonTypes() {
        var registry = TelemetryFieldRegistry.load();
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("unregistered.output", "value")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unregistered");
        for (String alias : List.of("clientIp", "actorId", "deviceId", "relatedMemberIds"))
            assertThatThrownBy(() -> registry.validateSnapshot(Map.of(alias, "old-output"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("golmok.schema.version", "2")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("type mismatch");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("event.category", "web")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("array");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("http.response.status_code", Long.MAX_VALUE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("type mismatch");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("attributes", Map.of("secret", "raw-value"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("marker");
        registry.validateSnapshot(Map.of("golmok.schema.version", 2, "event.duration", 1L,
                "attributes", Map.of("unreviewed", "REDACTED_UNREVIEWED")));
    }

    @Test void mutationOfRegistryBreaksOutputGateAndExpiredAliasesRemainInputOnly() {
        TelemetryFieldRegistry changed = TelemetryFieldRegistry.from(input(registryJson(field("different.field", "keyword", "[]", "[]"))));
        assertThatThrownBy(() -> changed.validateSnapshot(Map.of("message", "real emitter output")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
        TelemetryFieldRegistry oldDate = TelemetryFieldRegistry.from(input(registryJson(field("canonical.name", "keyword", "[\"oldName\"]", "[]"))));
        assertThat(oldDate.normalizationAliases()).containsEntry("oldName", "canonical.name");
        assertThat(oldDate.aliases().get("oldName").removeAfter()).isEqualTo("2000-01-01");
        assertThatThrownBy(() -> oldDate.validateSnapshot(Map.of("oldName", "expired output"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(registryJson(field("canonical.name", "keyword", "[]", "[\"hiddenAlias\"]")))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("subset");
    }

    @Test void legacyInputDeadlineIsIndependentAndRecheckedAcrossMidnight() {
        var observed = new java.util.concurrent.atomic.AtomicReference<>(java.time.Instant.parse("2000-01-01T12:00:00Z"));
        java.time.Clock clock = new java.time.Clock() {
            public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
            public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
            public java.time.Instant instant() { return observed.get(); }
        };
        String open = registryJson(field("canonical.name", "keyword", "[\"oldName\"]", "[]"));
        String limited = open.replace("\"input_accept_until\":null", "\"input_accept_until\":\"2000-01-02\"");
        var registry = TelemetryFieldRegistry.from(input(limited), clock);
        Map<String, String> retained = registry.normalizationAliases();
        assertThat(retained).containsEntry("oldName", "canonical.name");
        observed.set(java.time.Instant.parse("2000-01-02T23:59:59Z"));
        registry.requireLegacyInputAccepted("oldName");
        assertThat(retained.get("oldName")).isEqualTo("canonical.name");
        observed.set(java.time.Instant.parse("2000-01-03T00:00:00Z"));
        assertThat(retained.get("oldName")).isNull();
        assertThat(retained.entrySet()).isEmpty();
        assertThat(retained.containsKey("oldName")).isFalse();
        assertThatThrownBy(() -> registry.requireLegacyInputAccepted("oldName"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expired");
        assertThatThrownBy(() -> registry.validateSnapshot(Map.of("oldName", "output")))
                .isInstanceOf(IllegalArgumentException.class);
        var noDeadline = TelemetryFieldRegistry.from(input(open), clock);
        assertThat(noDeadline.normalizationAliases()).containsEntry("oldName", "canonical.name");
        assertThat(noDeadline.fields().get("canonical.name").inputAcceptUntil()).isNull();
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(open.replace("\"input_accept_until\":null,", ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Missing input_accept_until");
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(limited.replace("2000-01-02", "1999-12-31"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("acceptance date");
    }
    @Test void unsafeOrAmbiguousRegistryInputsFailClosed() {
        String minimal = registryJson(field("canonical.name", "keyword", "[]", "[]"));
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(minimal.replace("\"version\":1", "\"version\":1,\"version\":2"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(minimal.replace("\"version\":1", "\"version\":1,\"generated_by\":\"forbidden\""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown");
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(minimal + "{}"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input("<!DOCTYPE x SYSTEM 'file:///never-read'><x/>")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(" ".repeat(1_048_577)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TelemetryFieldRegistry.from(input(registryJson(field("invalid", "unmapped-type", "[]", "[]")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static LoggingEvent event(LoggerContext context) {
        return new LoggingEvent(OtelFieldRegistryContractTest.class.getName(), context.getLogger("registry.contract"),
                Level.INFO, "registry output contract", null, null);
    }
    private static void pair(LoggingEvent event, String name, Object value) { event.addKeyValuePair(new KeyValuePair(name, value)); }
    private static ByteArrayInputStream input(String json) { return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)); }
    private static String registryJson(String field) { return "{\"version\":1,\"fields\":[" + field + "]}"; }
    private static String field(String name, String type, String aliases, String discard) {
        return "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"tier\":\"optional\",\"meaning\":\"테스트 필드\","
                + "\"scope\":[\"request\"],\"aliases\":" + aliases + ",\"discardAliases\":" + discard + ","
                + "\"alias_until\":\"2000-01-01\",\"input_accept_until\":null,\"sensitivity\":\"none\",\"owner\":\"common\",\"structuredInput\":true}";
    }
}
