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
 * 컨트롤러 입출력 본문 로깅(payload)과 메서드 진입 인터셉트(controller-entry)가 실제로 동작하는지 검증.
 * <p>둘 다 기본 OFF 이므로 프로퍼티로 켠 뒤, 라이브러리 로거에 ListAppender 를 붙여 이벤트를 관찰한다.
 */
@SpringBootTest(properties = {
        "mutuus.common.payload-logging.enabled=true",
        "mutuus.common.controller-entry.enabled=true",
        "mutuus.common.controller-entry.url-patterns=/api/**",
        "mutuus.sample.mock-jwt=true"
})
@AutoConfigureMockMvc
class PayloadAndEntryLoggingIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private ListAppender<ILoggingEvent> payloadAppender;
    private ListAppender<ILoggingEvent> controllerAppender;
    private Logger payloadLogger;
    private Logger controllerLogger;

    @BeforeEach
    void attach() {
        payloadLogger = (Logger) LoggerFactory.getLogger("ai.mutuus.common.payload");
        controllerLogger = (Logger) LoggerFactory.getLogger("ai.mutuus.common.controller");
        payloadAppender = new ListAppender<>();
        payloadAppender.start();
        payloadLogger.addAppender(payloadAppender);
        controllerAppender = new ListAppender<>();
        controllerAppender.start();
        controllerLogger.addAppender(controllerAppender);
    }

    @AfterEach
    void detach() {
        payloadLogger.detachAppender(payloadAppender);
        controllerLogger.detachAppender(controllerAppender);
    }

    @Test
    void 입력_리턴_본문과_컨트롤러_진입이_자동_로깅된다() throws Exception {
        mockMvc.perform(post("/api/public/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isOk());

        // 진입 시 입력 본문, 종료 시 리턴 본문이 각각 남는다
        assertThat(events(payloadAppender)).contains("request.payload", "response.payload");
        assertThat(keyValue(payloadAppender, "request.payload", "requestBody")).contains("hi");
        assertThat(keyValue(payloadAppender, "response.payload", "responseBody")).contains("echoed");

        // 대상(/api/**) 컨트롤러 메서드 진입이 남고, 메서드명이 정확하다
        assertThat(events(controllerAppender)).contains("controller.entry");
        assertThat(keyValue(controllerAppender, "controller.entry", "method")).isEqualTo("echo");
    }

    private List<String> events(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(e -> kv(e, "event")).filter(v -> v != null).toList();
    }

    private String keyValue(ListAppender<ILoggingEvent> appender, String event, String key) {
        return appender.list.stream()
                .filter(e -> event.equals(kv(e, "event")))
                .map(e -> kv(e, key))
                .filter(v -> v != null)
                .findFirst().orElse(null);
    }

    private String kv(ILoggingEvent event, String key) {
        if (event.getKeyValuePairs() == null) {
            return null;
        }
        for (KeyValuePair pair : event.getKeyValuePairs()) {
            if (pair.key.equals(key)) {
                return String.valueOf(pair.value);
            }
        }
        return null;
    }
}
