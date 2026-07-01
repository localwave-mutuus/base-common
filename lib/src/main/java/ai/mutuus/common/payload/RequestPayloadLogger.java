package ai.mutuus.common.payload;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <b>컨트롤러 진입 시점</b>에 입력 파라미터(요청 본문 등)를 출력하는 함수.
 * <p>출력 형식을 지속적으로 갱신/교체할 수 있도록 인터페이스로 분리했다. 소비 서비스가 자체 빈을
 * 정의하면 {@code @ConditionalOnMissingBean} 으로 기본 구현({@link DefaultRequestPayloadLogger})을
 * 대체한다. {@link ResponsePayloadLogger}(리턴값 출력)와는 별개로 독립 수정한다.
 */
@FunctionalInterface
public interface RequestPayloadLogger {

    /**
     * @param request 진입한 요청(메서드/경로/헤더 등 메타데이터 접근용)
     * @param body    로깅 대상 요청 본문(Content-Type/크기 정책 적용 후, 없으면 {@code null})
     */
    void onRequest(HttpServletRequest request, String body);
}
