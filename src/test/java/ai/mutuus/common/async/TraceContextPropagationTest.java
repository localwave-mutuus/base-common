package ai.mutuus.common.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스레드 경계 너머 추적 컨텍스트/MDC 전파 검증.
 * <p>제출 스레드의 컨텍스트가 실행 스레드에서 복원되고, 실행 후 실행 스레드의 이전 상태로
 * 원복되는지(풀 재사용 누수 방지)를 확인한다.
 */
class TraceContextPropagationTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
        MDC.clear();
    }

    @Test
    void wrap한_Runnable은_제출_스레드의_TraceContext와_MDC를_실행_스레드로_전파한다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "t-async");
        TraceContext.put(HeaderNames.USER_ID, "u-9");
        MDC.put("X-Trace-Id", "t-async");

        String[] seen = new String[3];
        Runnable task = TraceContextPropagation.wrap(() -> {
            seen[0] = TraceContext.traceId();
            seen[1] = TraceContext.userId();
            seen[2] = MDC.get("X-Trace-Id");
        });

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(task).get();
        } finally {
            pool.shutdown();
        }

        assertThat(seen).containsExactly("t-async", "u-9", "t-async");
    }

    @Test
    void wrap하지_않으면_다른_스레드에는_컨텍스트가_전파되지_않는다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "t-async");

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> f = pool.submit(TraceContext::traceId);
            assertThat(f.get()).isNull();
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void wrap한_Callable은_값을_반환하고_실행_스레드의_이전_컨텍스트를_원복한다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "t-caller");
        Callable<String> task = TraceContextPropagation.wrap(TraceContext::traceId);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // 실행 스레드에 미리 다른 컨텍스트를 심어 둔다 → 작업 후 이 값으로 원복되어야 한다.
            pool.submit(() -> TraceContext.put(HeaderNames.TRACE_ID, "t-worker")).get();

            assertThat(pool.submit(task).get()).isEqualTo("t-caller");

            String afterRestore = pool.submit(TraceContext::traceId).get();
            assertThat(afterRestore).isEqualTo("t-worker");
        } finally {
            pool.shutdown();
        }
    }
}
