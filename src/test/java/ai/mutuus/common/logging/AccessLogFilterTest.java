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
    }

    @Test
    void 정상요청은_received와_completed를_순서대로_남긴다() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/orders");
        request.setQueryString("page=1");
        var response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, new MockFilterChain());

        List<String> events = events();
        assertThat(events).containsExactly("request.received", "request.completed");
        assertThat(kv(appender.list.get(1)))
                .containsEntry("httpStatus", 200)
                .containsKey("durationMs");
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

        assertThat(events()).containsExactly("request.received", "request.completed");
    }

    private List<String> events() {
        return appender.list.stream()
                .map(e -> String.valueOf(kv(e).get("event")))
                .toList();
    }

    private Map<String, Object> kv(ILoggingEvent ev) {
        List<KeyValuePair> pairs = ev.getKeyValuePairs();
        if (pairs == null) {
            return Map.of();
        }
        return pairs.stream().collect(Collectors.toMap(p -> p.key, p -> p.value));
    }
}
