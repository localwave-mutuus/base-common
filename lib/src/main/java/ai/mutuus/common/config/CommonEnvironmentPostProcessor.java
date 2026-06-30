package ai.mutuus.common.config;

import java.util.HashMap;
import java.util.Map;

import ai.mutuus.common.core.IdGenerator;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Spring Boot 컨텍스트 초기화 <b>이전</b>에 기본 설정값을 주입한다.
 * <p>auto-configuration 보다 먼저 동작하여 "convention over configuration" 기본값을
 * Environment 최하위 우선순위로 추가한다. 애플리케이션이 동일 키를 정의하면 그 값이 우선한다.
 * <p>등록: {@code META-INF/spring.factories} 의
 * {@code org.springframework.boot.EnvironmentPostProcessor}.
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

        // 어플리케이션코드(4)/인스턴스구분코드(6) 해석: 미지정 시 도출/생성, 지정 시 포맷 검증.
        // 컨텍스트·로깅 초기화 이전에 확정해 시스템 프로퍼티로 노출(로그 파일명/필드에 사용).
        resolveCodes(environment, defaults);

        // addLast → 최저 우선순위 (애플리케이션 설정이 항상 우선)
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }

    private void resolveCodes(ConfigurableEnvironment environment, Map<String, Object> defaults) {
        String serviceName = blankToNull(environment.getProperty("mutuus.common.service-name"));
        if (serviceName == null) {
            serviceName = blankToNull(environment.getProperty("spring.application.name"));
        }
        if (serviceName == null) {
            serviceName = "unknown-service";
        }

        String appCode = blankToNull(environment.getProperty("mutuus.common.app-code"));
        if (appCode == null) {
            appCode = IdGenerator.deriveAlnum(serviceName, 4); // 서비스명에서 결정적 도출
        } else if (!appCode.matches("[A-Za-z0-9]{4}")) {
            throw new IllegalStateException(
                    "mutuus.common.app-code 는 숫자/영문 4자리여야 합니다: '" + appCode + "'");
        } else {
            appCode = appCode.toUpperCase();
        }

        String instanceCode = blankToNull(environment.getProperty("mutuus.common.instance-code"));
        if (instanceCode == null) {
            instanceCode = IdGenerator.randomAlnum(6); // 미지정 → 구동 시 자동 생성
        } else if (!instanceCode.matches("[A-Za-z0-9]{6}")) {
            throw new IllegalStateException(
                    "mutuus.common.instance-code 는 숫자/영문 6자리여야 합니다: '" + instanceCode + "'");
        } else {
            instanceCode = instanceCode.toUpperCase();
        }

        // 해석된 최종값을 환경에 되돌려, 빈/프로퍼티가 동일 값을 보게 한다(미지정이었던 값 포함).
        defaults.put("mutuus.common.app-code", appCode);
        defaults.put("mutuus.common.instance-code", instanceCode);

        // 로깅 시스템 초기화 이전 시점이므로 시스템 프로퍼티로 노출 → logback ${...} 에서 사용.
        System.setProperty("mutuus.appCode", appCode);
        System.setProperty("mutuus.instanceCode", instanceCode);
        System.setProperty("mutuus.logFileBase", appCode + "-" + instanceCode);
        System.setProperty("SERVICE_NAME", serviceName);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    @Override
    public int getOrder() {
        // ConfigData(application.yml) 처리 이후, 다른 기본값보다 늦게
        return Ordered.LOWEST_PRECEDENCE;
    }
}
