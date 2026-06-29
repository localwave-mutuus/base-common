package ai.mutuus.common.async;

import org.springframework.core.task.TaskDecorator;

/**
 * Spring 비동기 실행기에 추적 컨텍스트 전파를 적용하는 {@link TaskDecorator}.
 * <p>Spring Boot 의 task executor 자동구성은 컨텍스트에 단일 {@code TaskDecorator} 빈이 있으면
 * 이를 실행기에 자동 적용한다. 따라서 이 빈만 등록해 두면 {@code @Async} 기본 실행기로
 * 넘어가는 작업에도 추적ID/사용자/MDC 가 유지된다.
 */
public class TraceContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return TraceContextPropagation.wrap(runnable);
    }
}
