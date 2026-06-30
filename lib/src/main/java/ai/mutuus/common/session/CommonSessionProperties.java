package ai.mutuus.common.session;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 분산 세션 설정. {@code mutuus.common.session.*} 네임스페이스.
 */
@ConfigurationProperties(prefix = "mutuus.common.session")
public class CommonSessionProperties {

    /** 공통 세션 컨벤션(네임스페이스/타임아웃 자동 적용) 활성화 여부. */
    private boolean enabled = true;

    /** Redis 키 네임스페이스. 미지정 시 {@code <service-name>:session} 을 사용. */
    private String namespace;

    /** 세션 최대 비활성 유지 시간. */
    private Duration timeout = Duration.ofMinutes(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
