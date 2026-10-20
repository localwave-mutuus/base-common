package ai.mutuus.common.agent;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.*;
import io.opentelemetry.sdk.autoconfigure.spi.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fail-closed fallback: SDK CLIENT URL is not authoritative for a redirected wire destination.
 * No approved-host propagation until an agent transport hook supplies final destination evidence.
 * Inbound extraction is unchanged; no application interceptor injection or header deletion.
 */
public final class TrustedDestinationPropagationCustomizer implements AutoConfigurationCustomizerProvider {
    private static final AtomicLong blocked = new AtomicLong();
    public static long blockedInjectionCount() { return blocked.get(); }
    @Override public void customize(AutoConfigurationCustomizer customizer) {
        customizer.addPropagatorCustomizer((original, config) -> {
            if (config.getBoolean("mutuus.common.observability.propagation.external-enabled", false))
                throw new IllegalArgumentException("External propagation requires a verified final-destination transport hook");
            return new DestinationPropagator(original,
                config.getList("mutuus.common.observability.propagation.allowed-hosts", List.of()));
        });
    }
    static final class DestinationPropagator implements TextMapPropagator {
        private final TextMapPropagator extraction;
        DestinationPropagator(TextMapPropagator extraction, List<String> hosts) {
            this.extraction = extraction;
            for (String host : hosts) {
                if (host == null || !host.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?")
                        || host.contains("..")) throw new IllegalArgumentException("Exact propagation hostname required");
            }
        }
        @Override public Collection<String> fields() { return extraction.fields(); }
        @Override public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
            return extraction.extract(context, carrier, getter);
        }
        @Override public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
            // Even an exact allowlist match against initial CLIENT URL cannot authorize a redirect.
            blocked.updateAndGet(value -> value == Long.MAX_VALUE ? value : value + 1);
        }
    }
}
