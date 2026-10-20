package ai.mutuus.common.agent;

import io.opentelemetry.sdk.autoconfigure.spi.*;
import io.opentelemetry.sdk.trace.export.*;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.lang.management.ManagementFactory;
import javax.management.*;
import java.util.*;

/** Dev fixture only, included solely in the harness extension jar. */
public final class HarnessExporter implements AutoConfigurationCustomizerProvider {
    public interface CaptureMBean { List<Map<String,String>> getSpans(); }
    public static final class Capture implements CaptureMBean {
        final List<Map<String,String>> spans = new ArrayList<>();
        public synchronized List<Map<String,String>> getSpans() { return List.copyOf(spans); }
        synchronized void add(Map<String,String> span) { spans.add(span); }
    }
    @Override public void customize(AutoConfigurationCustomizer customizer) {
        var capture = new Capture();
        try { ManagementFactory.getPlatformMBeanServer().registerMBean(capture, new ObjectName("ai.mutuus.test:type=Exported")); }
        catch (JMException e) { throw new IllegalStateException(e); }
        var exporter = new SpanExporter() {
            public CompletableResultCode export(Collection<io.opentelemetry.sdk.trace.data.SpanData> spans) {
                for (var span : spans) capture.add(Map.of("trace.id",span.getTraceId(),"span.id",span.getSpanId(),
                    "parent.id",span.getParentSpanId(),"kind",span.getKind().name(),"scope",span.getInstrumentationScopeInfo().getName()));
                return CompletableResultCode.ofSuccess();
            }
            public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode shutdown() { return flush(); }
        };
        customizer.addTracerProviderCustomizer((builder, config) -> builder.addSpanProcessor(SimpleSpanProcessor.create(exporter)));
    }
}
