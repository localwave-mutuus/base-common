package ai.mutuus.sample;

import java.util.Arrays;

import ai.mutuus.common.aop.DefaultMethodLoggingWriter;
import ai.mutuus.common.aop.MethodLoggingWriter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 확장점 실증: 소비 서비스가 {@link MethodLoggingWriter} 빈을 재정의하면 라이브러리 기본 구현
 * ({@link DefaultMethodLoggingWriter})을 <b>대체</b>하고({@code @ConditionalOnMissingBean}),
 * 그 재정의로 <b>PII 마스킹</b>이 실제 메서드 로그에 적용됨을 end-to-end 로 확인한다.
 * <p>method-logging 은 application.yml 에서 web/demo 패키지 대상으로 켜져 있다(데모 관찰용).
 * 여기서는 그 위에 마스킹 Writer 를 얹어, 6자리 이상 연속 숫자(카드/주민번호 류)를 {@code [REDACTED]} 로 가린다.
 */
@SpringBootTest(properties = "mutuus.common.method-logging.enabled=true")
@AutoConfigureMockMvc
@Import(CustomMethodLoggingWriterIntegrationTest.MaskingConfig.class)
class CustomMethodLoggingWriterIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MethodLoggingWriter writer;

    private ListAppender<ILoggingEvent> appender;
    private Logger methodLogger;

    @BeforeEach
    void attach() {
        methodLogger = (Logger) LoggerFactory.getLogger("ai.mutuus.common.method");
        appender = new ListAppender<>();
        appender.start();
        methodLogger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        methodLogger.detachAppender(appender);
    }

    @Test
    void 소비자_커스텀_Writer가_기본을_대체하고_PII를_마스킹한다() throws Exception {
        // (1) 대체 확인: 주입된 빈은 라이브러리 기본이 아니라 소비자 마스킹 구현이다.
        assertThat(writer).isInstanceOf(MaskingMethodLoggingWriter.class);
        assertThat(writer).isNotInstanceOf(DefaultMethodLoggingWriter.class);

        // (2) 마스킹 실증: 6자리 이상 연속 숫자가 들어간 본문으로 컨트롤러 메서드를 호출
        mockMvc.perform(post("/api/public/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"9876543210\"}"))
                .andExpect(status().isOk());

        // method.enter 의 args 는 마스킹돼 원본 숫자가 남지 않는다.
        String args = keyValue("method.enter", "args");
        assertThat(args).contains("[REDACTED]").doesNotContain("9876543210");
        // method.exit 의 return 도 동일하게 마스킹된다.
        String ret = keyValue("method.exit", "return");
        assertThat(ret).contains("[REDACTED]").doesNotContain("9876543210");
    }

    private String keyValue(String event, String key) {
        return appender.list.stream()
                .filter(e -> event.equals(kv(e, "event")))
                .map(e -> kv(e, key))
                .filter(v -> v != null)
                .findFirst().orElseThrow(() -> new AssertionError("이벤트 없음: " + event));
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

    /** 소비 서비스가 제공하는 마스킹 Writer — 기본 구현을 대체(@ConditionalOnMissingBean). */
    @TestConfiguration
    static class MaskingConfig {
        @Bean
        MethodLoggingWriter maskingMethodLoggingWriter() {
            return new MaskingMethodLoggingWriter();
        }
    }

    /**
     * 인자/리턴을 남기되 6자리 이상 연속 숫자(카드번호·주민번호 등)를 {@code [REDACTED]} 로 가린다.
     * 로거 이름은 기본과 동일({@code ai.mutuus.common.method})이라 소비 측에서 레벨을 그대로 제어할 수 있다.
     */
    static class MaskingMethodLoggingWriter implements MethodLoggingWriter {

        private static final org.slf4j.Logger log =
                LoggerFactory.getLogger("ai.mutuus.common.method");

        @Override
        public void onEnter(String type, String method, Object[] args) {
            emit("method.enter", type, method, "args", mask(Arrays.deepToString(args)), "controller method enter");
        }

        @Override
        public void onExit(String type, String method, Object result) {
            emit("method.exit", type, method, "return", mask(String.valueOf(result)), "controller method exit");
        }

        private void emit(String event, String type, String method, String valueKey, String value, String msg) {
            LoggingEventBuilder ev = log.atInfo()
                    .addKeyValue("event", event)
                    .addKeyValue("class", type)
                    .addKeyValue("method", method)
                    .addKeyValue(valueKey, value);
            ev.log(msg);
        }

        /** PII 마스킹: 6자리 이상 연속 숫자를 가린다. 실제로는 필드명/정규식 정책으로 확장한다. */
        static String mask(String s) {
            return s == null ? null : s.replaceAll("\\d{6,}", "[REDACTED]");
        }
    }
}
