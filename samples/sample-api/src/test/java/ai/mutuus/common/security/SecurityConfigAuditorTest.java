package ai.mutuus.common.security;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import ai.mutuus.common.security.audit.SecurityAuditLogger;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SecurityConfigAuditor} 단위 테스트 — 시작 시 설정 위험 조합을 {@code security.config.*} 경고로 남기는지 검증한다.
 */
class SecurityConfigAuditorTest {

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
    void 본문로깅_ON에_마스킹_OFF면_masking_disabled_경고() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("mutuus.common.payload-logging.enabled", "true")
                .withProperty("mutuus.common.logging.masking.enabled", "false");

        fire(env, new CommonSecurityProperties());

        assertThat(event("security.config.masking_disabled")).isPresent();
    }

    @Test
    void permit_all에_와일드카드가_있으면_permit_all_broad_경고() {
        CommonSecurityProperties props = new CommonSecurityProperties();
        props.setPermitAll(List.of("/**"));

        fire(new MockEnvironment(), props);

        assertThat(event("security.config.permit_all_broad")).isPresent();
    }

    @Test
    void 마스킹_ON이면_masking_disabled_경고없음() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("mutuus.common.payload-logging.enabled", "true")
                .withProperty("mutuus.common.logging.masking.enabled", "true");

        fire(env, new CommonSecurityProperties());

        assertThat(event("security.config.masking_disabled")).isEmpty();
    }

    private void fire(MockEnvironment env, CommonSecurityProperties props) {
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.setEnvironment(env);
        ctx.refresh();
        new SecurityConfigAuditor(new SecurityAuditLogger(), props)
                .onApplicationEvent(new ApplicationReadyEvent(
                        new SpringApplication(), new String[0], ctx, Duration.ZERO));
        ctx.close();
    }

    private Optional<ILoggingEvent> event(String name) {
        return appender.list.stream()
                .filter(e -> e.getKeyValuePairs() != null && e.getKeyValuePairs().stream()
                        .anyMatch(p -> "event".equals(p.key) && name.equals(p.value)))
                .findFirst();
    }
}
