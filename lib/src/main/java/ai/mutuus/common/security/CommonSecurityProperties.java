package ai.mutuus.common.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 보안 설정. {@code mutuus.common.security.*}.
 */
@ConfigurationProperties(prefix = "mutuus.common.security")
public class CommonSecurityProperties {

    /** 인증 없이 접근 허용하는 경로 패턴. */
    private List<String> permitAll = new ArrayList<>(List.of(
            "/actuator/health/**", "/actuator/info", "/error"));

    /** JWT 권한(roles) 클레임 이름. */
    private String rolesClaim = "roles";

    /** 권한 접두사 (Spring Security 관례). */
    private String authorityPrefix = "ROLE_";

    /**
     * 인입 {@code X-User-Id} 헤더를 <b>신뢰</b>할지. <b>기본 false</b>(위장 가능한 헤더로 감사/로그를 오염시키지
     * 않음 — 사용자 식별은 검증된 인증 주체에서만 파생). 게이트웨이가 내부 신뢰망에서 이 헤더를 세팅하는
     * 배치라면 true 로 켠다. false 여도 인증 주체는 {@code AuthenticatedUserContextFilter} 가 채운다.
     */
    private boolean trustForwardedUser = false;

    /**
     * 허용 audience(aud) 목록. 비어 있으면 audience 검증 안 함(기본). 하나라도 지정하면 JWT 의 aud 에 이 중
     * 하나가 포함돼야 하며(그 외 거부), 이는 <b>다른 서비스용으로 발급된 같은 발급자 토큰</b>을 막는 하드닝이다.
     */
    private List<String> audiences = new ArrayList<>();

    /** 기대 issuer(iss). 비어 있으면 issuer 검증은 Boot(issuer-uri 설정 시)에 위임. 지정 시 이 값과 일치해야 한다. */
    private String issuer = "";

    public List<String> getPermitAll() {
        return permitAll;
    }

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll;
    }

    public String getRolesClaim() {
        return rolesClaim;
    }

    public void setRolesClaim(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    public String getAuthorityPrefix() {
        return authorityPrefix;
    }

    public void setAuthorityPrefix(String authorityPrefix) {
        this.authorityPrefix = authorityPrefix;
    }

    public boolean isTrustForwardedUser() {
        return trustForwardedUser;
    }

    public void setTrustForwardedUser(boolean trustForwardedUser) {
        this.trustForwardedUser = trustForwardedUser;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
