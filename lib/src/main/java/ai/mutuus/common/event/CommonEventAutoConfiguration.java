package ai.mutuus.common.event;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * 공통 이벤트 발행 자동 구성. 브로커 비의존 코어 — 어떤 통합 스타터도 필요 없이 항상 사용 가능하다.
 * <p>{@code mutuus.common.event.enabled=false} 가 아니면 in-process 기본 발행자
 * ({@link ApplicationEventPublisherAdapter})를 등록한다. 소비 서비스가 자체 {@link EventPublisher}
 * (예: Kafka/Rabbit 발행자) 빈을 정의하면 {@code @ConditionalOnMissingBean} 으로 비켜선다 — 봉투
 * ({@link DomainEvent}) 규약은 그대로 유지된다.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mutuus.common.event", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(CommonEventProperties.class)
public class CommonEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    EventPublisher applicationEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        return new ApplicationEventPublisherAdapter(applicationEventPublisher);
    }
}
