package ai.mutuus.common.exception;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Locale;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.common.logging.AccessLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(new MessageResolver(messageSource()), new AccessLogger());

    @AfterEach
    void tearDown() {
        TraceContext.clear();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void BusinessException은_ErrorCode_status_code_i18n메시지_traceId를_담은_ApiResponse로_변환된다() {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        TraceContext.put(HeaderNames.TRACE_ID, "t-1");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusiness(
                new BusinessException(CommonErrorCode.NOT_FOUND), new MockHttpServletRequest("GET", "/api/orders/9"));

        assertThat(resp.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
        ApiResponse<Void> body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("NOT_FOUND");
        assertThat(body.message()).isEqualTo("리소스를 찾을 수 없습니다.");
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.error().code()).isEqualTo("NOT_FOUND");
        assertThat(body.traceId()).isEqualTo("t-1");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void 예상치_못한_예외는_500_INTERNAL_ERROR이고_기본은_예외클래스를_응답에_노출하지_않는다() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        // 기본(exposeException=false): 예외 클래스명은 응답에 노출하지 않는다(정보 노출 방지)
        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnexpected(
                new IllegalStateException("boom"), new MockHttpServletRequest("GET", "/api/orders/9"));

        assertThat(resp.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        ApiResponse<Void> body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("An unexpected error occurred.");
        assertThat(body.error().exception()).isNull();
    }

    @Test
    void exposeException_true면_진단용으로_예외클래스를_담는다() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        GlobalExceptionHandler exposing =
                new GlobalExceptionHandler(new MessageResolver(messageSource()), new AccessLogger(), true);

        ResponseEntity<ApiResponse<Void>> resp = exposing.handleUnexpected(
                new IllegalStateException("boom"), new MockHttpServletRequest("GET", "/api/orders/9"));

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().error().exception()).isEqualTo("java.lang.IllegalStateException");
    }

    @Test
    void nested_socket_timeout_is_mapped_to_gateway_timeout() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ResourceAccessException ex = new ResourceAccessException("wrapped",
                new IOException("nested", new SocketTimeoutException("read timed out")));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleNetwork(
                ex, new MockHttpServletRequest("GET", "/api/downstream"));

        assertThat(resp.getStatusCode().value()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo("GATEWAY_TIMEOUT");
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
