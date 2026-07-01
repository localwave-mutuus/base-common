package ai.mutuus.common.event;

import org.springframework.context.ApplicationEventPublisher;

/**
 * {@link EventPublisher} 의 in-process 기본 구현. Spring 의 {@link ApplicationEventPublisher} 로 위임해
 * 같은 컨텍스트 안의 {@code @EventListener}/{@code @TransactionalEventListener} 에게 이벤트를 전달한다.
 * <p>외부 브로커가 필요하면 소비 서비스가 {@link EventPublisher} 를 구현한 빈으로 대체한다
 * ({@code @ConditionalOnMissingBean}).
 */
public class ApplicationEventPublisherAdapter implements EventPublisher {

    private final ApplicationEventPublisher delegate;

    public ApplicationEventPublisherAdapter(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent<?> event) {
        // DomainEvent 는 ApplicationEvent 가 아니므로 Spring 이 PayloadApplicationEvent 로 감싸 전달한다.
        // 리스너는 파라미터 타입 DomainEvent 로 구독하면 봉투를 그대로 받는다.
        delegate.publishEvent(event);
    }
}
