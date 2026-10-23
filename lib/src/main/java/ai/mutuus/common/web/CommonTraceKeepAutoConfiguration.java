package ai.mutuus.common.web;

import java.util.Set;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration
@ConditionalOnClass(OncePerRequestFilter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "golmok.trace.keep", name = "enabled", havingValue = "true")
public class CommonTraceKeepAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    TraceKeepUserSource traceKeepUserSource() {
        return Set::of;
    }

    @Bean
    @ConditionalOnMissingBean
    TraceKeepLoginIdExtractor traceKeepLoginIdExtractor() {
        return request -> null;
    }

    @Bean
    FilterRegistrationBean<TraceKeepFilter> traceKeepFilterRegistration(
            TraceKeepUserSource users, TraceKeepLoginIdExtractor extractor) {
        var registration = new FilterRegistrationBean<>(new TraceKeepFilter(users, extractor));
        // Spring Boot's security filter is -100; run immediately inside it.
        registration.setOrder(-99);
        registration.setAsyncSupported(true);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
