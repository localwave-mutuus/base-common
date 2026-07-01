package ai.mutuus.common.intercept;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 컨트롤러 메서드 진입 인터셉트 자동 구성(웹 환경 전용, <b>기본 비활성</b>).
 * <p>{@code mutuus.common.controller-entry.enabled=true} 일 때만 활성화된다. 타게팅(매처)·진입 동작(핸들러)을
 * 각각 빈으로 등록하며 둘 다 {@code @ConditionalOnMissingBean} 이라 소비 서비스가 교체/갱신할 수 있다.
 * <p>인터셉터는 별도 {@link WebMvcConfigurer} 빈으로 등록한다(URL 필터링은 매처가 담당하므로 전 경로 등록).
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(ControllerEntryProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.controller-entry", name = "enabled", havingValue = "true")
public class CommonControllerEntryAutoConfiguration {

    /** 타게팅(명칭/패키지/URL) 판별 — 소비자가 재정의하면 비활성. */
    @Bean
    @ConditionalOnMissingBean
    public ControllerMethodMatcher controllerMethodMatcher(ControllerEntryProperties props) {
        return new DefaultControllerMethodMatcher(props);
    }

    /** 진입 시 수행 동작 — 소비자가 재정의하면 비활성. */
    @Bean
    @ConditionalOnMissingBean
    public ControllerEntryHandler controllerEntryHandler() {
        return new DefaultControllerEntryHandler();
    }

    @Bean
    public ControllerEntryInterceptor controllerEntryInterceptor(
            ControllerMethodMatcher matcher, ControllerEntryHandler handler) {
        return new ControllerEntryInterceptor(matcher, handler);
    }

    /** 인터셉터를 MVC 핸들러 체인에 등록(전 경로 — 실제 타게팅은 매처가 수행). */
    @Bean
    public WebMvcConfigurer controllerEntryWebMvcConfigurer(ControllerEntryInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
