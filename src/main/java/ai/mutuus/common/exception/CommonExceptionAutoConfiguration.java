package ai.mutuus.common.exception;

import ai.mutuus.common.i18n.MessageResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 공통 예외 처리 자동 구성. 웹(MVC) 환경에서만 동작한다.
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
public class CommonExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(MessageResolver messageResolver) {
        return new GlobalExceptionHandler(messageResolver);
    }
}
