package ai.mutuus.sample;

import java.util.Map;
import java.util.Optional;

import ai.mutuus.common.web.HttpClientLoggingInterceptor;
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
 * <b>아웃바운드 HTTP 로깅</b>(Phase 3a) 통합 검증 — 소비 서비스가 주입받는 {@code RestClient.Builder}로 만든
 * 클라이언트가, 별도 설정 없이 라이브러리의 {@link HttpClientLoggingInterceptor}를 통해 하위 호출마다
 * {@code http.client.completed}({@code mutuus.http_client} dataset) 이벤트를 남기는지 실서버(RANDOM_PORT)로 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "mutuus.sample.mock-jwt=true")
class HttpClientLoggingIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder restClientBuilder;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attach() {
        logger = (Logger) LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    @Test
    void 아웃바운드_호출은_http_client_completed를_ECS로_남긴다() {
        RestClient client = restClientBuilder.baseUrl("http://localhost:" + port).build();
        client.get().uri("/api/public/echo-headers").retrieve().body(Map.class);

        ILoggingEvent ev = event("http.client.completed");
        // ECS
        assertThat(raw(ev, "event.dataset")).isEqualTo("mutuus.http_client");
        assertThat(raw(ev, "data_stream.dataset")).isEqualTo("mutuus.http_client");
        assertThat(raw(ev, "event.outcome")).isEqualTo("success");
        assertThat(raw(ev, "http.request.method")).isEqualTo("GET");
        assertThat(raw(ev, "url.domain")).isEqualTo("localhost");
        assertThat(raw(ev, "http.response.status_code")).isEqualTo(200);
        assertThat(raw(ev, "event.duration")).isInstanceOf(Long.class);
        assertThat((Long) raw(ev, "event.duration")).isGreaterThan(0L);
        // url.full 은 쿼리 제외(scheme://host:port/path)
        assertThat(String.valueOf(raw(ev, "url.full"))).contains("/api/public/echo-headers").doesNotContain("?");
        // legacy 병존(dual)
        assertThat(raw(ev, "event")).isEqualTo("http.client.completed");
        assertThat(raw(ev, "httpStatus")).isEqualTo(200);
    }

    private ILoggingEvent event(String action) {
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
