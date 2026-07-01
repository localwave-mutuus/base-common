package ai.mutuus.common.idempotency;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 멱등성(Idempotency-Key) 설정. {@code mutuus.common.idempotency.*}. <b>기본 OFF(opt-in)</b>.
 * <p>켜면 지정 메서드(기본 POST/PUT/PATCH) 요청에 {@code Idempotency-Key} 헤더가 있을 때, 같은 키의
 * 중복 요청을 재처리하지 않고 첫 응답을 그대로 재방(replay)한다. 헤더가 없으면 평소대로 동작한다.
 */
@ConfigurationProperties(prefix = "mutuus.common.idempotency")
public class IdempotencyProperties {

    /** 멱등성 처리 활성화 여부(기본 false). */
    private boolean enabled = false;

    /** 멱등 키 헤더명. */
    private String headerName = "Idempotency-Key";

    /** 저장 TTL(이 기간 내 같은 키는 첫 응답 재방). */
    private Duration ttl = Duration.ofHours(24);

    /** 대상 HTTP 메서드(대문자). */
    private List<String> methods = new ArrayList<>(List.of("POST", "PUT", "PATCH"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }
}
