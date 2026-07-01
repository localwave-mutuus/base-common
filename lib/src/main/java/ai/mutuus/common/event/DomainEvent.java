package ai.mutuus.common.event;

import java.time.Instant;
import java.util.UUID;

import ai.mutuus.common.core.TraceContext;

/**
 * 도메인 이벤트 봉투. 발행 시점의 추적 컨텍스트(traceId/userId)를 함께 실어, 이벤트가 스레드·프로세스
 * 경계를 넘어도 <b>어떤 요청에서 비롯됐는지</b>를 잃지 않게 한다(로그 상관·감사 일관성).
 *
 * <p>브로커 비의존 순수 DTO다. 라이브러리는 in-process 발행({@link ApplicationEventPublisherAdapter})을
 * 기본 제공하고, 소비 서비스는 같은 봉투를 Kafka/Rabbit 등으로 내보내는 {@link EventPublisher} 구현으로
 * 대체할 수 있다(봉투 규약은 유지).
 *
 * @param eventId    이벤트 고유 식별자(멱등 처리·중복 제거용)
 * @param type       이벤트 타입(예: {@code order.created})
 * @param occurredAt 발생 시각(UTC)
 * @param traceId    발행 시점의 추적 ID(없으면 null)
 * @param userId     발행 시점의 인증 사용자(없으면 null)
 * @param payload    이벤트 본문
 */
public record DomainEvent<T>(String eventId, String type, Instant occurredAt,
                             String traceId, String userId, T payload) {

    /**
     * 현재 {@link TraceContext}(traceId/userId)로 봉투를 채워 이벤트를 만든다. eventId 는 새로 발급하고
     * occurredAt 은 현재 시각으로 잡는다.
     */
    public static <T> DomainEvent<T> of(String type, T payload) {
        return new DomainEvent<>(UUID.randomUUID().toString(), type, Instant.now(),
                TraceContext.traceId(), TraceContext.userId(), payload);
    }
}
