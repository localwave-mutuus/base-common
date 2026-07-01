package ai.mutuus.common.payload;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 컨트롤러 입출력(요청/응답 본문) 로깅 설정. {@code mutuus.common.payload-logging.*}.
 * <p>본문에는 PII·민감정보가 들어갈 수 있고 비용도 크므로 <b>기본 비활성(opt-in)</b>이다.
 */
@ConfigurationProperties(prefix = "mutuus.common.payload-logging")
public class PayloadLoggingProperties {

    /** 입출력 본문 로깅 활성화 여부(기본 false — 명시적으로 켠다). */
    private boolean enabled = false;

    /** 로깅할 본문 최대 바이트(초과분은 절단). */
    private int maxBodyBytes = 2048;

    /** 본문을 로깅할 Content-Type 접두사. 이외(바이너리 등)는 본문을 생략한다. */
    private List<String> includeContentTypes = new ArrayList<>(List.of(
            "application/json", "application/xml", "application/x-www-form-urlencoded", "text/"));

    /** 로깅을 생략할 경로 접두사(헬스체크 등). */
    private List<String> excludePathPrefixes = new ArrayList<>(List.of("/actuator"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public List<String> getIncludeContentTypes() {
        return includeContentTypes;
    }

    public void setIncludeContentTypes(List<String> includeContentTypes) {
        this.includeContentTypes = includeContentTypes;
    }

    public List<String> getExcludePathPrefixes() {
        return excludePathPrefixes;
    }

    public void setExcludePathPrefixes(List<String> excludePathPrefixes) {
        this.excludePathPrefixes = excludePathPrefixes;
    }
}
