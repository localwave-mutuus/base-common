package ai.mutuus.common.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Spring Boot 컨텍스트 초기화 <b>이전</b>에 기본 설정값을 주입한다.
 * <p>auto-configuration 보다 먼저 동작하여 "convention over configuration" 기본값을
 * Environment 최하위 우선순위로 추가한다. 애플리케이션이 동일 키를 정의하면 그 값이 우선한다.
 * <p>등록: {@code META-INF/spring.factories} 의
 * {@code org.springframework.boot.env.EnvironmentPostProcessor}.
 */
public class CommonEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE_NAME = "mutuusCommonDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        // 관측/추적 기본값
        defaults.put("management.tracing.sampling.probability", "1.0");
        defaults.put("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        // 다국어 기본값
        defaults.put("spring.messages.basename", "messages/messages");
        defaults.put("spring.messages.fallback-to-system-locale", "false");
        // 공통 모듈 기본값
        defaults.put("mutuus.common.tracing-enabled", "true");
        defaults.put("mutuus.common.default-locale", "ko-KR");

        // addLast → 최저 우선순위 (애플리케이션 설정이 항상 우선)
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        // ConfigData(application.yml) 처리 이후, 다른 기본값보다 늦게
        return Ordered.LOWEST_PRECEDENCE;
    }
}
