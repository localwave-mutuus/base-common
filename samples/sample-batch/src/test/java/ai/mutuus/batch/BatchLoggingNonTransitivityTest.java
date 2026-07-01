package ai.mutuus.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로깅 확장 3계층(payload/controller-entry/method-logging)의 <b>비웹 비전이</b> 회귀 가드.
 * <p>세 토글을 <b>모두 켠 상태로</b>도, 비웹 배치 소비자에는 이 자동구성들이 활성화되지 않음을 증명한다.
 * payload/controller-entry 는 {@code @ConditionalOnClass(DispatcherServlet)}, method-logging 은
 * {@code @ConditionalOnClass(ProceedingJoinPoint, RestController)} 로 보호되는데, web/AspectJ starter 가
 * lib 에서 optional 이라 배치 classpath 에 전이되지 않기 때문이다(토글이 아니라 classpath 가 막는다).
 * <p>웹 전용/AOP 클래스는 배치 classpath 에 없어 타입 참조조차 불가하므로 <b>빈 이름</b>으로 부재를 확인한다.
 */
@SpringBootTest(properties = {
        "mutuus.common.payload-logging.enabled=true",
        "mutuus.common.controller-entry.enabled=true",
        "mutuus.common.method-logging.enabled=true"
})
class BatchLoggingNonTransitivityTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void 비웹에서는_로깅_토글을_켜도_payload_controllerEntry_methodLogging_이_활성화되지_않는다() {
        // payload(웹 전용) — 본문 캐싱 필터/출력 함수 부재
        assertThat(ctx.containsBean("payloadLoggingFilterRegistration")).isFalse();
        assertThat(ctx.containsBean("requestPayloadLogger")).isFalse();
        assertThat(ctx.containsBean("responsePayloadLogger")).isFalse();
        // controller-entry(웹 전용) — 인터셉터/매처/핸들러 부재
        assertThat(ctx.containsBean("controllerEntryInterceptor")).isFalse();
        assertThat(ctx.containsBean("controllerMethodMatcher")).isFalse();
        // method-logging(AOP) — Aspect/Writer 부재
        assertThat(ctx.containsBean("controllerMethodLoggingAspect")).isFalse();
        assertThat(ctx.containsBean("methodLoggingWriter")).isFalse();
    }
}
