package ai.mutuus.common.logging;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLogFilterTest {

    private final CommonLoggingProperties props = new CommonLoggingProperties();
    private final AccessLogFilter filter = new AccessLogFilter(new AccessLogger(), props);
    private ListAppender<ILoggingEvent> appender;
    private ai.mutuus.common.logging.TelemetryTestEvents.Capture snapshotCapture;
    private Logger logger;

    @Test
    void 접근로그는_실제_SDK_span을_컨텍스트_정리뒤에도_한번_남긴다() throws Exception {
        var remote = io.opentelemetry.api.trace.Span.wrap(io.opentelemetry.api.trace.SpanContext.createFromRemoteParent(
                "1234567890abcdef1234567890abcdef", "1234567890abcdef",
                io.opentelemetry.api.trace.TraceFlags.getSampled(), io.opentelemetry.api.trace.TraceState.getDefault()));
        String trace, span;
        try (var traced = TelemetryTestEvents.span("server", io.opentelemetry.context.Context.root().with(remote))) {
            trace = traced.traceId(); span = traced.spanId();
            assertThat(((io.opentelemetry.sdk.trace.ReadableSpan) traced.span()).getParentSpanContext().getSpanId()).isEqualTo("1234567890abcdef");
            var request = new MockHttpServletRequest("GET", "/api/orders");
            request.addHeader("X-Trace-Id", "forged");
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                    new ai.mutuus.common.web.TraceFilter("TEST", "INST01").doFilter(req, res, (r, s) -> {
                        assertThat(ai.mutuus.common.core.TraceContext.traceId()).isEqualTo(trace);
                    }));
        }
        assertThat(appender.list).hasSize(1);
        assertThat(kv(appender.list.getFirst())).containsEntry("trace.id", trace).containsEntry("span.id", span)
                .containsEntry("event.action", "http.server.complete");
        assertThat(ai.mutuus.common.core.TraceContext.snapshot()).isEmpty();
        assertThat(io.opentelemetry.api.trace.Span.current().getSpanContext().isValid()).isFalse();
    }
    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        snapshotCapture = ai.mutuus.common.logging.TelemetryTestEvents.attach(logger, appender);
    }

    @AfterEach
    void tearDown() {
        snapshotCapture.close();
    }

    @Test
    void 정상요청은_received와_completed를_순서대로_남긴다() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/orders");
        request.setQueryString("page=1");
        var response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, new MockFilterChain());

        List<String> events = events();
        assertThat(events).containsExactly("http.server.complete");
        assertThat(kv(appender.list.getFirst()))
                .containsEntry("http.response.status_code", 200)
                .containsKey("event.duration");
    }

    @Test
    void 제외경로는_로깅하지_않는다() throws Exception {
        // 기본 제외 접두사: /actuator
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(appender.list).isEmpty();
    }

    @Test
    void 체인에서_예외가_나도_completed가_기록된다() {
        var request = new MockHttpServletRequest("POST", "/api/orders");
        var response = new MockHttpServletResponse();
        MockFilterChain boom = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new RuntimeException("downstream failure");
            }
        };

        try {
            filter.doFilter(request, response, boom);
        } catch (Exception ignored) {
            // 필터는 예외를 전파하되 finally 에서 completed 를 남겨야 한다
        }

        assertThat(events()).containsExactly("http.server.complete");
    }

    private List<String> events() {
        return appender.list.stream()
                .map(e -> String.valueOf(kv(e).get("event.action")))
                .toList();
    }

    private Map<String, Object> kv(ILoggingEvent ev) {
        return TelemetryTestEvents.fields(ev);
    }
}
