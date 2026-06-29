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
}
