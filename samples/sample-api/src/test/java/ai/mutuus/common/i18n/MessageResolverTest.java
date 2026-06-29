package ai.mutuus.common.i18n;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 메시지 번들(messages/messages*.properties)을 로드해 로케일별 해석을 검증한다.
 */
class MessageResolverTest {

    private final MessageResolver resolver = new MessageResolver(messageSource());

    @Test
    void 로케일별로_메시지를_해석한다() {
        assertThat(resolver.get(Locale.KOREAN, "error.not.found")).isEqualTo("리소스를 찾을 수 없습니다.");
        assertThat(resolver.get(Locale.ENGLISH, "error.not.found")).isEqualTo("Resource not found.");
    }

    @Test
    void 미존재_키는_코드를_그대로_반환한다() {
        assertThat(resolver.get(Locale.KOREAN, "no.such.key")).isEqualTo("no.such.key");
    }

    private ReloadableResourceBundleMessageSource messageSource() {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }
}
