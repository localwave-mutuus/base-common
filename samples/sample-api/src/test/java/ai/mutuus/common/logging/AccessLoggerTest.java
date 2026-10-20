package ai.mutuus.common.logging;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLoggerTest {

    private final AccessLogger accessLogger = new AccessLogger();
    private ListAppender<ILoggingEvent> appender;
    private ai.mutuus.common.logging.TelemetryTestEvents.Capture snapshotCapture;
    private Logger logger;

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
        TraceContext.clear();
    }

    @Test
    void requestReceived는_event와_http필드를_남긴다() {
        accessLogger.requestReceived("GET", "/api/orders", "page=1", "10.0.0.1");

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.INFO);
        assertThat(kv(ev))
                .containsEntry("event.action", "request.received")
                .containsEntry("http.request.method", "GET")
                .containsEntry("url.path", "/api/orders")
                .doesNotContainKey("url.query")
                .doesNotContainKey("client.ip");
    }

    @Test
    void requestCompleted는_상태와_소요시간을_남기고_5xx면_WARN이다() {
        accessLogger.requestCompleted("GET", "/api/orders", 500, 12L, 12_000_000L, "10.0.0.1", false);

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.WARN);
        assertThat(kv(ev))
                .containsEntry("event.action", "request.completed")
                .containsEntry("http.response.status_code", 500)
                // 정본 단일 출력 필드
                .containsEntry("event.dataset", "mutuus.access")
                .containsEntry("event.outcome", "failure")
                .containsEntry("http.response.status_code", 500)
                .containsEntry("event.duration", 12_000_000L)
                .doesNotContainKey("client.ip");
    }

    @Test
    void 느린요청은_WARN과_slow플래그를_남긴다() {
        accessLogger.requestCompleted("GET", "/api/slow", 200, 3000L, 3_000_000_000L, "10.0.0.1", true);

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.WARN);
        assertThat(kv(ev)).containsEntry("golmok.slow", true).containsEntry("http.response.status_code", 200);
    }

    @Test
    void authFailure는_WARN으로_reason을_남긴다() {
        accessLogger.authFailure("/api/secure", "expired token");

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.WARN);
        assertThat(kv(ev))
                .containsEntry("event.action", "auth.failure")
                .containsEntry("golmok.security.reason", "expired token");
    }

    @Test
    void serverError는_ERROR로_예외와_스택을_남긴다() {
        accessLogger.serverError("/api/orders", new IllegalStateException("boom"));

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.ERROR);
        assertThat(kv(ev))
                .containsEntry("event.action", "error.server")
                .containsEntry("error.type", "java.lang.IllegalStateException");
        assertThat(ev.getThrowableProxy()).isNotNull();
        assertThat(ev.getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Test
    void 검증신원_scope가_사용자와_realm을_함께_남긴다() {
        try (var identity = TelemetryTestEvents.identity("u-7", "member")) {

        accessLogger.requestCompleted("GET", "/api/orders", 200, 5L, 5_000_000L, null, false);

        assertThat(kv(single())).containsEntry("user.id", "u-7").containsEntry("golmok.identity.realm", "member");
        }
    }

    private ILoggingEvent single() {
        List<ILoggingEvent> list = appender.list;
        assertThat(list).hasSize(1);
        return list.get(0);
    }

    private Map<String, Object> kv(ILoggingEvent ev) {
        return TelemetryTestEvents.fields(ev);
    }
}
