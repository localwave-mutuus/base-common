package ai.mutuus.common.config;

import java.util.HashMap;
import java.util.Map;

import ai.mutuus.common.core.IdGenerator;
import ai.mutuus.common.core.LogFilePolicy;
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
        // HTTP 클라이언트 기본 타임아웃(아웃바운드 호출 무한 대기 방지) — Boot 가 자동구성한
        // RestClient/RestTemplate 빌더에 적용된다. 소비자는 spring.http.client.* 로 재정의.
        defaults.put("spring.http.client.connect-timeout", "2s");
        defaults.put("spring.http.client.read-timeout", "10s");
        // 공통 모듈 기본값
        defaults.put("mutuus.common.tracing-enabled", "true");
        defaults.put("mutuus.common.default-locale", "ko-KR");

        // 어플리케이션코드(4)/인스턴스구분코드(6) 해석: 미지정 시 도출/생성, 지정 시 포맷 검증.
        // 컨텍스트·로깅 초기화 이전에 확정해 시스템 프로퍼티로 노출(로그 파일명/필드에 사용).
        resolveCodes(environment, defaults);
        resolveFilePolicy(environment);

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

        // 배포 환경/네임스페이스 해석(ECS service.environment / data_stream.namespace 의 근거).
        // 미지정 시 활성 프로파일 첫 값, 그것도 없으면 local. namespace 미지정 시 environment 로 대체.
        String environmentName = blankToNull(environment.getProperty("mutuus.common.logging.environment"));
        if (environmentName == null) {
            String[] profiles = environment.getActiveProfiles();
            environmentName = profiles.length > 0 ? profiles[0] : "local";
        }
        String namespace = blankToNull(environment.getProperty("mutuus.common.logging.data-stream.namespace"));
        if (namespace == null) {
            namespace = environmentName;
        }

        // 로깅 시스템 초기화 이전 시점이므로 시스템 프로퍼티로 노출 → logback ${...} 에서 사용.
        System.setProperty("mutuus.appCode", appCode);
        System.setProperty("mutuus.instanceCode", instanceCode);
        String shortName = blankToNull(environment.getProperty("mutuus.common.service-short-name"));
        if (shortName == null) shortName = "app";
        if (!shortName.matches("app|member|bo|batch")) throw new IllegalStateException("service-short-name: app/member/bo/batch만 허용");
        String fileEnvironment = environmentName.matches("local|dev|prod") ? environmentName : "local";
        String serial = blankToNull(environment.getProperty("mutuus.common.logging.serial"));
        if (serial == null) serial = blankToNull(environment.getProperty("GS_LOG_SERIAL"));
        if (serial == null && !fileEnvironment.equals("local")) throw new IllegalStateException("DEV/PROD logging.serial owner 주입 필요");
        if (serial == null) serial = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        if (!serial.matches("[0-9a-z]{8}")) throw new IllegalStateException("logging.serial: 소문자 영숫자 8자리 필요");
        defaults.put("mutuus.common.service-short-name", shortName);
        defaults.put("mutuus.common.logging.serial", serial);
        String directory = blankToNull(environment.getProperty("mutuus.common.logging.directory"));
        if (directory != null) System.setProperty("mutuus.logDirectory", directory);
        else System.clearProperty("mutuus.logDirectory");
        System.setProperty("mutuus.logFileBase", fileEnvironment + "-" + shortName + "-" + serial + "-" + ProcessHandle.current().pid());
        System.setProperty("SERVICE_NAME", serviceName);
        System.setProperty("mutuus.environment", environmentName);
        System.setProperty("mutuus.dataStreamNamespace", namespace);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private void resolveFilePolicy(ConfigurableEnvironment environment) {
        String interval = fileValue(environment, "GS_LOG_ROLL_INTERVAL", "roll-interval",
                environment.getProperty("mutuus.log.roll-interval", LogFilePolicy.DEFAULT_ROLL_INTERVAL));
        String size = fileValue(environment, "GS_LOG_MAX_FILE_SIZE", "max-file-size", LogFilePolicy.DEFAULT_MAX_FILE_SIZE);
        String normalizedInterval = LogFilePolicy.rollInterval(interval).toString();
        long bytes = LogFilePolicy.maxFileSize(size);
        // env가 프로젝트 YAML보다 우선하며 바인딩과 Logback도 같은 확정값을 사용한다.
        environment.getPropertySources().addFirst(new MapPropertySource("mutuusLogFilePolicy", Map.of(
                "mutuus.common.logging.file.roll-interval", normalizedInterval,
                "mutuus.common.logging.file.max-file-size", bytes + "B")));
        System.setProperty("mutuus.logRollInterval", normalizedInterval);
        System.setProperty("mutuus.logMaxFileSize", bytes + "");
    }

    private String fileValue(ConfigurableEnvironment environment, String env, String key, String fallback) {
        String override = environment.getProperty(env);
        if (override != null) return override;
        return environment.getProperty("mutuus.common.logging.file." + key, fallback);
    }

    @Override
    public int getOrder() {
        // ConfigData(application.yml) 처리 이후, 다른 기본값보다 늦게
        return Ordered.LOWEST_PRECEDENCE;
    }
}
