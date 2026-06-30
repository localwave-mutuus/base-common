package ai.mutuus.common.i18n;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * 다국어 자동 구성. {@link MessageResolver} 빈을 제공한다.
 * <p>메시지 번들은 Spring Boot 의 {@code MessageSourceAutoConfiguration} 이 만드는 표준
 * {@code messageSource} 를 사용한다(공통 기본 basename {@code messages/messages} 는
 * {@code CommonEnvironmentPostProcessor} 가 주입). 표준 MessageSource 가 없는 환경에서만
 * 공통 번들 기반 폴백을 제공한다. 이렇게 하여 컨텍스트에 MessageSource 가 항상 <b>하나만</b>
 * 존재하도록 보장한다(별도 빈을 추가하면 by-type 주입이 모호해짐).
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration")
public class CommonI18nAutoConfiguration {

    /** 표준 MessageSource 가 없을 때만 동작하는 폴백(보통은 Boot 의 messageSource 사용). */
    @Bean
    @ConditionalOnMissingBean(MessageSource.class)
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageResolver messageResolver(MessageSource messageSource) {
        return new MessageResolver(messageSource);
    }
}
