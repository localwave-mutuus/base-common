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
}
