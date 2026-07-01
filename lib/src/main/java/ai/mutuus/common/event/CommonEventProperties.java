package ai.mutuus.common.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 이벤트 설정. {@code mutuus.common.event.*}. 기본 ON(in-process 기본 발행자 제공).
 * 소비 서비스가 자체 {@link EventPublisher} 빈을 정의하면 그 값이 우선한다.
 */
@ConfigurationProperties(prefix = "mutuus.common.event")
public class CommonEventProperties {

    /** 공통 이벤트 발행자 자동 등록 여부(기본 true). */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
