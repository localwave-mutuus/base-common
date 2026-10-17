package ai.mutuus.common.core;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TraceHeadersTest {
    private static final String TRACE = "1234567890abcdef1234567890abcdef";
    private static final String SPAN = "1234567890abcdef";
    private static final String PARENT = "00-" + TRACE + "-" + SPAN + "-00";

    @AfterEach void clear() { TraceContext.clear(); System.clearProperty(TraceHeaders.W3C_RUNTIME_PROPERTY); }

    @Test void W3C만_온_요청은_trace와_parent를_수용하고_새_span을_만든다() {
        var inbound = TraceHeaders.inbound(Map.of(TraceHeaders.TRACEPARENT, PARENT)::get, true);
        assertThat(inbound.traceId()).isEqualTo(TRACE);
        assertThat(inbound.parentId()).isEqualTo(SPAN);
        assertThat(inbound.spanId()).hasSize(16).isNotEqualTo(SPAN);
        assertThat(inbound.flags()).isEqualTo("00");
    }

    @Test void 레거시_커스텀_상관키는_우선하고_다른_W3C_parent와_혼합하지_않는다() {
        var inbound = TraceHeaders.inbound(Map.of(HeaderNames.TRACE_ID, "legacy",
                TraceHeaders.TRACEPARENT, PARENT)::get, true);
        assertThat(inbound.traceId()).isEqualTo("legacy");
        assertThat(inbound.parentId()).isNull();
        var custom = TraceHeaders.inbound(Map.of(HeaderNames.TRACE_ID, "legacy",
                HeaderNames.SPAN_ID, "old-span", TraceHeaders.TRACEPARENT, PARENT)::get, true);
        assertThat(custom.parentId()).isEqualTo("old-span");
    }

    @Test void 잘못된_W3C와_영_ID는_수용하지_않고_비활성화도_따른다() {
        for (String bad : new String[] {"malformed", "00-" + "0".repeat(32) + "-" + SPAN + "-01",
                "00-" + TRACE + "-" + "0".repeat(16) + "-01", PARENT + "-extra"}) {
            var inbound = TraceHeaders.inbound(Map.of(TraceHeaders.TRACEPARENT, bad)::get, true);
            assertThat(inbound.traceId()).isNotEqualTo(TRACE);
            assertThat(inbound.parentId()).isNull();
        }
        assertThat(TraceHeaders.inbound(Map.of(TraceHeaders.TRACEPARENT, PARENT)::get, false).parentId()).isNull();
    }

    @Test void JavaHttp에_현재_span과_유효한_W3C를_전파한다() {
        TraceContext.put(HeaderNames.TRACE_ID, TRACE);
        TraceContext.put(HeaderNames.SPAN_ID, SPAN);
        TraceContext.put(TraceHeaders.TRACEPARENT, PARENT);
        var request = TraceHeaders.apply(HttpRequest.newBuilder(URI.create("https://example.test"))).build();
        assertThat(request.headers().firstValue(HeaderNames.TRACE_ID)).contains(TRACE);
        assertThat(request.headers().firstValue(HeaderNames.SPAN_ID)).contains(SPAN);
        assertThat(request.headers().firstValue(TraceHeaders.TRACEPARENT)).contains(PARENT);
    }

    @Test void JavaHttp는_레거시ID를_W3C로_위조하지_않고_요청과_비웹_토글을_따른다() {
        TraceContext.put(HeaderNames.TRACE_ID, "legacy");
        TraceContext.put(HeaderNames.SPAN_ID, SPAN);
        assertThat(build().headers().firstValue(TraceHeaders.TRACEPARENT)).isEmpty();
        TraceContext.put(HeaderNames.TRACE_ID, TRACE);
        System.setProperty(TraceHeaders.W3C_RUNTIME_PROPERTY, "false");
        assertThat(build().headers().firstValue(TraceHeaders.TRACEPARENT)).isEmpty();
        TraceContext.put(TraceHeaders.W3C_ENABLED, "true");
        assertThat(build().headers().firstValue(TraceHeaders.TRACEPARENT)).isPresent();
    }

    private HttpRequest build() { return TraceHeaders.apply(HttpRequest.newBuilder(URI.create("https://example.test"))).build(); }
}
