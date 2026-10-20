package ai.mutuus.common.web;

import ai.mutuus.common.core.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class TraceFilterTest {
    @Test void safeHintSetIsValidatedAndPreviousMetadataRestored() throws Exception {
        var hints = java.util.Map.of(
                HeaderNames.SCREEN_ID, "BO-ISS-010",
                HeaderNames.SCREEN_CATALOG_VERSION, "1.0.0-bo.8",
                HeaderNames.BUSINESS_SCREEN_NO, "APP-123",
                HeaderNames.EVENT_ID, "abcdef0123456789",
                HeaderNames.DEVICE_LEVEL, "ANDROID",
                HeaderNames.LOCALE, "ko-KR");
        TraceContext.put(HeaderNames.SCREEN_ID, "outer-screen");
        TraceContext.put(HeaderNames.EVENT_ID, "outer-event");
        var before = TraceContext.snapshot();
        try {
            var request = new MockHttpServletRequest();
            hints.forEach(request::addHeader);
            request.addHeader(HeaderNames.DEVICE_ID, "raw-hardware-id");
            request.addHeader(HeaderNames.USER_ID, "forged-user");
            request.addHeader(HeaderNames.APP_CODE, "EVIL");
            request.addHeader(HeaderNames.INSTANCE_ID, "EVIL01");
            new TraceFilter("TEST", "INST01", true).doFilter(request, new MockHttpServletResponse(), (req,res) -> {
                assertThat(TraceContext.snapshot()).containsAllEntriesOf(hints)
                        .containsEntry(HeaderNames.APP_CODE, "TEST")
                        .containsEntry(HeaderNames.INSTANCE_ID, "INST01");
                assertThat(TraceContext.get(HeaderNames.DEVICE_ID)).isEmpty();
                assertThat(TraceContext.userId()).isNull();
            });
            assertThat(TraceContext.snapshot()).isEqualTo(before);
            var invalid = new MockHttpServletRequest();
            invalid.addHeader(HeaderNames.SCREEN_ID, "bad\nvalue");
            invalid.addHeader(HeaderNames.EVENT_ID, "x".repeat(65));
            invalid.addHeader(HeaderNames.DEVICE_LEVEL, "ANDROID WEB");
            invalid.addHeader(HeaderNames.LOCALE, "ko_KR\rmalicious");
            invalid.addHeader(HeaderNames.SCREEN_CATALOG_VERSION, "2026-10-18x1");
            invalid.addHeader(HeaderNames.BUSINESS_SCREEN_NO, "app-123");
            invalid.addHeader(HeaderNames.INTERNAL_SCREEN_ID, "GM-SCR-" + "1".repeat(65));
            new TraceFilter("TEST", "INST01").doFilter(invalid, new MockHttpServletResponse(), (req,res) -> {
                for (String key : hints.keySet()) assertThat(TraceContext.get(key)).isEmpty();
            });
            assertThat(TraceContext.snapshot()).isEqualTo(before);
        } finally { TraceContext.clear(); }
    }
    @Test void currentAgentSpanIsReturnedAndUntrustedUserNeverAccepted() throws Exception {
        var span = Span.wrap(SpanContext.create("1234567890abcdef1234567890abcdef", "1234567890abcdef", TraceFlags.getSampled(), TraceState.getDefault()));
        try (Scope ignored = span.makeCurrent()) {
            var request = new MockHttpServletRequest();
            request.addHeader(HeaderNames.TRACE_ID, "forged");
            request.addHeader(HeaderNames.USER_ID, "forged-user");
            request.addHeader(HeaderNames.SCREEN_ID, "BO-ISS-010");
            request.addHeader(HeaderNames.SCREEN_CATALOG_VERSION, "1.0.0-bo.8");
            var response = new MockHttpServletResponse();
            new TraceFilter("TEST", "INST01", true).doFilter(request, response, (req,res) -> {
                assertThat(TraceContext.traceId()).isEqualTo(span.getSpanContext().getTraceId());
                assertThat(TraceContext.userId()).isNull();
                assertThat(TraceContext.get(HeaderNames.SCREEN_ID)).contains("BO-ISS-010");
                assertThat(MDC.get(HeaderNames.TRACE_ID)).isNull();
            });
            assertThat(response.getHeader(HeaderNames.TRACE_ID)).isEqualTo(span.getSpanContext().getTraceId());
            assertThat(TraceContext.get(HeaderNames.SCREEN_ID)).isEmpty();
        }
    }
    @Test void noAgentMeansNoInventedTraceAndExceptionsRestorePreviousState() {
        TraceContext.put(HeaderNames.SCREEN_ID, "outer");
        MDC.put("owner", "outer");
        var response = new MockHttpServletResponse();
        try {
            assertThatThrownBy(() -> new TraceFilter("TEST", "INST01").doFilter(new MockHttpServletRequest(), response, (req,res) -> {
                assertThat(TraceContext.traceId()).isNull();
                TraceContext.put(HeaderNames.SCREEN_ID, "inner");
                throw new jakarta.servlet.ServletException("expected");
            })).isInstanceOf(jakarta.servlet.ServletException.class);
            assertThat(response.getHeader(HeaderNames.TRACE_ID)).isNull();
            assertThat(TraceContext.get(HeaderNames.SCREEN_ID)).contains("outer");
            assertThat(MDC.get("owner")).isEqualTo("outer");
        } finally { TraceContext.clear(); MDC.clear(); }
    }
}
