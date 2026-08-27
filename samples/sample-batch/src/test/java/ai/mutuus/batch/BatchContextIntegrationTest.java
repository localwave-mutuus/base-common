package ai.mutuus.batch;

import java.util.Locale;

import ai.mutuus.common.config.CommonProperties;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.i18n.MessageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 라이브러리의 핵심 설계 원칙 검증: <b>비웹 소비 서비스에서는 web/security 자동구성이
 * 전이/활성화되지 않고</b>, core/config/i18n 등 비웹 기능만 동작한다.
 * <p>FilteredClassLoader 단위 슬라이스가 아니라 <b>실제 비웹 Boot 컨텍스트</b>로 증명한다
 * (web/security starter 미의존 → optional 의존성 비전이). 웹 전용 빈은 클래스(spring-web 등)가
 * classpath 에 없어 타입 참조조차 불가하므로 <b>빈 이름</b>으로 부재를 확인한다.
 */
@SpringBootTest
class BatchContextIntegrationTest {

    @Autowired
    ApplicationContext ctx;

    @Autowired
    BatchTraceJob job;

    @Autowired
    MessageResolver messages;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void 비웹_컨텍스트에는_web_security_자동구성_빈이_없다() {
        // web 자동구성 산출물(인터셉터/필터)이 없어야 한다 — DispatcherServlet 부재로 비활성.
        assertThat(ctx.containsBean("headerPropagationInterceptor")).isFalse();
        assertThat(ctx.containsBean("traceFilterRegistration")).isFalse();
        // security/예외(웹) 자동구성 산출물도 없어야 한다.
        assertThat(ctx.containsBean("commonSecurityFilterChain")).isFalse();
        assertThat(ctx.containsBean("globalExceptionHandler")).isFalse();
    }

    @Test
    void 비웹에서도_config_i18n_기능은_동작한다() {
        assertThat(ctx.getBeanNamesForType(CommonProperties.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(MessageResolver.class)).hasSize(1);
        assertThat(messages.get(Locale.KOREAN, "error.not.found")).isEqualTo("리소스를 찾을 수 없습니다.");
        assertThat(messages.get(Locale.ENGLISH, "error.not.found")).isEqualTo("Resource not found.");
    }

    @Test
    void secret_loader_optOut이면_배치_환경에_복호화_source가_없다() {
        ConfigurableEnvironment environment = (ConfigurableEnvironment) ctx.getEnvironment();
        assertThat(environment.getPropertySources().contains("mutuusDecryptedSecrets")).isFalse();
    }

    @Test
    void 배치가_추적_컨텍스트를_직접_채우고_i18n을_해석한다() {
        String message = job.run(Locale.KOREAN);
        assertThat(message).isEqualTo("리소스를 찾을 수 없습니다.");
        assertThat(TraceContext.traceId()).isNotBlank();
    }
}
