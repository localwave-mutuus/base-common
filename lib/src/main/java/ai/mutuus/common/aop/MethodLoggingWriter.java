package ai.mutuus.common.aop;

/**
 * <b>메서드 진입(인자) / 종료(리턴) 출력 함수</b>. AOP Aspect 가 진입·종료 시점에 호출한다.
 * <p>출력 형식을 지속적으로 갱신/교체할 수 있도록 인터페이스로 분리했다. 소비 서비스가 자체 빈을
 * 정의하면 {@code @ConditionalOnMissingBean} 으로 기본 구현({@link DefaultMethodLoggingWriter})을 대체한다.
 */
public interface MethodLoggingWriter {

    /** 메서드 진입 — 입력 인자 출력. */
    void onEnter(String type, String method, Object[] args);

    /** 메서드 정상 종료 — 리턴값 출력. */
    void onExit(String type, String method, Object result);
}
