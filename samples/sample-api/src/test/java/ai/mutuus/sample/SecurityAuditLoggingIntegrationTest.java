package ai.mutuus.sample;

import java.util.Map;
import java.util.Optional;

import ai.mutuus.common.core.HeaderNames;
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
 * <b>보안 감사 로깅</b>(Phase 0) 통합 테스트 — 인증/인가 위배 시 전용 로거({@link SecurityAuditLogger#LOGGER_NAME})로
 * 구조화 보안 이벤트가 남는지 실서버 + 로그 캡처(ListAppender)로 검증한다.
 * <ul>
 *   <li>토큰 없이 보호 리소스 → 401 + {@code security.authn.failed}(httpPath·clientIp)</li>
 *   <li>USER 토큰으로 ADMIN 리소스 → 403 + {@code security.authz.denied}(principal)</li>
 * </ul>
 * mock JwtDecoder({@code mutuus.sample.mock-jwt=true})는 {@code Bearer <name>} 의 name 을 주체(권한 USER)로 준다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "mutuus.sample.mock-jwt=true")
class SecurityAuditLoggingIntegrationTest {

    @LocalServerPort
    int port;

    private ListAppender<ILoggingEvent> appender;
    private Logger securityLogger;

    @BeforeEach
    void attachAppender() {
        securityLogger = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        securityLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        securityLogger.detachAppender(appender);
    }

    @Test
    void 토큰없이_보호리소스_접근시_401과_security_authn_failed_로그() {
        RestClient client = RestClient.create();
        int status = client.get().uri(base() + "/api/secure/whoami")
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(401);

        Optional<ILoggingEvent> ev = event("security.authn.failed");
        assertThat(ev).isPresent();
        assertThat(kv(ev.get(), "httpPath")).contains("/api/secure/whoami");
        assertThat(kv(ev.get(), "clientIp")).isPresent();
        assertThat(kv(ev.get(), "securityEvent")).contains("true");
    }

    @Test
    void USER토큰으로_ADMIN리소스_접근시_403과_security_authz_denied_로그() {
        RestClient client = RestClient.create();
        int status = client.get().uri(base() + "/api/admin/ping")
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice")
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(403);

        Optional<ILoggingEvent> ev = event("security.authz.denied");
        assertThat(ev).isPresent();
        assertThat(kv(ev.get(), "httpPath")).contains("/api/admin/ping");
        assertThat(kv(ev.get(), "principal")).contains("alice"); // 검증된 주체가 로그에 남는다
    }

    @Test
    void 유효토큰_whoami는_200이며_보안위배로그가_없다() {
        RestClient client = RestClient.create();
        int status = client.get().uri(base() + "/api/secure/whoami")
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice")
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(200);
        assertThat(event("security.authn.failed")).isEmpty();
        assertThat(event("security.authz.denied")).isEmpty();
    }

    @Test
    void 미인증_permitall에_X_User_Id_위장은_폐기되고_header_ignored_로그와_빈_감사주체() {
        RestClient client = RestClient.create();
        Map<?, ?> res = client.get().uri(base() + "/api/public/whoami")
                .header(HeaderNames.USER_ID, "spoofed-admin") // 위장 시도(토큰 없음)
                .retrieve().body(Map.class);

        // 감사 위조 방지: 위장값이 TraceContext(→ created_by)에 실리지 않아야 한다
        Map<?, ?> data = (Map<?, ?>) res.get("data");
        assertThat(data.get("traceContextUserId")).isEqualTo("");

        Optional<ILoggingEvent> ev = event("security.identity.header_ignored");
        assertThat(ev).isPresent();
        assertThat(kv(ev.get(), "clientIp")).isPresent();
    }

    @Test
    void 인증주체와_다른_X_User_Id_주장은_identity_mismatch_로그() {
        RestClient client = RestClient.create();
        int status = client.get().uri(base() + "/api/secure/whoami")
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice")
                .header(HeaderNames.USER_ID, "bob") // 검증 주체(alice)와 다른 주장
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(200);

        Optional<ILoggingEvent> ev = event("security.identity.mismatch");
        assertThat(ev).isPresent();
        assertThat(kv(ev.get(), "authenticatedUserId")).contains("alice");
    }

    @Test
    void 서버오류_500응답은_내부_예외클래스를_노출하지_않는다() {
        RestClient client = RestClient.create();
        Map<?, ?> body = client.get().uri(base() + "/api/secure/boom")
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice")
                .exchange((req, res) -> res.bodyTo(Map.class));

        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo("INTERNAL_ERROR");
        Map<?, ?> error = (Map<?, ?>) body.get("error");
        assertThat(error.get("exception")).isNull(); // 예외 클래스명 미노출
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
