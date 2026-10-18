package ai.mutuus.common.async;

import java.util.concurrent.*;
import ai.mutuus.common.core.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.assertj.core.api.Assertions.*;

class TraceContextPropagationTest {
    @Test void taskDecoratorRestoresWorkerMetadataAfterException() throws Exception {
        TraceContext.put(HeaderNames.SCREEN_ID, "submitter-screen");
        MDC.put("application.hint", "submitter");
        Runnable task = new TraceContextTaskDecorator().decorate(() -> {
            assertThat(TraceContext.get(HeaderNames.SCREEN_ID)).contains("submitter-screen");
            assertThat(MDC.get("application.hint")).isEqualTo("submitter");
            throw new IllegalStateException("expected");
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                TraceContext.put(HeaderNames.SCREEN_ID, "worker-screen");
                MDC.put("application.hint", "worker");
            }).get();
            assertThatThrownBy(() -> executor.submit(task).get()).hasCauseInstanceOf(IllegalStateException.class);
            assertThat(executor.submit(() -> TraceContext.get(HeaderNames.SCREEN_ID).orElse(null)).get()).isEqualTo("worker-screen");
            assertThat(executor.submit(() -> MDC.get("application.hint")).get()).isEqualTo("worker");
        } finally { TraceContext.clear(); MDC.clear(); }
    }
    @Test void callablePropagatesActualOtelContextWithoutLeakingToReusedThread() throws Exception {
        var span = Span.wrap(SpanContext.create("1234567890abcdef1234567890abcdef", "1234567890abcdef", TraceFlags.getSampled(), TraceState.getDefault()));
        Callable<String> captured;
        try (Scope ignored = span.makeCurrent()) {
            captured = TraceContextPropagation.wrap(TraceContext::traceId);
        }
        try (var executor = Executors.newSingleThreadExecutor()) {
            assertThat(executor.submit(captured).get()).isEqualTo(span.getSpanContext().getTraceId());
            assertThat(executor.submit(TraceContext::traceId).get()).isNull();
        }
    }
}
