package ai.mutuus.common.logging;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 액세스 로깅 설정. {@code mutuus.common.logging.*}.
 */
@ConfigurationProperties(prefix = "mutuus.common.logging")
public class CommonLoggingProperties {

    /** 액세스 로깅 전체 활성화 여부. */
    private boolean enabled = true;

    /** 요청 수신 로그에 쿼리스트링 포함 여부. */
    private boolean includeQueryString = true;

    /** 로깅을 생략할 경로 접두사(노이즈 감소 — 헬스체크 등). */
    private List<String> excludePathPrefixes = new ArrayList<>(List.of("/actuator"));

    /** 느린 요청 경고 임계값(ms). 0이면 비활성. 초과 시 request.completed 가 WARN 으로 기록된다. */
    private long slowRequestThresholdMillis = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeQueryString() {
        return includeQueryString;
    }

    public void setIncludeQueryString(boolean includeQueryString) {
        this.includeQueryString = includeQueryString;
    }

    public List<String> getExcludePathPrefixes() {
        return excludePathPrefixes;
    }

    public void setExcludePathPrefixes(List<String> excludePathPrefixes) {
        this.excludePathPrefixes = excludePathPrefixes;
    }

    public long getSlowRequestThresholdMillis() {
        return slowRequestThresholdMillis;
    }

    public void setSlowRequestThresholdMillis(long slowRequestThresholdMillis) {
        this.slowRequestThresholdMillis = slowRequestThresholdMillis;
    }
}
