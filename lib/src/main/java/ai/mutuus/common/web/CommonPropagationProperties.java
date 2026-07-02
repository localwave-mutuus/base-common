package ai.mutuus.common.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 아웃바운드 헤더 전파 설정. {@code mutuus.common.propagation.*}.
 */
@ConfigurationProperties(prefix = "mutuus.common.propagation")
public class CommonPropagationProperties {

    /**
     * 식별/민감 헤더({@code X-User-Id}·{@code X-Device-*}·{@code X-App-Code} 등)를 전파해도 되는 <b>신뢰 호스트</b> 목록.
     * <b>비어 있으면(기본) 모든 호스트에 전파</b>(하위호환) — 운영에서는 내부 도메인만 지정해 외부 유출을 막는 것을 권장한다.
     * 목록이 지정되면 그 외 호스트로의 호출엔 상관용(trace/span/locale)만 붙고 식별 헤더는 <b>제거</b>된다
     * ({@code security.propagation.blocked} 로 기록). 호스트 매칭은 대소문자 무시, 정확 일치 또는 {@code .suffix} 접미 일치.
     */
    private List<String> allowedHosts = new ArrayList<>();

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }
}
