package ai.mutuus.common.logging;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import ai.mutuus.common.core.TelemetryScope;
import ai.mutuus.common.core.TelemetryMetadata;

/** 웹 클래스 없이 호출하는 작업 invocation 범위. try-with-resources로 모든 종료 경로를 닫는다. */
public final class JobPairLogger {
    public static final String LOGGER_NAME = "ai.mutuus.common.job";
    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    /** 독립 정기 실행의 root span. OFF/NO_CLAIM도 이 범위에서 결과로 기록한다. */
    public Scope begin(String jobName, String operationId) {
        return root(jobName, operationId, null);
    }

    /** 원인과 link만 연결하는 독립 root. 호출자의 사용자 문맥은 물려받지 않는다. */
    public Scope beginRootLinked(String jobName, String operationId, SpanContext cause) {
        if (cause == null || !cause.isValid()) throw new IllegalArgumentException("valid cause required");
        return root(jobName, operationId, cause);
    }

    /** 저장 owner가 복원한 유효 span과 execution metadata에서 로그 범위만 연다.
     * 빌린 span의 종료는 호출자가 소유하며 span 속성을 읽거나 다른 실행 ID로 보완하지 않는다. */
    public Scope beginInCurrentSpan(String jobName, String operationId) {
        validName(jobName);
        Span span = Span.current();
        if (!span.getSpanContext().isValid()) throw new IllegalStateException("current valid span required");
        if (!TelemetryMetadata.current().containsKey("execution.id"))
            throw new IllegalStateException("current verified execution metadata required");
        return new Scope(jobName, operationId, span, Context.current(), false);
    }

    private static void validName(String jobName) {
        if (jobName == null || jobName.isBlank()) throw new IllegalArgumentException("jobName required");
    }

    private Scope root(String jobName, String operationId, SpanContext cause) {
        validName(jobName);
        var builder = GlobalOpenTelemetry.getTracer("ai.mutuus.common").spanBuilder(jobName).setNoParent();
        if (cause != null) builder.addLink(cause);
        Span span;
        try (var ignored = Context.root().makeCurrent()) { span = builder.startSpan(); }
        return new Scope(jobName, operationId, span, Context.root().with(span), true);
    }

    public static final class Scope implements AutoCloseable {
        private final String jobName, operationId;
        private final String id;
        private final Span span;
        private final boolean ownsSpan;
        private final io.opentelemetry.context.Scope otelScope;
        private final long started = System.nanoTime();
        private final AtomicBoolean ended = new AtomicBoolean();
        private Scope(String jobName, String operationId, Span span, Context context, boolean ownsSpan) {
            this.jobName = jobName;
            var inherited = ownsSpan ? java.util.Map.<String, String>of() : TelemetryMetadata.current();
            this.operationId = inherited.getOrDefault("operation.id", operationId);
            this.id = inherited.containsKey("execution.id") ? inherited.get("execution.id") : UUID.randomUUID().toString();
            this.span = span;
            this.ownsSpan = ownsSpan;
            if (ownsSpan) {
                span.setAttribute("job.name", jobName).setAttribute("execution.id", id);
                if (this.operationId != null) span.setAttribute("operation.id", this.operationId);
            }
            otelScope = TelemetryScope.withJob(context, jobName, this.operationId, id).makeCurrent();
            try { emit("job.start", "OK", "", 0); }
            catch (RuntimeException | Error failure) {
                try { otelScope.close(); } finally { if (ownsSpan) span.end(); }
                throw failure;
            }
        }
        public String executionId() { return id; }
        public String traceId() { return span.getSpanContext().isValid() ? span.getSpanContext().getTraceId() : null; }
        public void success() { finish("OK", ""); }
        public void fail(String errorCode) { finish("ERROR", code(errorCode)); }
        public void cancel() { finish("CANCELLED", "JOB_CANCELLED"); }
        /** 예외 메시지/stack/payload는 쓰지 않고 취소 여부만 분류한다. */
        public void fail(Throwable failure, String errorCode) {
            if (failure instanceof CancellationException || failure instanceof InterruptedException || Thread.currentThread().isInterrupted()) cancel();
            else fail(errorCode);
        }
        @Override public void close() {
            if (Thread.currentThread().isInterrupted()) cancel();
            else finish("ERROR", "JOB_SCOPE_UNFINISHED");
        }
        private static String code(String value) {
            return value != null && value.matches("[A-Z][A-Z0-9_]{0,79}") ? value : "JOB_FAILED";
        }
        private void finish(String outcome, String errorCode) {
            if (ended.compareAndSet(false, true)) {
                try {
                    if (!"OK".equals(outcome)) span.setStatus(StatusCode.ERROR);
                    emit("job.end", outcome, errorCode, System.nanoTime() - started);
                } finally {
                    try { otelScope.close(); } finally { if (ownsSpan) span.end(); }
                }
            }
        }
        private void emit(String action, String outcome, String errorCode, long nanos) {
            var event = outcome.equals("OK") ? log.atInfo() : log.atWarn();
            event.addKeyValue("event.action", action)
                    .addKeyValue("golmok.scope", "job")
                    .addKeyValue("event.dataset", "mutuus.job")
                    .addKeyValue("execution.id", id)
                    .addKeyValue("job.name", jobName)
                    .addKeyValue("operation.id", operationId == null ? "" : operationId)
                    .addKeyValue("event.duration", nanos)
                    .addKeyValue("error.code", errorCode)
                    .addKeyValue("event.outcome", outcome.equals("OK") ? "success" : "failure").log(action);
        }
    }
}
