package ai.mutuus.common.agent;
import ai.mutuus.common.observability.SpanLineageSpanProcessor;
import io.opentelemetry.sdk.autoconfigure.spi.*;
public final class LineageAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {
    @Override public void customize(AutoConfigurationCustomizer customizer) {
        customizer.addTracerProviderCustomizer((builder, config) -> {
            if (config.getBoolean("mutuus.common.observability.lineage.enabled", true)) {
                var processor = new SpanLineageSpanProcessor(config.getInt("mutuus.common.observability.lineage.max-active-spans", 100000));
                boolean transport = config.getBoolean("otel.instrumentation.mutuus-jdk-transport.enabled", false);
                boolean urlTransport = config.getBoolean("otel.instrumentation.mutuus-url-transport.enabled", false);
                if (transport || urlTransport) {
                    if (!config.getBoolean("otel.javaagent.experimental.indy", false))
                        throw new IllegalArgumentException("Pinned transport requires agent isolated advice (experimental.indy=true)");
                    if (transport && config.getBoolean("otel.instrumentation.java-http-client.enabled", true))
                        throw new IllegalArgumentException("Disable java-http-client before enabling mutuus-jdk-transport");
                    if (!System.getProperty("java.runtime.version").equals("21.0.12.1+1-1-24.04.4-Ubuntu"))
                        throw new IllegalArgumentException("Unverified JDK transport ABI: " + System.getProperty("java.runtime.version"));
                    if (urlTransport && config.getBoolean("otel.instrumentation.http-url-connection.enabled", true))
                        throw new IllegalArgumentException("Disable http-url-connection before enabling mutuus-url-transport");
                    if (urlTransport && !ModuleLayer.boot().findModule("java.base").orElseThrow().isExported("sun.net.www", getClass().getModule()))
                        throw new IllegalArgumentException("Pinned URL transport requires --add-exports=java.base/sun.net.www=ALL-UNNAMED");
                    processor.configureTransport(config.getList("mutuus.common.observability.propagation.allowed-hosts", java.util.List.of()));
                }
                processor.register(); builder.addSpanProcessor(processor);
            }
            return builder;
        });
    }
}
