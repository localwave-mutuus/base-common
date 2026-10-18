package ai.mutuus.common.core;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TraceHeadersTest {
    private static final String TRACE = "1234567890abcdef1234567890abcdef";
    @Test void inboundNeverCreatesSpanOrTrustsCustomTrace() {
        var value = TraceHeaders.inbound(Map.of(HeaderNames.TRACE_ID, "forged", HeaderNames.SPAN_ID, "forged")::get, true);
        assertThat(value.traceId()).isNull();
        assertThat(value.spanId()).isNull();
        assertThat(value.parentId()).isNull();
    }
    @Test void inboundReadsCurrentAgentSpan() {
        var context = SpanContext.create(TRACE, "1234567890abcdef", TraceFlags.getSampled(), TraceState.getDefault());
        try (Scope ignored = Span.wrap(context).makeCurrent()) {
            var value = TraceHeaders.inbound(key -> "forged", true);
            assertThat(value.traceId()).isEqualTo(TRACE);
            assertThat(value.spanId()).isEqualTo(context.getSpanId());
            assertThat(value.parentId()).isNull();
        }
    }
    @Test void jdkClientInjectionBelongsToAgentAndDisabledExplicitInjectionIsEmpty() {
        var request = TraceHeaders.apply(HttpRequest.newBuilder(URI.create("https://example.test"))).build();
        assertThat(request.headers().map()).isEmpty();
        assertThat(TraceHeaders.outbound(false)).isEmpty();
    }
}
