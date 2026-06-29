package ai.mutuus.common.web;

import java.util.List;
import java.util.Locale;

import ai.mutuus.common.core.HeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
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
public class CommonWebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilterRegistration() {
        FilterRegistrationBean<TraceFilter> reg = new FilterRegistrationBean<>(new TraceFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public HeaderPropagationInterceptor headerPropagationInterceptor() {
        return new HeaderPropagationInterceptor();
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
}
