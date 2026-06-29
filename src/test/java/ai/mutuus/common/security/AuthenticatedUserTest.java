package ai.mutuus.common.security;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void hasRole는_보유한_역할에_true를_반환한다() {
        var user = new AuthenticatedUser("u-1", "tenant-a", Set.of("ADMIN", "USER"));
        assertThat(user.hasRole("ADMIN")).isTrue();
        assertThat(user.hasRole("GUEST")).isFalse();
    }

    @Test
    void roles가_null이어도_안전하다() {
        var user = new AuthenticatedUser("u-1", "tenant-a", null);
        assertThat(user.hasRole("ADMIN")).isFalse();
    }
}
