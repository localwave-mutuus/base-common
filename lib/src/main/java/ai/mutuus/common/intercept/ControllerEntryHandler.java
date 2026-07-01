package ai.mutuus.common.intercept;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * <b>대상 컨트롤러 메서드 진입 시 수행하는 동작</b>.
 * <p>타게팅({@link ControllerMethodMatcher})과 분리해, "무엇을 할지"만 독립적으로 갱신/교체한다.
 * 기본 구현({@link DefaultControllerEntryHandler})은 진입을 로깅하며, 소비 서비스가 자체 빈을
 * 정의하면 {@code @ConditionalOnMissingBean} 으로 대체된다(감사/메트릭/알림 등으로 확장).
 */
@FunctionalInterface
public interface ControllerEntryHandler {

    /**
     * 매처가 통과시킨 컨트롤러 메서드 진입 시점에 호출된다.
     *
     * @param handlerMethod 진입한 컨트롤러 메서드(클래스/메서드명/패키지)
     * @param request       현재 요청(URL/헤더 등)
     */
    void onEntry(HandlerMethod handlerMethod, HttpServletRequest request);
}
