package ai.mutuus.common.event;

import org.springframework.context.event.EventListener;

/**
 * in-process 로 전달되는 {@link DomainEvent} 를 받아 소비({@code domain_event.consumed}) 로그를 남긴다.
 * <p>{@code mutuus.common.event.logging-enabled=true}(기본) 일 때만 등록된다. 발행 로그는
 * {@link ApplicationEventPublisherAdapter} 가, 소비 로그는 이 리스너가 담당한다({@link DomainEventLogger}).
 * 동기 전달이면 발행 스레드와 같아 {@code trace.id}(MDC) 상관이 유지된다.
 */
public class DomainEventLoggingListener {

    private final DomainEventLogger logger;

    public DomainEventLoggingListener(DomainEventLogger logger) {
        this.logger = logger;
    }

    @EventListener
    public void onDomainEvent(DomainEvent<?> event) {
        logger.consumed(event);
    }
}
