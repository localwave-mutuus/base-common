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
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        TraceContext.clear();
    }

    @Test
    void requestReceived는_event와_http필드를_남긴다() {
        accessLogger.requestReceived("GET", "/api/orders", "page=1", "10.0.0.1");

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.INFO);
        assertThat(kv(ev))
                .containsEntry("event", "request.received")
                .containsEntry("httpMethod", "GET")
                .containsEntry("httpPath", "/api/orders")
                .containsEntry("httpQuery", "page=1")
                .containsEntry("clientIp", "10.0.0.1");
    }

    @Test
    void requestCompleted는_상태와_소요시간을_남기고_5xx면_WARN이다() {
        accessLogger.requestCompleted("GET", "/api/orders", 500, 12L, false);

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.WARN);
        assertThat(kv(ev))
                .containsEntry("event", "request.completed")
                .containsEntry("httpStatus", 500)
                .containsEntry("durationMs", 12L);
    }

    @Test
    void 느린요청은_WARN과_slow플래그를_남긴다() {
        accessLogger.requestCompleted("GET", "/api/slow", 200, 3000L, true);

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.WARN);
        assertThat(kv(ev)).containsEntry("slow", true).containsEntry("httpStatus", 200);
    }

    @Test
    void authFailure는_WARN으로_reason을_남긴다() {
        accessLogger.authFailure("/api/secure", "expired token");

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.WARN);
        assertThat(kv(ev))
                .containsEntry("event", "auth.failure")
                .containsEntry("reason", "expired token");
    }

    @Test
    void serverError는_ERROR로_예외와_스택을_남긴다() {
        accessLogger.serverError("/api/orders", new IllegalStateException("boom"));

        ILoggingEvent ev = single();
        assertThat(ev.getLevel()).isEqualTo(Level.ERROR);
        assertThat(kv(ev))
                .containsEntry("event", "error.server")
                .containsEntry("exception", "java.lang.IllegalStateException");
        assertThat(ev.getThrowableProxy()).isNotNull();
        assertThat(ev.getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Test
    void TraceContext의_userId가_있으면_userId_필드가_포함된다() {
        TraceContext.put(HeaderNames.USER_ID, "u-7");

        accessLogger.requestCompleted("GET", "/api/orders", 200, 5L, false);

        assertThat(kv(single())).containsEntry("userId", "u-7");
    }

    private ILoggingEvent single() {
        List<ILoggingEvent> list = appender.list;
        assertThat(list).hasSize(1);
        return list.get(0);
    }

    private Map<String, Object> kv(ILoggingEvent ev) {
        List<KeyValuePair> pairs = ev.getKeyValuePairs();
        if (pairs == null) {
            return Map.of();
        }
        return pairs.stream().collect(Collectors.toMap(p -> p.key, p -> p.value));
    }
}
