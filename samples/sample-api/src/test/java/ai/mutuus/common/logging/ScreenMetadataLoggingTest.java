package ai.mutuus.common.logging;

import java.util.Map;
import ai.mutuus.common.core.*;
import ai.mutuus.common.web.TraceFilter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.*;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.assertThat;

/** 공통 소비자가 선택 메타데이터를 수신/종료/보안거부/async에 보존하는지 확인한다. */
class ScreenMetadataLoggingTest {
    private static final Map<String, String> VALID = Map.of(
            HeaderNames.SCREEN_CATALOG_VERSION, "1.0.0-bo.8",
            HeaderNames.BUSINESS_SCREEN_NO, "AAA-123",
            HeaderNames.SCREEN_ID, "BO-ISS-010",
            HeaderNames.UI_ELEMENT_ID, "BO-ISS-010-BTN-SEARCH",
            HeaderNames.UI_ACTION, "press",
            HeaderNames.EVENT_ID, "abcdef0123456789");

    @Test void 형식이_잘못된_메타데이터만_생략한다() {
        assertThat(ScreenMetadata.fromHeaders(VALID::get)).isEqualTo(VALID);
        for (String invalid : new String[]{"", " ", "private\r\ninjected", "2026-10-05x1", "2026-10-05\\x1", "a".repeat(100)}) {
            assertThat(ScreenMetadata.fromHeaders(name -> invalid)).isEmpty();
        }
        assertThat(ScreenMetadata.fromHeaders(Map.of(HeaderNames.BUSINESS_SCREEN_NO, "aaa-123",
                HeaderNames.INTERNAL_SCREEN_ID, "GM-SCR-12345", HeaderNames.SCREEN_CATALOG_VERSION, "2026-10-05.1 ")::get)).containsOnlyKeys(HeaderNames.INTERNAL_SCREEN_ID);
    }

    @Test void trace_문맥은_구형_헤더와_신규_세개를_보존하고_종료후_정리한다() throws Exception {
        var request = request();
        new TraceFilter("TEST", "INST01").doFilter(request, new MockHttpServletResponse(), (req,res) -> {
            VALID.forEach((key,value) -> {
                assertThat(MDC.get(key)).isNull();
                assertThat(TraceContext.get(key)).contains(value);
            });
            assertThat(TraceContext.get(HeaderNames.SCREEN_ID)).contains("BO-ISS-010");
            assertThat(TraceContext.get(HeaderNames.EVENT_ID)).contains("abcdef0123456789");
        });
        VALID.keySet().forEach(key -> { assertThat(MDC.get(key)).isNull(); assertThat(TraceContext.get(key)).isEmpty(); });
        assertThat(java.util.List.of(HeaderNames.PROPAGATED)).contains(HeaderNames.UI_ELEMENT_ID, HeaderNames.SCREEN_CATALOG_VERSION);
    }

    @Test void 가장앞_수신과_정리뒤_응답_및_보안거부에도_동일한_필드가_남는다() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); var snapshotCapture = ai.mutuus.common.logging.TelemetryTestEvents.attach(logger, capture);
        try {
            var filter = new AccessLogFilter(new AccessLogger(), new CommonLoggingProperties());
            filter.doFilter(request(), new MockHttpServletResponse(), (req,res) ->
                    new TraceFilter("TEST", "INST01").doFilter(req,res,(r,s) -> {}));
            filter.doFilter(request(), new MockHttpServletResponse(), (req,res) -> ((jakarta.servlet.http.HttpServletResponse)res).setStatus(401));
            assertThat(capture.list).hasSize(2);
            capture.list.forEach(event -> VALID.forEach((key,value) -> assertThat(field(event,key)).isEqualTo(value)));
            assertThat(field(capture.list.get(1),"http.response.status_code")).isEqualTo(401);
        } finally { snapshotCapture.close(); }
    }

    @Test void 비동기_완료_콜백과_다음_요청은_메타데이터를_공유하지_않는다() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); var snapshotCapture = ai.mutuus.common.logging.TelemetryTestEvents.attach(logger, capture);
        try {
            var filter = new AccessLogFilter(new AccessLogger(), new CommonLoggingProperties());
            var request = request(); request.setAsyncSupported(true);
            var response = new MockHttpServletResponse();
            filter.doFilter(request,response,(req,res) -> req.startAsync());
            var async = (MockAsyncContext) request.getAsyncContext();
            var listener = async.getListeners().getFirst();
            var event = new jakarta.servlet.AsyncEvent(async,request,response);
            listener.onComplete(event); listener.onComplete(event);
            assertThat(capture.list).hasSize(1);
            VALID.forEach((key,value) -> assertThat(field(capture.list.get(0),key)).isEqualTo(value));
            filter.doFilter(new MockHttpServletRequest("GET","/normal"),new MockHttpServletResponse(),(r,s) -> {});
            assertThat(capture.list).hasSize(2);
            VALID.keySet().forEach(key -> assertThat(field(capture.list.get(1),key)).isNull());
        } finally { snapshotCapture.close(); }
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/screen"); VALID.forEach(request::addHeader); return request;
    }

    @Test void invalidMetadataDoesNotRejectRequest() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); var snapshotCapture = ai.mutuus.common.logging.TelemetryTestEvents.attach(logger, capture);
        try {
            var request = request(); request.removeHeader(HeaderNames.BUSINESS_SCREEN_NO);
            request.addHeader(HeaderNames.BUSINESS_SCREEN_NO, "PRIVATE_DO_NOT_LOG");
            var response = new MockHttpServletResponse();
            new AccessLogFilter(new AccessLogger(),new CommonLoggingProperties()).doFilter(request,response,
                    (req,res) -> ((jakarta.servlet.http.HttpServletResponse)res).setStatus(202));
            assertThat(response.getStatus()).isEqualTo(202);
            assertThat(capture.list).hasSize(1);
            var encoder = new CommonJsonEncoder();
            encoder.setContext(logger.getLoggerContext()); encoder.start();
            for (var event : capture.list) {
                assertThat(field(event,HeaderNames.BUSINESS_SCREEN_NO)).isNull();
                String json = new String(encoder.encode(event),java.nio.charset.StandardCharsets.UTF_8);
                assertThat(json).contains("catalog_version", "1.0.0-bo.8", "element", "BO-ISS-010-BTN-SEARCH");
                assertThat(json).doesNotContain("PRIVATE_DO_NOT_LOG", "X-Business-Screen-No", "internal_id");
            }
            encoder.stop();
        } finally { snapshotCapture.close(); }
    }
    private Object field(ILoggingEvent event,String name) {
        String canonical = ScreenMetadata.FIELDS.getOrDefault(name, name);
        return CanonicalLogSnapshot.snapshot(event).get(canonical);
    }
}
