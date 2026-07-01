package ai.mutuus.common.payload;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <b>컨트롤러 종료 시점</b>에 리턴값(응답 본문)을 출력하는 함수.
 * <p>입력 출력({@link RequestPayloadLogger})과 분리해 독립적으로 갱신/교체한다. 소비 서비스가 자체 빈을
 * 정의하면 {@code @ConditionalOnMissingBean} 으로 기본 구현({@link DefaultResponsePayloadLogger})을 대체한다.
 */
@FunctionalInterface
public interface ResponsePayloadLogger {

    /**
     * @param request 원 요청(메서드/경로 등 상관관계용)
     * @param status  응답 상태코드
     * @param body    로깅 대상 응답 본문(Content-Type/크기 정책 적용 후, 없으면 {@code null})
     */
    void onResponse(HttpServletRequest request, int status, String body);
}
