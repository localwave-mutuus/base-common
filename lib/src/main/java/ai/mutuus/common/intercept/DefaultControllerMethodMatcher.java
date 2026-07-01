package ai.mutuus.common.intercept;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link ControllerMethodMatcher} 기본 구현 — {@link ControllerEntryProperties}의
 * 패키지/메서드명/URL 규칙으로 판별한다. 비어 있는 축은 제약하지 않고, 지정된 축은 OR,
 * 축 간에는 AND 로 결합한다(셋 다 비면 모든 컨트롤러 매칭).
 */
public class DefaultControllerMethodMatcher implements ControllerMethodMatcher {

    private final ControllerEntryProperties props;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public DefaultControllerMethodMatcher(ControllerEntryProperties props) {
        this.props = props;
    }

    @Override
    public boolean matches(HandlerMethod handlerMethod, HttpServletRequest request) {
        // 세 축(패키지/메서드명/URL) "모두" 통과해야 대상 → 축 간 AND.
        // 각 축은 규칙이 없으면(빈 리스트) 무조건 통과(true)라 제약하지 않는다.
        return matchesPackage(handlerMethod)
                && matchesMethodName(handlerMethod)
                && matchesUrl(request);
    }

    private boolean matchesPackage(HandlerMethod handlerMethod) {
        List<String> packages = props.getPackages();
        if (packages.isEmpty()) {
            return true;   // 패키지 규칙 미설정 → 이 축은 제약 없음
        }
        // 컨트롤러 클래스의 FQN 이 지정 접두사 중 하나로 시작하면 통과(축 내 OR)
        String typeName = handlerMethod.getBeanType().getName();
        for (String prefix : packages) {
            if (typeName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesMethodName(HandlerMethod handlerMethod) {
        List<String> names = props.getMethodNames();
        if (names.isEmpty()) {
            return true;
        }
        // 메서드명이 패턴 중 하나에 매칭되면 통과. simpleMatch 는 * 와일드카드 지원(예: get*, *audit).
        String methodName = handlerMethod.getMethod().getName();
        for (String pattern : names) {
            if (PatternMatchUtils.simpleMatch(pattern, methodName)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesUrl(HttpServletRequest request) {
        List<String> patterns = props.getUrlPatterns();
        if (patterns.isEmpty()) {
            return true;
        }
        // 요청 URI 가 Ant 패턴(/api/**, /demo/audit 등) 중 하나에 매칭되면 통과
        String uri = request.getRequestURI();
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
