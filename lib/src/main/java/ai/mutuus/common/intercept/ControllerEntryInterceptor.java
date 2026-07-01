package ai.mutuus.common.intercept;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 컨트롤러 메서드 진입 인터셉터(오케스트레이터).
 * <p>{@code preHandle}(= 컨트롤러 메서드 실행 직전)에서, 핸들러가 컨트롤러 메서드이고
 * {@link ControllerMethodMatcher} 가 대상으로 판정하면 {@link ControllerEntryHandler}를 호출한다.
 * 절대 요청을 막지 않는다(항상 {@code true} 반환). 타게팅/동작은 각각 주입된 별도 빈이라 독립 교체 가능.
 */
public class ControllerEntryInterceptor implements HandlerInterceptor {

    private final ControllerMethodMatcher matcher;
    private final ControllerEntryHandler handler;

    public ControllerEntryInterceptor(ControllerMethodMatcher matcher, ControllerEntryHandler handler) {
        this.matcher = matcher;
        this.handler = handler;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handlerObject) {
        // handler 가 컨트롤러 메서드일 때만(정적 리소스/에러 핸들러 등은 HandlerMethod 가 아님) &&
        // 매처가 대상으로 판정하면 진입 동작 실행. 판별/동작은 각각 주입된 별도 빈이라 독립 교체된다.
        if (handlerObject instanceof HandlerMethod handlerMethod && matcher.matches(handlerMethod, request)) {
            handler.onEntry(handlerMethod, request);
        }
        // 항상 true — 이 인터셉터는 관측 목적이라 요청 진행을 절대 막지 않는다.
        return true;
    }
}
