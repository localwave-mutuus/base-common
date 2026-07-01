package ai.mutuus.common.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 OpenAPI 설정. {@code mutuus.common.openapi.*}.
 * <p>소비자가 springdoc 를 classpath 에 추가하면 활성(기본 on), 자체 {@code OpenAPI} 빈을 정의하면 비켜선다.
 */
@ConfigurationProperties(prefix = "mutuus.common.openapi")
public class CommonOpenApiProperties {

    /** 공통 OpenAPI 빈 제공 여부(springdoc 존재 시 기본 활성). */
    private boolean enabled = true;

    /** 문서 제목. 비우면 {@code <service-name> API}. */
    private String title;

    /** 문서 설명. */
    private String description = "common-platform 기반 API";

    /** 문서 버전. */
    private String version = "v1";

    /** Bearer JWT 보안 스킴 등록 여부(자원 서버 인증과 정합 — Swagger UI 의 Authorize). */
    private boolean bearerJwt = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isBearerJwt() {
        return bearerJwt;
    }

    public void setBearerJwt(boolean bearerJwt) {
        this.bearerJwt = bearerJwt;
    }
}
