package ai.mutuus.common.exception;

import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.common.logging.AccessLogger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 공통 예외 처리 자동 구성. 웹(MVC) 환경에서만 동작한다.
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(CommonExceptionProperties.class)
public class CommonExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(MessageResolver messageResolver,
                                                         ObjectProvider<AccessLogger> accessLogger,
                                                         CommonExceptionProperties props) {
        return new GlobalExceptionHandler(messageResolver,
                accessLogger.getIfAvailable(AccessLogger::new), props.isExposeException());
    }
}
