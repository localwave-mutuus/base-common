package ai.mutuus.sample;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 소비 서비스에서 별도 코드 없이 API 생명주기 로깅이 자동으로 일어나는지 end-to-end 검증.
 * <p>라이브러리 로거({@code ai.mutuus.common.access})에 ListAppender 를 붙여 실제 이벤트를 관찰한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CommonPlatformIntegrationTest.TestSecurityConfig.class)
class AccessLoggingIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private ListAppender<ILoggingEvent> appender;
    private Logger accessLogger;

    @BeforeEach
    void attachAppender() {
        accessLogger = (Logger) LoggerFactory.getLogger("ai.mutuus.common.access");
        appender = new ListAppender<>();
        appender.start();
        accessLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        accessLogger.detachAppender(appender);
    }

    @Test
    void 정상요청은_received와_completed_200을_자동_로깅한다() throws Exception {
        mockMvc.perform(get("/api/public/hello")).andExpect(status().isOk());

        assertThat(eventStatusTuples())
                .contains(tuple("request.received", null))
                .contains(tuple("request.completed", 200));
    }

    @Test
    void 인증실패는_auth_failure와_completed_401을_자동_로깅한다() throws Exception {
        mockMvc.perform(get("/api/secure/me")).andExpect(status().isUnauthorized());

        assertThat(events()).contains("auth.failure");
        assertThat(eventStatusTuples()).contains(tuple("request.completed", 401));
    }

    @Test
    void 비즈니스예외는_error_business와_completed_404를_자동_로깅한다() throws Exception {
        mockMvc.perform(get("/api/public/boom")).andExpect(status().isNotFound());

        assertThat(events()).contains("error.business");
        assertThat(eventStatusTuples()).contains(tuple("request.completed", 404));
    }

    @Test
    void 인증된_요청은_헤더가_아닌_JWT주체를_userId로_남긴다() throws Exception {
        // X-User-Id 헤더로 위장값을 보내도, 검증된 JWT subject(user-1)가 우선해야 한다
        mockMvc.perform(get("/api/secure/me")
                        .header("Authorization", "Bearer valid-token")
                        .header("X-User-Id", "spoofed"))
                .andExpect(status().isOk());

        ILoggingEvent completed = appender.list.stream()
                .filter(e -> "request.completed".equals(kv(e).get("event")))
                .findFirst().orElseThrow();
        assertThat(kv(completed)).containsEntry("userId", "user-1");
    }

    private List<String> events() {
        return appender.list.stream().map(e -> String.valueOf(kv(e).get("event"))).toList();
    }

    private List<org.assertj.core.groups.Tuple> eventStatusTuples() {
        return appender.list.stream()
                .map(e -> tuple(kv(e).get("event"), kv(e).get("httpStatus")))
                .toList();
    }

    private Map<String, Object> kv(ILoggingEvent ev) {
        List<KeyValuePair> pairs = ev.getKeyValuePairs();
        if (pairs == null) {
            return Map.of();
        }
        return pairs.stream().collect(Collectors.toMap(p -> p.key, p -> p.value));
    }
}
