package ai.mutuus.common.i18n;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * 다국어 자동 구성. 공통 메시지 번들({@code messages/messages*})을 로드하는
 * MessageSource 와 {@link MessageResolver} 빈을 제공한다.
 */
@AutoConfiguration
public class CommonI18nAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "commonMessageSource")
    public MessageSource commonMessageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageResolver messageResolver(MessageSource commonMessageSource) {
        return new MessageResolver(commonMessageSource);
    }
}
