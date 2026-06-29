package ai.mutuus.common.web;

import java.util.List;
import java.util.Locale;

import ai.mutuus.common.core.HeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
 */
@AutoConfiguration
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

    /** X-Locale 헤더 우선, 없으면 Accept-Language 사용. */
    @Bean
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
