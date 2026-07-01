package ai.mutuus.common.idempotency;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 멱등성(Idempotency-Key) 자동 구성(<b>기본 OFF, opt-in</b>). 웹 소비 서비스에서
 * {@code mutuus.common.idempotency.enabled=true} 일 때만 {@link IdempotencyFilter} 를 등록한다.
 * <p>저장소는 기본 {@link InMemoryIdempotencyStore}(단일 인스턴스). 소비 서비스가 {@link IdempotencyStore}
 * 빈을 정의하면(예: Redis 기반) {@code @ConditionalOnMissingBean} 으로 대체된다. 필터는
 * {@code AccessLogFilter}(HIGHEST_PRECEDENCE+10) 뒤, payload 필터(+20) 앞({@code +15})에 놓인다.
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.idempotency", name = "enabled", havingValue = "true")
public class CommonIdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(
            IdempotencyStore store, IdempotencyProperties props) {
        FilterRegistrationBean<IdempotencyFilter> reg =
                new FilterRegistrationBean<>(new IdempotencyFilter(store, props));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 15);
        reg.addUrlPatterns("/*");
        reg.setName("idempotencyFilter");
        return reg;
    }
}
