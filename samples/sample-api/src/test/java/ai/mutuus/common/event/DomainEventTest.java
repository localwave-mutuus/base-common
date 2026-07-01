package ai.mutuus.common.event;

import java.util.Map;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DomainEvent#of} 단위 테스트 — 봉투가 현재 {@link TraceContext}(traceId/userId)로 채워지고
 * eventId/occurredAt 이 발급되는지 검증한다. 라이브러리와 같은 패키지(split-package).
 */
class DomainEventTest {

    @AfterEach
    void clear() {
        TraceContext.clear();
    }

    @Test
    void of는_현재_TraceContext로_봉투를_채운다() {
        TraceContext.replace(Map.of(HeaderNames.TRACE_ID, "t-123", HeaderNames.USER_ID, "u-1"));

        DomainEvent<Map<String, Object>> event = DomainEvent.of("order.created", Map.of("id", 7));

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.type()).isEqualTo("order.created");
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.traceId()).isEqualTo("t-123");
        assertThat(event.userId()).isEqualTo("u-1");
        assertThat(event.payload()).isEqualTo(Map.of("id", 7));
    }

    @Test
    void 추적_컨텍스트가_없으면_traceId_userId는_null이다() {
        TraceContext.clear();

        DomainEvent<String> event = DomainEvent.of("noop", "x");

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.traceId()).isNull();
        assertThat(event.userId()).isNull();
    }

    @Test
    void eventId는_발행마다_고유하다() {
        DomainEvent<String> a = DomainEvent.of("t", "x");
        DomainEvent<String> b = DomainEvent.of("t", "x");
        assertThat(a.eventId()).isNotEqualTo(b.eventId());
    }
}
