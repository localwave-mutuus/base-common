package ai.mutuus.common.intercept;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * <b>대상 컨트롤러 메서드 판별 함수</b> — 명칭/패키지/URL 규칙으로 "이 진입을 인터셉트할지" 결정한다.
 * <p>진입 동작({@link ControllerEntryHandler})과 분리해, 타게팅 정책만 독립적으로 갱신/교체한다.
 * 소비 서비스가 자체 빈을 정의하면 {@code @ConditionalOnMissingBean} 으로 기본 구현
 * ({@link DefaultControllerMethodMatcher})을 대체한다.
 */
@FunctionalInterface
public interface ControllerMethodMatcher {

    /** 이 요청/핸들러 메서드가 인터셉트 대상이면 true. */
    boolean matches(HandlerMethod handlerMethod, HttpServletRequest request);
}
