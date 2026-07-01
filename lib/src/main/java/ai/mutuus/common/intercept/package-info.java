/**
 * 컨트롤러 메서드 진입 인터셉트 — 선택적(opt-in) 통합.
 * <p>{@link org.springframework.web.servlet.HandlerInterceptor} 기반으로, <b>명칭/패키지/URL</b>로
 * 지정한 컨트롤러 메서드의 <b>진입 시점</b>에 동작을 수행한다. 타게팅 판별
 * ({@link ai.mutuus.common.intercept.ControllerMethodMatcher})과 진입 동작
 * ({@link ai.mutuus.common.intercept.ControllerEntryHandler})을 분리해 각각 독립적으로 갱신/교체한다.
 * <p>기본 비활성({@code mutuus.common.controller-entry.enabled=true} 로 켠다). web 계층 통합이다.
 */
package ai.mutuus.common.intercept;
