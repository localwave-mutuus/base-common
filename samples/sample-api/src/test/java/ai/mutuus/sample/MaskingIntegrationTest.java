package ai.mutuus.sample;

import java.util.List;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #18(로그 민감정보 마스킹) 검증 — {@code masking.enabled=true}(application.yml)에서 본문의 카드/주민번호가
 * payload 로그(requestBody/responseBody)에서 마스킹됨을 ListAppender 로 확인한다(응답 자체는 원문).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MaskingIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private ListAppender<ILoggingEvent> appender;
    private Logger payloadLogger;

    @BeforeEach
    void attach() {
        payloadLogger = (Logger) LoggerFactory.getLogger("ai.mutuus.common.payload");
        appender = new ListAppender<>();
        appender.start();
        payloadLogger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        payloadLogger.detachAppender(appender);
    }

    @Test
    void 카드_주민번호가_payload로그에서_마스킹된다() throws Exception {
        mockMvc.perform(post("/demo/mask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"card\":\"9876543210123456\",\"rrn\":\"990101-1234567\"}"))
                .andExpect(status().isOk());

        // 입력 본문 로그: 카드/주민번호 원문이 사라지고 마지막 4자만 남는다.
        String requestBody = keyValue("request.payload", "requestBody");
        assertThat(requestBody)
                .isNotNull()
                .doesNotContain("9876543210123456")
                .doesNotContain("1234567")
                .contains("3456");
        // 응답 본문 로그도 동일하게 마스킹.
        String responseBody = keyValue("response.payload", "responseBody");
        assertThat(responseBody).doesNotContain("9876543210123456");
    }

    private String keyValue(String event, String key) {
        return appender.list.stream()
                .filter(e -> event.equals(kv(e, "event")))
                .map(e -> kv(e, key))
                .filter(v -> v != null)
                .findFirst().orElse(null);
    }

    private String kv(ILoggingEvent event, String key) {
        List<KeyValuePair> pairs = event.getKeyValuePairs();
        if (pairs == null) {
            return null;
        }
        for (KeyValuePair pair : pairs) {
            if (pair.key.equals(key)) {
                return String.valueOf(pair.value);
            }
        }
        return null;
    }
}
