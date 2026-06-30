/**
 * common-async: 비동기/가상스레드 경계 추적 컨텍스트 전파.
 * <p>{@link ai.mutuus.common.core.TraceContext} 와 SLF4J MDC 는 ThreadLocal 기반이라
 * {@code @Async}/스레드 풀/가상 스레드 경계를 자동으로 넘지 못한다. 이 패키지는 제출 스레드의
 * 컨텍스트를 캡처해 실행 스레드에서 복원하는 유틸과, Spring 실행기에 이를 자동 적용하는
 * 자동구성을 제공한다.
 */
package ai.mutuus.common.async;
