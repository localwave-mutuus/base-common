package ai.mutuus.common.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 현재 요청 로케일 기준으로 메시지를 해석하는 헬퍼.
 * <p>로케일은 common-web 의 LocaleResolver(헤더 {@code X-Locale}) 또는
 * Accept-Language 로부터 {@link LocaleContextHolder}에 설정된다.
 */
public class MessageResolver {

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String code, Object... args) {
        return get(LocaleContextHolder.getLocale(), code, args);
    }

    public String get(Locale locale, String code, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
