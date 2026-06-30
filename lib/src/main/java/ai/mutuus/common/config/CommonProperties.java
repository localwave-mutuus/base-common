package ai.mutuus.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 모듈 전역 설정 프로퍼티.
 * <p>{@code mutuus.common.*} 네임스페이스로 바인딩된다.
 */
@ConfigurationProperties(prefix = "mutuus.common")
public class CommonProperties {

    /** 서비스(애플리케이션) 논리 이름 — 로그/추적 태깅에 사용. */
    private String serviceName = "unknown-service";

    /** 기본 로케일 (다국어 폴백). */
    private String defaultLocale = "ko-KR";

    /** 추적/헤더 전파 활성화 여부. */
    private boolean tracingEnabled = true;

    /**
     * 어플리케이션코드 — 숫자/영문 4자리. 미지정 시 {@code service-name} 에서 결정적으로 도출된다.
     * 응답/아웃바운드 헤더(X-App-Code)와 로그 파일명에 사용. 해석은 {@code CommonEnvironmentPostProcessor}.
     */
    private String appCode;

    /**
     * 인스턴스구분코드 — 숫자/영문 6자리. 미지정 시 구동 시 자동 생성된다.
     * 응답/아웃바운드 헤더(X-Instance-Id)와 로그 파일명에 사용.
     */
    private String instanceCode;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    public void setTracingEnabled(boolean tracingEnabled) {
        this.tracingEnabled = tracingEnabled;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }
}
