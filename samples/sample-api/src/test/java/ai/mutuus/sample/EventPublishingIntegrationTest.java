package ai.mutuus.sample;

import java.util.Map;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.event.ApplicationEventPublisherAdapter;
import ai.mutuus.common.event.DomainEvent;
import ai.mutuus.common.event.EventPublisher;
import ai.mutuus.sample.demo.DemoEventRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이벤트 코어(broker-agnostic) 소비 검증 — 소비 서비스가 무설정으로 {@link EventPublisher} 를 주입받아
 * 봉투를 발행하면, 봉투에 추적 컨텍스트가 실리고 in-process {@code @EventListener}({@link DemoEventRecorder})
 * 가 같은 봉투를 받는지 확인한다. 기본 발행자가 {@link ApplicationEventPublisherAdapter} 인지도 본다.
 */
@SpringBootTest
class EventPublishingIntegrationTest {

    @Autowired
    EventPublisher eventPublisher;

    @Autowired
    DemoEventRecorder recorder;

    @AfterEach
    void clear() {
        TraceContext.clear();
    }

    @Test
    void 기본_발행자는_in_process_ApplicationEventPublisherAdapter다() {
        assertThat(eventPublisher).isInstanceOf(ApplicationEventPublisherAdapter.class);
    }

    @Test
    void 발행하면_리스너가_같은_봉투를_받고_추적ID가_실린다() {
        TraceContext.put(HeaderNames.TRACE_ID, "evt-trace");

        DomainEvent<?> published = eventPublisher.publish("test.event", Map.of("k", "v"));

        DomainEvent<?> received = recorder.last(); // in-process 동기 전달 → 이미 수신됨
        assertThat(received).isNotNull();
        assertThat(received.eventId()).isEqualTo(published.eventId());
        assertThat(received.type()).isEqualTo("test.event");
        assertThat(received.traceId()).isEqualTo("evt-trace");
        assertThat(received.payload()).isEqualTo(Map.of("k", "v"));
    }
}
