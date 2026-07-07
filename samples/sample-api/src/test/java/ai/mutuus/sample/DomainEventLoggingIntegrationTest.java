package ai.mutuus.sample;

import java.util.Map;
import java.util.Optional;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.event.DomainEventLogger;
import ai.mutuus.common.event.EventPublisher;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>도메인 이벤트 로깅</b>(Phase 3b) 통합 검증 — 무설정으로 {@link EventPublisher} 를 주입받아 이벤트를 발행하면,
 * 발행({@code domain_event.published})과 in-process 소비({@code domain_event.consumed})가 각각
 * {@code mutuus.domain_event} dataset 으로 남는지 확인한다({@link DomainEventLogger}).
 */
@SpringBootTest
class DomainEventLoggingIntegrationTest {

    @Autowired
    EventPublisher eventPublisher;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attach() {
        logger = (Logger) LoggerFactory.getLogger(DomainEventLogger.LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
        TraceContext.clear();
    }

    @Test
    void 발행과_소비가_domain_event_dataset으로_ECS로그를_남긴다() {
        TraceContext.put(HeaderNames.TRACE_ID, "evt-ecs-trace");

        eventPublisher.publish("test.ecs.event", Map.of("k", "v"));

        ILoggingEvent published = event("domain_event.published");
        assertThat(raw(published, "event.dataset")).isEqualTo("mutuus.domain_event");
        assertThat(raw(published, "data_stream.dataset")).isEqualTo("mutuus.domain_event");
        assertThat(raw(published, "mutuus.domain_event.type")).isEqualTo("test.ecs.event");
        assertThat(raw(published, "mutuus.domain_event.id")).isInstanceOf(String.class);
        assertThat(raw(published, "event")).isEqualTo("domain_event.published"); // legacy 병존

        ILoggingEvent consumed = event("domain_event.consumed");
        assertThat(raw(consumed, "event.dataset")).isEqualTo("mutuus.domain_event");
        assertThat(raw(consumed, "mutuus.domain_event.type")).isEqualTo("test.ecs.event");
    }

    private ILoggingEvent event(String action) {
        Optional<ILoggingEvent> ev = appender.list.stream()
                .filter(e -> action.equals(raw(e, "event.action")))
                .findFirst();
        assertThat(ev).as("event.action=%s 로그가 있어야 한다", action).isPresent();
        return ev.get();
    }

    private static Object raw(ILoggingEvent e, String key) {
        if (e.getKeyValuePairs() == null) {
            return null;
        }
        return e.getKeyValuePairs().stream()
                .filter(p -> key.equals(p.key))
                .map(p -> p.value)
                .findFirst().orElse(null);
    }
}
