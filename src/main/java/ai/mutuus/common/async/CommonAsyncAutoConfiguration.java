package ai.mutuus.common.async;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

/**
 * 비동기/가상스레드 추적 컨텍스트 전파 자동 구성.
 * <p>{@link TraceContextTaskDecorator} 빈을 등록하면 Spring Boot 의 task executor 자동구성이
 * 이를 자동 적용하여, {@code @Async} 기본 실행기로 넘어가는 작업에도 추적 컨텍스트가 유지된다.
 * 소비 서비스가 자체 {@code TaskDecorator} 를 정의하면 그 값이 우선한다
 * ({@code @ConditionalOnMissingBean}).
 * <p>추적 자체를 끄려면 {@code mutuus.common.tracing-enabled=false} 로 비활성화한다
 * (웹 추적 자동구성과 동일한 스위치).
 */
@AutoConfiguration
@ConditionalOnClass(TaskDecorator.class)
@ConditionalOnProperty(prefix = "mutuus.common", name = "tracing-enabled", matchIfMissing = true)
public class CommonAsyncAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskDecorator traceContextTaskDecorator() {
        return new TraceContextTaskDecorator();
    }
}
