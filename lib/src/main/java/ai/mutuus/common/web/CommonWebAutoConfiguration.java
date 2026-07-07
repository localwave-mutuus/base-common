package ai.mutuus.common.web;

import java.util.Locale;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.LogFormat;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * 공통 웹 자동 구성: 추적 필터 등록, 헤더 전파 인터셉터, 로케일 리졸버.
 * <p>{@code localeResolver} 는 Spring Boot 의 {@code WebMvcAutoConfiguration} 도 동일 이름으로
 * 등록하므로, 본 자동구성을 그보다 <b>먼저</b> 실행시켜 우리 빈이 우선 등록되게 한다
 * (Boot 의 localeResolver 는 {@code @ConditionalOnMissingBean} 이라 자동으로 물러난다).
 * 빈 오버라이딩이 비활성(Boot 기본값)인 환경에서의 {@code BeanDefinitionOverrideException} 을 방지한다.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(prefix = "mutuus.common", name = "tracing-enabled", matchIfMissing = true)
@EnableConfigurationProperties({CommonHttpProperties.class, CommonPropagationProperties.class})
public class CommonWebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilterRegistration(
            @Value("${mutuus.common.app-code:}") String appCode,
            @Value("${mutuus.common.instance-code:}") String instanceCode,
            // 인입 X-User-Id 신뢰 여부(기본 미신뢰). 값은 mutuus.common.security.* 소속이지만 여기선 원시
            // 프로퍼티로만 읽어 web→security 패키지 결합을 만들지 않는다.
            @Value("${mutuus.common.security.trust-forwarded-user:false}") boolean trustForwardedUser) {
        FilterRegistrationBean<TraceFilter> reg =
                new FilterRegistrationBean<>(new TraceFilter(appCode, instanceCode, trustForwardedUser));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public HeaderPropagationInterceptor headerPropagationInterceptor(
            CommonPropagationProperties props, ObjectProvider<SecurityAuditLogger> securityAuditLogger) {
        return new HeaderPropagationInterceptor(
                props.getAllowedHosts(), securityAuditLogger.getIfAvailable(SecurityAuditLogger::new));
    }

    /** X-Locale 헤더 우선, 없으면 Accept-Language 사용. 앱이 자체 정의하면 그것을 따른다. */
    @Bean
    @ConditionalOnMissingBean(LocaleResolver.class)
    public LocaleResolver localeResolver() {
        return new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String header = request.getHeader(HeaderNames.LOCALE);
                if (StringUtils.hasText(header)) {
                    return Locale.forLanguageTag(header);
                }
                return super.resolveLocale(request);
            }
        };
    }

    /**
     * 아웃바운드 추적 헤더 전파를 Boot가 자동구성한 {@code RestClient.Builder}에 자동 적용한다.
     * <p>Boot의 {@code RestClientAutoConfiguration}이 단일/다수 {@link RestClientCustomizer} 빈을
     * 빌더에 자동 적용하므로, 소비 서비스가 주입받는 {@code RestClient.Builder}로 만든 클라이언트는
     * 별도 설정 없이 {@link HeaderPropagationInterceptor}를 갖는다. (RestClient 자동구성 모듈
     * {@code spring-boot-restclient}가 classpath에 있을 때만 활성화)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    static class RestClientPropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "headerPropagationRestClientCustomizer")
        RestClientCustomizer headerPropagationRestClientCustomizer(HeaderPropagationInterceptor interceptor) {
            return builder -> builder.requestInterceptor(interceptor);
        }
    }

    /** 위와 동일한 전파를 {@code RestTemplateBuilder} 기반 {@code RestTemplate}에도 적용한다. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestTemplateCustomizer.class)
    static class RestTemplatePropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "headerPropagationRestTemplateCustomizer")
        RestTemplateCustomizer headerPropagationRestTemplateCustomizer(HeaderPropagationInterceptor interceptor) {
            return restTemplate -> restTemplate.getInterceptors().add(interceptor);
        }
    }

    // ---------------------------------------------------------------------
    // 아웃바운드 호출 로깅(기본 ON) — 하위 서비스 호출마다 http.client.completed 이벤트를
    // mutuus.http_client dataset 으로 남긴다(e2e 호출 구간 관측). client-logging.enabled=false 로 OFF.
    // ---------------------------------------------------------------------

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mutuus.common.http.client-logging", name = "enabled", matchIfMissing = true)
    public HttpClientLoggingInterceptor httpClientLoggingInterceptor(
            CommonHttpProperties props,
            // 로그 포맷은 액세스 로깅과 동일 출처를 원시 프로퍼티로만 읽어 web→logging 패키지 결합을 피한다.
            @Value("${mutuus.common.logging.format:DUAL}") LogFormat format) {
        return new HttpClientLoggingInterceptor(format, props.getClientLogging().getSlowRequestThresholdMillis());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.client-logging", name = "enabled", matchIfMissing = true)
    static class HttpClientLoggingRestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpClientLoggingRestClientCustomizer")
        RestClientCustomizer httpClientLoggingRestClientCustomizer(HttpClientLoggingInterceptor interceptor) {
            return builder -> builder.requestInterceptor(interceptor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestTemplateCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.client-logging", name = "enabled", matchIfMissing = true)
    static class HttpClientLoggingRestTemplateConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpClientLoggingRestTemplateCustomizer")
        RestTemplateCustomizer httpClientLoggingRestTemplateCustomizer(HttpClientLoggingInterceptor interceptor) {
            return restTemplate -> restTemplate.getInterceptors().add(interceptor);
        }
    }

    // ---------------------------------------------------------------------
    // 아웃바운드 재시도(opt-in) — 멱등 요청 IOException(연결/타임아웃) 재시도.
    // mutuus.common.http.retry.enabled=true 일 때만 클라이언트 빌더에 인터셉터를 얹는다.
    // ---------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "true")
    static class HttpRetryConfiguration {

        @Bean
        @ConditionalOnMissingBean
        HttpRetryInterceptor httpRetryInterceptor(CommonHttpProperties props) {
            return new HttpRetryInterceptor(props.getRetry().getMaxAttempts());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "true")
    static class HttpRetryRestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpRetryRestClientCustomizer")
        RestClientCustomizer httpRetryRestClientCustomizer(HttpRetryInterceptor interceptor) {
            return builder -> builder.requestInterceptor(interceptor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestTemplateCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "true")
    static class HttpRetryRestTemplateConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpRetryRestTemplateCustomizer")
        RestTemplateCustomizer httpRetryRestTemplateCustomizer(HttpRetryInterceptor interceptor) {
            return restTemplate -> restTemplate.getInterceptors().add(interceptor);
        }
    }
}
