package ai.mutuus.common.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 공통 액세스 로깅 자동 구성.
 * <p>{@link AccessLogger} 는 항상 제공하여 보안/예외 모듈이 사용할 수 있게 하고,
 * 요청/응답을 기록하는 {@link AccessLogFilter} 는 웹(MVC) 환경에서만 등록한다.
 * {@code mutuus.common.logging.enabled=false} 로 전체 비활성화 가능.
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonLoggingProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.logging", name = "enabled", matchIfMissing = true)
public class CommonLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessLogger accessLogger(CommonLoggingProperties props) {
        return new AccessLogger(props.getFormat());
    }

    /** 웹 환경에서만 요청/응답 필터를 등록한다. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(DispatcherServlet.class)
    static class WebAccessLogConfiguration {

        @Bean
        public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration(
                AccessLogger accessLogger, CommonLoggingProperties props) {
            FilterRegistrationBean<AccessLogFilter> reg =
                    new FilterRegistrationBean<>(new AccessLogFilter(accessLogger, props));
            reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);   // TraceFilter 바로 뒤
            reg.addUrlPatterns("/*");
            return reg;
        }
    }
}
