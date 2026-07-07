package ai.mutuus.sample;

import ai.mutuus.common.security.CommonSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that sample-api's {@code application-ai.yml} is not only syntactically
 * loadable, but also drives sample-owned behavior.
 *
 * <p>The test uses SpringBootTest's default MOCK web environment. Even though the
 * ai profile file declares {@code server.port=8095} by default, this test does not open a
 * server socket and therefore cannot collide with another local service port.
 */
@SpringBootTest
@ActiveProfiles("ai")
class AiProfilePropertiesTest {

    @Autowired
    Environment env;

    @Autowired
    ApplicationContext ctx;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    CommonSecurityProperties securityProperties;

    @Test
    void ai_profile_file_enables_sample_mock_jwt_decoder_and_audience() {
        assertThat(env.getActiveProfiles()).containsExactly("ai");
        assertThat(env.getProperty("demo.environment")).isEqualTo("ai");
        assertThat(env.getProperty("server.port", Integer.class)).isEqualTo(8095);
        assertThat(env.getProperty("sample.ports.app", Integer.class)).isEqualTo(8090);
        assertThat(env.getProperty("sample.ports.test-range")).isEqualTo("8095-8099");
        assertThat(env.getProperty("mutuus.sample.mock-jwt", Boolean.class)).isTrue();
        assertThat(env.getProperty("mutuus.common.security.headers.enabled", Boolean.class)).isTrue();
        assertThat(securityProperties.getAudiences()).containsExactly("sample-api");

        assertThat(ctx.containsBean("demoJwtDecoder")).isTrue();
        assertThat(jwtDecoder.decode("alice").getSubject()).isEqualTo("alice");
        assertThat(jwtDecoder.decode("alice").getAudience()).containsExactly("sample-api");
    }
}
