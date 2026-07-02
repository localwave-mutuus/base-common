package ai.mutuus.common.security.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 보안 감사 로깅 설정. {@code mutuus.common.security.audit.*}.
 * <p>보안 위배는 기본적으로 기록해야 하므로 {@code enabled} 기본값은 <b>true</b> 다.
 */
@ConfigurationProperties(prefix = "mutuus.common.security.audit")
public class CommonSecurityAuditProperties {

    /** 보안 감사 로깅 활성화(기본 ON — 위배는 기본 기록). */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
