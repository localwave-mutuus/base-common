package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.logging.TelemetryTestEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소비 서비스가 주입받는 {@code RestClient.Builder}(Boot 자동구성)로 만든 클라이언트가,
 * 라이브러리 인터셉터가 agent의 HTTP span/전파 소유권과 경쟁하지 않는지 실서버로 검증한다.
 * <p>이 JVM에는 agent가 없으므로 현재 SDK span이 있어도 HTTP trace/user 헤더를 직접 만들지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(OutboundPropagationIntegrationTest.TestSecurityConfig.class)
class OutboundPropagationIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder restClientBuilder;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void RestClient_인터셉터가_agent없이_추적이나_사용자헤더를_발명하지_않는다() {
        try (var span = TelemetryTestEvents.span("outbound-owner");
             var identity = TelemetryTestEvents.identity("u-out", "member")) {

            RestClient client = restClientBuilder.baseUrl("http://localhost:" + port).build();
            Map<String, Object> envelope = client.get().uri("/api/public/echo-headers")
                    .retrieve().body(Map.class);

            assertThat(envelope).isNotNull();
            Map<String, Object> echoed = (Map<String, Object>) envelope.get("data");
            assertThat(echoed.get("traceId")).isEqualTo("null");
            assertThat(echoed.get("userId")).isEqualTo("null");
            assertThat(echoed.get("spanId")).isEqualTo("null");
            assertThat(TraceContext.traceId()).isEqualTo(span.traceId());
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!"valid-token".equals(token)) {
                    throw new BadJwtException("invalid token");
                }
                return Jwt.withTokenValue(token).header("alg", "none").subject("user-1")
                        .claim("roles", List.of("USER"))
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            };
        }
    }
}
