package ai.mutuus.common.logging;

import jakarta.servlet.http.HttpServletRequest;

/** 처리된 예외가 필터 밖으로 다시 던져지지 않아도 완료 로그에 실제 클래스와 업무 코드를 전달한다.
 * <p>Throwable·메시지·스택은 보관하지 않으며 응답 노출 정책과 독립적이다.
 */
public final class HandledExceptionMetadata {
    private static final String ATTRIBUTE = HandledExceptionMetadata.class.getName() + ".type";
    private static final String CODE_ATTRIBUTE = HandledExceptionMetadata.class.getName() + ".code";
    private HandledExceptionMetadata() { }
    public static void record(HttpServletRequest request, Throwable exception) {
        if (exception != null) request.setAttribute(ATTRIBUTE, exception.getClass().getName());
        request.removeAttribute(CODE_ATTRIBUTE);
        if (exception instanceof ai.mutuus.common.exception.BusinessException business) {
            String code = business.getErrorCode().code();
            if (code != null && !code.isBlank()) request.setAttribute(CODE_ATTRIBUTE, code);
        }
    }
    public static String errorCode(HttpServletRequest request) {
        return request.getAttribute(CODE_ATTRIBUTE) instanceof String code ? code : null;
    }
    public static String errorType(HttpServletRequest request) {
        return request.getAttribute(ATTRIBUTE) instanceof String type ? type : null;
    }
}