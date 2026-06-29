package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
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
 * 별도 설정 없이 라이브러리의 {@code HeaderPropagationInterceptor}를 통해 아웃바운드 호출에
 * 추적 헤더를 자동 전파하는지 실서버(RANDOM_PORT)로 검증한다.
 * <p>{@code X-Span-Id}는 전파하지 않고 호출 구간마다 새로 발급되는 것도 확인한다.
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
    void RestClient_호출에_추적헤더가_자동_전파되고_span은_새로_발급된다() {
        TraceContext.put(HeaderNames.TRACE_ID, "t-out");
        TraceContext.put(HeaderNames.USER_ID, "u-out");
        TraceContext.put(HeaderNames.SPAN_ID, "s-old");   // 전파되면 안 되는 값(새 span으로 대체되어야 함)

        RestClient client = restClientBuilder.baseUrl("http://localhost:" + port).build();
        Map<String, Object> envelope = client.get().uri("/api/public/echo-headers")
                .retrieve().body(Map.class);

        assertThat(envelope).isNotNull();
        Map<String, Object> echoed = (Map<String, Object>) envelope.get("data");
        assertThat(echoed.get("traceId")).isEqualTo("t-out");
        assertThat(echoed.get("userId")).isEqualTo("u-out");
        // span은 새로 발급 → 존재하고, 입력값/누락("null")이 아니어야 한다.
        assertThat(echoed.get("spanId")).isNotNull().isNotEqualTo("s-old").isNotEqualTo("null");
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
