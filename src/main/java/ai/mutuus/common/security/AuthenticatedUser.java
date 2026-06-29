package ai.mutuus.common.security;

import java.util.Set;

/**
 * 인증된 사용자 표현. 별도 인증/인가 MSA가 발급한 JWT 클레임으로부터 매핑된다.
 *
 * @param userId  사용자 식별자 (sub 또는 커스텀 클레임)
 * @param tenant  테넌트/조직 식별자
 * @param roles   권한(역할) 집합
 */
public record AuthenticatedUser(String userId, String tenant, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
