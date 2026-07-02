package ai.mutuus.sample;

import java.util.Map;
import java.util.Optional;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>아웃바운드 헤더 전파 차단</b>(Phase 2-③) 통합 테스트 — {@code allowed-hosts} 를 localhost 가 아닌 값으로 두면,
 * 로컬 호출에는 식별 헤더({@code X-User-Id} 등)가 부착되지 않고({@code security.propagation.blocked}) 상관용
 * ({@code X-Trace-Id})만 전파되는지 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "mutuus.common.propagation.allowed-hosts=internal.example.com") // localhost 는 미신뢰
class SecurityPropagationBlockIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder restClientBuilder;

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
        TraceContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 미신뢰_호스트로는_식별헤더가_차단되고_상관헤더만_전파된다() {
        TraceContext.put(HeaderNames.TRACE_ID, "t-out");
        TraceContext.put(HeaderNames.USER_ID, "u-out");

        RestClient client = restClientBuilder.baseUrl("http://localhost:" + port).build();
        Map<String, Object> data = (Map<String, Object>) client.get().uri("/api/public/echo-headers")
                .retrieve().body(Map.class).get("data");

        assertThat(data.get("traceId")).isEqualTo("t-out");  // 상관용은 전파
        assertThat(data.get("userId")).isEqualTo("null");    // 식별 헤더는 차단(미부착)

        Optional<ILoggingEvent> ev = event("security.propagation.blocked");
        assertThat(ev).isPresent();
        assertThat(kv(ev.get(), "targetHost")).contains("localhost");
        assertThat(kv(ev.get(), "droppedHeaders")).hasValueSatisfying(v -> assertThat(v).contains(HeaderNames.USER_ID));
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
