package ai.mutuus.common.event;

/**
 * 도메인 이벤트 발행자. 라이브러리는 in-process 기본 구현({@link ApplicationEventPublisherAdapter})을
 * 제공하며, <b>브로커(Kafka/Rabbit 등)로 내보내려면 소비 서비스가 이 인터페이스를 구현한 빈으로
 * 대체</b>한다({@code @ConditionalOnMissingBean}). 어느 경우든 이벤트 봉투({@link DomainEvent}) 규약은
 * 동일하게 유지된다.
 */
public interface EventPublisher {

    /** 완성된 이벤트 봉투를 발행한다. */
    void publish(DomainEvent<?> event);

    /**
     * 타입/본문으로 봉투를 만들어 발행한다({@link DomainEvent#of} 로 추적 컨텍스트 자동 주입).
     * @return 발행된 봉투(eventId 등 확인용)
     */
    default DomainEvent<?> publish(String type, Object payload) {
        DomainEvent<?> event = DomainEvent.of(type, payload);
        publish(event);
        return event;
    }
}
