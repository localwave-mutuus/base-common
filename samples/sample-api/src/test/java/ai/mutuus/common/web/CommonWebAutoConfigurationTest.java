package ai.mutuus.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.LocaleResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * optional 의존성 + 조건부 자동구성 검증.
 * <p>이 라이브러리의 핵심 설계(@ConditionalOnClass/@ConditionalOnProperty)가
 * 의도대로 빈을 켜고 끄는지 확인한다.
 */
class CommonWebAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class));

    @Test
    void 웹_환경이면_추적필터_헤더전파_로케일리졸버가_등록된다() {
        runner.run(ctx -> {
            assertThat(ctx).hasBean("traceFilterRegistration");
            assertThat(ctx).hasSingleBean(HeaderPropagationInterceptor.class);
            assertThat(ctx).hasSingleBean(LocaleResolver.class);
        });
    }

    @Test
    void tracing_enabled가_false면_웹_자동구성이_비활성화된다() {
        runner.withPropertyValues("mutuus.common.tracing-enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(HeaderPropagationInterceptor.class));
    }

    @Test
    void RestClient_RestTemplate가_classpath에_있으면_전파_및_아웃바운드로깅_커스터마이저가_등록된다() {
        // spring-boot-restclient 가 테스트 classpath 에 있으므로 @ConditionalOnClass 통과.
        // 전파(headerPropagation) + 아웃바운드 로깅(httpClientLogging, 기본 ON) 커스터마이저가 함께 등록된다.
        runner.run(ctx -> {
            assertThat(ctx).hasBean("headerPropagationRestClientCustomizer");
            assertThat(ctx).hasBean("httpClientLoggingRestClientCustomizer");
            assertThat(ctx).getBeans(RestClientCustomizer.class).hasSize(2);
            assertThat(ctx).hasBean("headerPropagationRestTemplateCustomizer");
            assertThat(ctx).hasBean("httpClientLoggingRestTemplateCustomizer");
            assertThat(ctx).getBeans(RestTemplateCustomizer.class).hasSize(2);
            assertThat(ctx).hasSingleBean(HttpClientLoggingInterceptor.class);
        });
    }

    @Test
    void client_logging_enabled가_false면_아웃바운드로깅_커스터마이저가_비활성화된다() {
        runner.withPropertyValues("mutuus.common.http.client-logging.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(HttpClientLoggingInterceptor.class);
                    assertThat(ctx).doesNotHaveBean("httpClientLoggingRestClientCustomizer");
                    // 전파 커스터마이저는 그대로 유지
                    assertThat(ctx).hasBean("headerPropagationRestClientCustomizer");
                });
    }
}
