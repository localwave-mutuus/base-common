package ai.mutuus.sample;

import java.util.Optional;

import ai.mutuus.common.security.audit.SecurityAuditLogger;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>JWT audience 하드닝</b>(Phase 1-②) 통합 테스트 — {@code mutuus.common.security.audiences} 설정 시 허용되지 않은
 * aud 토큰이 거부되고 {@code security.jwt.rejected} 로 기록되는지 검증한다. mock JwtDecoder 는 {@code <name>#<aud>}
 * 형식으로 aud 를 실어 audience 불일치를 시연한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "mutuus.sample.mock-jwt=true",
        "mutuus.common.security.audiences=sample-api"
})
class SecurityJwtAudienceIntegrationTest {

    @LocalServerPort
    int port;

    private ListAppender<ILoggingEvent> appender;
    private Logger securityLogger;

    @BeforeEach
    void attach() {
        securityLogger = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        securityLogger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        securityLogger.detachAppender(appender);
    }

    @Test
    void 허용된_audience_토큰은_200() {
        int status = RestClient.create().get().uri(base() + "/api/secure/whoami")
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice") // aud = 설정된 sample-api → 통과
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(200);
        assertThat(event("security.jwt.rejected")).isEmpty();
    }

    @Test
    void 허용되지_않은_audience_토큰은_401과_security_jwt_rejected() {
        int status = RestClient.create().get().uri(base() + "/api/secure/whoami")
                // 구분자 ~ 는 Bearer 토큰 허용문자라 필터 malformed 가 아닌 실제 audience 검증으로 거부된다
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice~other-service") // aud = other-service → 거부
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(401);

        Optional<ILoggingEvent> ev = event("security.jwt.rejected");
        assertThat(ev).isPresent();
        assertThat(kv(ev.get(), "errorCode")).contains("invalid_token");
        assertThat(kv(ev.get(), "reason")).hasValueSatisfying(r -> assertThat(r).containsIgnoringCase("audience"));
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private Optional<ILoggingEvent> event(String name) {
        return appender.list.stream()
                .filter(e -> kv(e, "event").map(name::equals).orElse(false))
                .findFirst();
    }

    private static Optional<String> kv(ILoggingEvent e, String key) {
        if (e.getKeyValuePairs() == null) {
            return Optional.empty();
        }
        return e.getKeyValuePairs().stream()
                .filter(p -> key.equals(p.key))
                .map(p -> String.valueOf(p.value))
                .findFirst();
    }
}
