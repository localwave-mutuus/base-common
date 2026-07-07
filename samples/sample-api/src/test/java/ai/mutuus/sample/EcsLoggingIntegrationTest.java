package ai.mutuus.sample;

import java.util.List;
import java.util.Optional;

import ai.mutuus.common.logging.AccessLogger;
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
 * <b>ECS(Elastic Common Schema) 로깅</b> 통합 검증 — 공통모듈이 dual 모드(기본)에서 access/error/security 로그에
 * ECS 필드를 함께 남기는지 실서버 + 로그 캡처(ListAppender)로 확인한다.
 * <p>검증 포인트(전략 문서 [260707.004]):
 * <ul>
 *   <li>access completed: {@code event.action}/{@code event.dataset=mutuus.access}/{@code event.duration}(nanos)/
 *       {@code event.outcome}/{@code http.response.status_code}/{@code event.category=[web]} + legacy 병존</li>
 *   <li>server error: {@code event.dataset=mutuus.error}/{@code error.type}/{@code error.code}/{@code event.outcome=failure}</li>
 *   <li>security audit: {@code event.dataset=mutuus.security_audit}/{@code event.category=[authentication,iam]}/
 *       {@code event.outcome=failure}/{@code client.ip}</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "mutuus.sample.mock-jwt=true")
class EcsLoggingIntegrationTest {

    @LocalServerPort
    int port;

    private ListAppender<ILoggingEvent> accessAppender;
    private ListAppender<ILoggingEvent> securityAppender;
    private Logger accessLogger;
    private Logger securityLogger;

    @BeforeEach
    void attach() {
        accessLogger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        securityLogger = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.LOGGER_NAME);
        accessAppender = new ListAppender<>();
        securityAppender = new ListAppender<>();
        accessAppender.start();
        securityAppender.start();
        accessLogger.addAppender(accessAppender);
        securityLogger.addAppender(securityAppender);
    }

    @AfterEach
    void detach() {
        accessLogger.detachAppender(accessAppender);
        securityLogger.detachAppender(securityAppender);
    }

    @Test
    void access_completed는_ECS필드와_legacy를_함께_남긴다() {
        int status = RestClient.create().get().uri(base() + "/api/secure/whoami")
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice")
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(200);

        ILoggingEvent ev = accessEvent("request.completed");
        // ECS
        assertThat(raw(ev, "event.action")).isEqualTo("request.completed");
        assertThat(raw(ev, "event.dataset")).isEqualTo("mutuus.access");
        assertThat(raw(ev, "data_stream.dataset")).isEqualTo("mutuus.access");
        assertThat(raw(ev, "event.outcome")).isEqualTo("success");
        assertThat(raw(ev, "http.response.status_code")).isEqualTo(200);
        assertThat(raw(ev, "event.category")).isEqualTo(List.of("web"));
        assertThat(raw(ev, "event.duration")).isInstanceOf(Long.class);
        assertThat((Long) raw(ev, "event.duration")).isGreaterThan(0L);
        // legacy 병존(dual)
        assertThat(raw(ev, "event")).isEqualTo("request.completed");
        assertThat(raw(ev, "httpStatus")).isEqualTo(200);
    }

    @Test
    void server_error는_mutuus_error_dataset과_error필드를_남긴다() {
        RestClient.create().get().uri(base() + "/api/secure/boom")
                .header(HttpHeaders.AUTHORIZATION, "Bearer alice")
                .exchange((req, res) -> res.getStatusCode().value());

        ILoggingEvent ev = accessEvent("error.server");
        assertThat(raw(ev, "event.dataset")).isEqualTo("mutuus.error");
        assertThat(raw(ev, "event.outcome")).isEqualTo("failure");
        assertThat(raw(ev, "error.type")).isInstanceOf(String.class);
        assertThat(raw(ev, "error.code")).isEqualTo("INTERNAL_ERROR");
        // 스택트레이스는 인코더가 렌더(throwable 존재)
        assertThat(ev.getThrowableProxy()).isNotNull();
    }

    @Test
    void security_authn_failed는_security_audit_dataset과_ECS보안필드를_남긴다() {
        int status = RestClient.create().get().uri(base() + "/api/secure/whoami")
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(401);

        ILoggingEvent ev = securityEvent("security.authn.failed");
        assertThat(raw(ev, "event.dataset")).isEqualTo("mutuus.security_audit");
        assertThat(raw(ev, "event.action")).isEqualTo("security.authn.failed");
        assertThat(raw(ev, "event.outcome")).isEqualTo("failure");
        assertThat(raw(ev, "event.category")).isEqualTo(List.of("authentication", "iam"));
        assertThat(raw(ev, "client.ip")).isInstanceOf(String.class);
        assertThat(raw(ev, "mutuus.security.disposition")).isEqualTo("denied");
        // legacy 병존
        assertThat(raw(ev, "event")).isEqualTo("security.authn.failed");
        assertThat(raw(ev, "securityEvent")).isEqualTo(true);
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private ILoggingEvent accessEvent(String action) {
        return find(accessAppender, action);
    }

    private ILoggingEvent securityEvent(String action) {
        return find(securityAppender, action);
    }

    private static ILoggingEvent find(ListAppender<ILoggingEvent> appender, String action) {
        Optional<ILoggingEvent> ev = appender.list.stream()
                .filter(e -> action.equals(raw(e, "event.action")))
                .findFirst();
        assertThat(ev).as("event.action=%s 로그가 있어야 한다", action).isPresent();
        return ev.get();
    }

    private static Object raw(ILoggingEvent e, String key) {
        if (e.getKeyValuePairs() == null) {
            return null;
        }
        return e.getKeyValuePairs().stream()
                .filter(p -> key.equals(p.key))
                .map(p -> p.value)
                .findFirst().orElse(null);
    }
}
