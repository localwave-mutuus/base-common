package ai.mutuus.common.response;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 성공 응답 자동 래핑 설정. {@code mutuus.common.response-wrapper.*}.
 * <p><b>기본 비활성(opt-in)</b> — 자동 래핑은 소비 서비스의 응답 규약을 바꾸는 오지랖이 될 수 있어
 * 명시적으로 켤 때만 동작한다.
 */
@ConfigurationProperties(prefix = "mutuus.common.response-wrapper")
public class ResponseWrapperProperties {

    /** 성공 응답 자동 래핑 활성화 여부(기본 false). */
    private boolean enabled = false;

    /** 래핑을 생략할 경로 접두사 — springdoc(OpenAPI JSON)·actuator·swagger 등 프레임워크 응답을 보호. */
    private List<String> excludePathPrefixes = new ArrayList<>(List.of(
            "/actuator", "/v3/api-docs", "/swagger-ui"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePathPrefixes() {
        return excludePathPrefixes;
    }

    public void setExcludePathPrefixes(List<String> excludePathPrefixes) {
        this.excludePathPrefixes = excludePathPrefixes;
    }
}
