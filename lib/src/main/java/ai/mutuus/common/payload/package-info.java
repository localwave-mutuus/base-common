/**
 * 컨트롤러 입출력(요청/응답 본문) 로깅 — 선택적(opt-in) 통합.
 * <p>모든 인입 요청을 컨트롤러 바깥에서 일괄 감싸, <b>진입 시 입력 파라미터</b> /
 * <b>종료 시 리턴값</b>을 각각 출력한다. 진입·종료 출력 로직을 {@link ai.mutuus.common.payload.RequestPayloadLogger}
 * / {@link ai.mutuus.common.payload.ResponsePayloadLogger} 로 분리해 독립적으로 갱신/교체할 수 있다.
 * <p>{@code core}(추적 컨텍스트) 외 상위 패키지에 의존하지 않는 web 계층 통합이며, 기본 비활성이다
 * ({@code mutuus.common.payload-logging.enabled=true} 로 켠다).
 */
package ai.mutuus.common.payload;
