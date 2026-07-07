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
    private final DomainEventLogger logger;

    public ApplicationEventPublisherAdapter(ApplicationEventPublisher delegate) {
        this(delegate, null);
    }

    /** {@code logger} 가 있으면 발행 시 {@code domain_event.published} 로그를 남긴다(null 이면 무로깅). */
    public ApplicationEventPublisherAdapter(ApplicationEventPublisher delegate, DomainEventLogger logger) {
        this.delegate = delegate;
        this.logger = logger;
    }

    @Override
    public void publish(DomainEvent<?> event) {
        if (logger != null) {
            logger.published(event);
        }
        // DomainEvent 는 ApplicationEvent 가 아니므로 Spring 이 PayloadApplicationEvent 로 감싸 전달한다.
        // 리스너는 파라미터 타입 DomainEvent 로 구독하면 봉투를 그대로 받는다.
        delegate.publishEvent(event);
    }
}
