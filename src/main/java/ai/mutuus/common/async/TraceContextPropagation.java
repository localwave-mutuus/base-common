package ai.mutuus.common.async;

import java.util.Map;
import java.util.concurrent.Callable;

import ai.mutuus.common.core.TraceContext;
import org.slf4j.MDC;

/**
 * {@link TraceContext}/SLF4J {@code MDC} 를 스레드 경계 너머로 전파하는 유틸.
 * <p>두 컨텍스트 모두 ThreadLocal 기반이라 가상 스레드/{@code @Async}/수동 스레드 풀 경계를
 * 자동으로 넘지 못한다. 이 유틸은 <b>작업을 제출하는 스레드</b>의 컨텍스트를 캡처해 두었다가
 * <b>실제 실행 스레드</b>에서 복원하고, 작업이 끝나면 실행 스레드의 이전 상태로 원복한다
 * (풀 스레드 재사용 시 컨텍스트 누수 방지).
 *
 * <p>Spring 비동기 실행기에는 {@link TraceContextTaskDecorator} 를 통해 자동 적용되며,
 * 직접 만든 스레드 풀에는 {@code executor.submit(TraceContextPropagation.wrap(task))} 처럼 쓴다.
 */
public final class TraceContextPropagation {

    private TraceContextPropagation() {
    }

    /** 제출 스레드의 추적 컨텍스트를 입혀 실행하도록 {@link Callable} 을 감싼다. */
    public static <T> Callable<T> wrap(Callable<T> task) {
        Map<String, String> trace = TraceContext.snapshot();
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> prevTrace = TraceContext.snapshot();
            Map<String, String> prevMdc = MDC.getCopyOfContextMap();
            apply(trace, mdc);
            try {
                return task.call();
            } finally {
                apply(prevTrace, prevMdc);
            }
        };
    }

    /** 제출 스레드의 추적 컨텍스트를 입혀 실행하도록 {@link Runnable} 을 감싼다. */
    public static Runnable wrap(Runnable task) {
        Callable<Void> wrapped = wrap(() -> {
            task.run();
            return null;
        });
        return () -> {
            try {
                wrapped.call();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                // Runnable 은 검사 예외를 던지지 않으므로 도달 불가
                throw new IllegalStateException(e);
            }
        };
    }

    /** 추적 컨텍스트와 MDC 를 주어진 스냅샷으로 통째로 교체한다(MDC 가 비면 clear). */
    private static void apply(Map<String, String> trace, Map<String, String> mdc) {
        TraceContext.replace(trace);
        if (mdc != null) {
            MDC.setContextMap(mdc);
        } else {
            MDC.clear();
        }
    }
}
