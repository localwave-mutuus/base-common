package ai.mutuus.common.exception;

import java.util.Locale;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.i18n.MessageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new MessageResolver(messageSource()));

    @AfterEach
    void tearDown() {
        TraceContext.clear();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void BusinessException은_ErrorCode의_status_title_i18n메시지로_변환된다() {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        TraceContext.put(HeaderNames.TRACE_ID, "t-1");
        TraceContext.put(HeaderNames.SCREEN_ID, "SCR-001");

        ProblemDetail pd = handler.handleBusiness(new BusinessException(ErrorCode.NOT_FOUND), null);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("NOT_FOUND");
        assertThat(pd.getDetail()).isEqualTo("리소스를 찾을 수 없습니다.");
        assertThat(pd.getType()).hasToString("urn:mutuus:error:not_found");
        assertThat(pd.getProperties())
                .containsEntry("traceId", "t-1")
                .containsEntry("screenId", "SCR-001")
                .containsKey("timestamp");
    }

    @Test
    void 예상치_못한_예외는_500_INTERNAL_ERROR로_변환된다() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ProblemDetail pd = handler.handleUnexpected(new RuntimeException("boom"), null);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo("INTERNAL_ERROR");
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred.");
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
