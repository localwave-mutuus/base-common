package ai.mutuus.common.event;

import ai.mutuus.common.core.LogFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
 * <p>{@code mutuus.common.event.logging-enabled=true}(기본) 면 발행/소비를 {@code mutuus.domain_event}
 * dataset 으로 로깅한다({@link DomainEventLogger} + {@link DomainEventLoggingListener}).
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mutuus.common.event", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(CommonEventProperties.class)
public class CommonEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    EventPublisher applicationEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher,
                                                    ObjectProvider<DomainEventLogger> domainEventLogger) {
        return new ApplicationEventPublisherAdapter(applicationEventPublisher, domainEventLogger.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mutuus.common.event", name = "logging-enabled", matchIfMissing = true)
    DomainEventLogger domainEventLogger(
            // 로그 포맷은 액세스 로깅과 동일 출처를 원시 프로퍼티로만 읽어 event→logging 패키지 결합을 피한다.
            @Value("${mutuus.common.logging.format:DUAL}") LogFormat format) {
        return new DomainEventLogger(format);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mutuus.common.event", name = "logging-enabled", matchIfMissing = true)
    DomainEventLoggingListener domainEventLoggingListener(DomainEventLogger logger) {
        return new DomainEventLoggingListener(logger);
    }
}
