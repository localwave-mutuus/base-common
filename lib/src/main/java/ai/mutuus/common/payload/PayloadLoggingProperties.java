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
    public static final int MAX_BODY_BYTES = 4096;

    private int maxBodyBytes = MAX_BODY_BYTES;

    /** 기존 enabled=true 동작은 유지하며 error-only는 명시적으로 선택한다. */
    private Mode mode = Mode.ALL;

    /** 이 목록에 명시된 요청 헤더만 기록한다. 인증/쿠키 헤더는 목록에 있어도 제외한다. */
    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Content-Type", "X-Trace-Id", "X-Span-Id", "traceparent"));

    public enum Mode { ALL, ERROR_ONLY }

    public Mode getMode() { return mode; }

    public void setMode(Mode mode) {
        if (mode == null) throw new IllegalArgumentException("payload logging mode must not be null");
        this.mode = mode;
    }

    public List<String> getAllowedHeaders() { return allowedHeaders; }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders == null ? new ArrayList<>() : new ArrayList<>(allowedHeaders);
    }

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
        if (maxBodyBytes < 1 || maxBodyBytes > MAX_BODY_BYTES) {
            throw new IllegalArgumentException("max-body-bytes must be between 1 and 4096");
        }
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
