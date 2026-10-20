package ai.mutuus.common.observability;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.*;
import io.opentelemetry.sdk.trace.*;
import org.junit.jupiter.api.Test;
import java.net.URLClassLoader;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import static org.assertj.core.api.Assertions.*;
class SpanLineageSpanProcessorTest {
    @Test void sdkRemoteParentCanonicalSnapshotEndRemovalAndIsolatedLoader() throws Exception {
        var processor = new SpanLineageSpanProcessor(10); processor.register();
        try (var sdk = SdkTracerProvider.builder().addSpanProcessor(processor).build()) {
            var remote = SpanContext.createFromRemoteParent("1234567890abcdef1234567890abcdef", "abcdef1234567890", TraceFlags.getSampled(), TraceState.getDefault());
            var server = sdk.get("fixture").spanBuilder("server").setSpanKind(SpanKind.SERVER)
                .setParent(Context.root().with(Span.wrap(remote))).startSpan();
            TelemetryContextSnapshot snapshot;
            try (var ignored = server.makeCurrent()) {
                snapshot = TelemetryContextSnapshot.capture();
                assertThat(ai.mutuus.common.logging.CanonicalLogSnapshot.context()).containsEntry("parent.id", remote.getSpanId());
                assertThat(((ReadableSpan)server).toSpanData().getParentSpanId()).isEqualTo(snapshot.fields().get("parent.id"));
                try (var loader = new URLClassLoader(new java.net.URL[]{PlatformSpanLineageAccessor.class.getProtectionDomain().getCodeSource().getLocation()}, ClassLoader.getPlatformClassLoader())) {
                    var type = loader.loadClass(PlatformSpanLineageAccessor.class.getName());
                    assertThat(type).isNotEqualTo(PlatformSpanLineageAccessor.class);
                    assertThat(type.getMethod("parentId", String.class, String.class).invoke(type.getConstructor().newInstance(), remote.getTraceId(), server.getSpanContext().getSpanId()))
                        .isEqualTo(java.util.Optional.of(remote.getSpanId()));
                }
            }
            server.end();
            assertThat(new PlatformSpanLineageAccessor().parentId(remote.getTraceId(), server.getSpanContext().getSpanId())).isEmpty();
            var task = snapshot.wrap(() -> assertThat(TelemetryContextSnapshot.capture().fields()).containsEntry("parent.id", remote.getSpanId()));
            var future = java.util.concurrent.CompletableFuture.runAsync(task); future.get();
        }
    }
    @Test void rootAndLimitAreNotGuessedAndLiveEntriesAreRemoved() throws Exception {
        var processor = new SpanLineageSpanProcessor(1); processor.register();
        try (var sdk = SdkTracerProvider.builder().addSpanProcessor(processor).build()) {
            var root = sdk.get("fixture").spanBuilder("root").setNoParent().startSpan();
            try (var ignored = root.makeCurrent()) {
                assertThat(TelemetryContextSnapshot.capture().fields()).doesNotContainKey("parent.id");
                var child = sdk.get("fixture").spanBuilder("child").startSpan();
                try (var nested = child.makeCurrent()) { assertThat(TelemetryContextSnapshot.capture().fields()).doesNotContainKey("parent.id"); }
                assertThat(ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName(SpanLineageSpanProcessor.NAME), "Dropped")).isEqualTo(1L);
                child.end();
            }
            root.end();
            assertThat(ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName(SpanLineageSpanProcessor.NAME), "Active")).isEqualTo(0);
        }
        assertThat(new PlatformSpanLineageAccessor().parentId("0", "0")).isEmpty();
    }
    @Test void exporterParentMatchesCanonicalAndSpiProviderIsDiscoverable() {
        var exported = new java.util.ArrayList<io.opentelemetry.sdk.trace.data.SpanData>();
        var exporter = new io.opentelemetry.sdk.trace.export.SpanExporter() {
            public io.opentelemetry.sdk.common.CompletableResultCode export(java.util.Collection<io.opentelemetry.sdk.trace.data.SpanData> spans) { exported.addAll(spans); return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess(); }
            public io.opentelemetry.sdk.common.CompletableResultCode flush() { return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess(); }
            public io.opentelemetry.sdk.common.CompletableResultCode shutdown() { return flush(); }
        };
        assertThat(java.util.ServiceLoader.load(io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider.class).stream()
            .anyMatch(provider -> provider.type().getName().equals("ai.mutuus.common.agent.LineageAutoConfigurationCustomizerProvider"))).isTrue();
        var lineage = new SpanLineageSpanProcessor(10); lineage.register();
        try (var sdk = SdkTracerProvider.builder().addSpanProcessor(lineage)
                .addSpanProcessor(io.opentelemetry.sdk.trace.export.SimpleSpanProcessor.create(exporter)).build()) {
            var root = sdk.get("fixture").spanBuilder("root").setNoParent().startSpan();
            java.util.Map<String,Object> fields;
            try (var ignored = root.makeCurrent()) {
                var child = sdk.get("fixture").spanBuilder("child").startSpan();
                try (var active = child.makeCurrent()) { fields = ai.mutuus.common.logging.CanonicalLogSnapshot.context(); }
                child.end();
            }
            root.end();
            assertThat(exported).hasSize(2);
            assertThat(fields).containsEntry("trace.id",exported.getFirst().getTraceId())
                .containsEntry("span.id",exported.getFirst().getSpanId()).containsEntry("parent.id",exported.getFirst().getParentSpanId());
        }
    }
}
