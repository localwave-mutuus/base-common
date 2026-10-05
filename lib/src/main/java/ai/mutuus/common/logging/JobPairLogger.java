package ai.mutuus.common.logging;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 웹 클래스 없이 호출하는 작업 invocation 범위. try-with-resources로 모든 종료 경로를 닫는다. */
public final class JobPairLogger {
    public static final String LOGGER_NAME = "ai.mutuus.common.job";
    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    /** lease를 얻은 뒤 호출한다. OFF/NO_CLAIM에는 호출하지 않는다. 복구 invocation도 새 ID를 쓴다. */
    public Scope begin(String jobName, String operationId) {
        if (jobName == null || jobName.isBlank()) throw new IllegalArgumentException("jobName required");
        return new Scope(jobName, operationId);
    }

    public static final class Scope implements AutoCloseable {
        private final String jobName, operationId;
        private final String id = UUID.randomUUID().toString(), trace = UUID.randomUUID().toString();
        private final long started = System.nanoTime();
        private final AtomicBoolean ended = new AtomicBoolean();
        private Scope(String jobName, String operationId) {
            this.jobName = jobName; this.operationId = operationId;
            emit("job.start", "OK", "", 0);
        }
        public String executionId() { return id; }
        public String traceId() { return trace; }
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
            if (ended.compareAndSet(false, true)) emit("job.end", outcome, errorCode, System.nanoTime() - started);
        }
        private void emit(String action, String outcome, String errorCode, long nanos) {
            var event = outcome.equals("OK") ? log.atInfo() : log.atWarn();
            event.addKeyValue("event", action).addKeyValue("event.action", action)
                    .addKeyValue("event.dataset", "mutuus.job").addKeyValue("data_stream.dataset", "mutuus.job")
                    .addKeyValue("jobExecutionId", id).addKeyValue("execution.id", id).addKeyValue("requestId", id)
                    .addKeyValue("trace.id", trace).addKeyValue("job.name", jobName)
                    .addKeyValue("operation.id", operationId == null ? "" : operationId)
                    .addKeyValue("event.duration", nanos).addKeyValue("durationMs", nanos / 1_000_000)
                    .addKeyValue("outcome", outcome).addKeyValue("errorCode", errorCode).addKeyValue("error.code", errorCode)
                    .addKeyValue("event.outcome", outcome.equals("OK") ? "success" : "failure").log(action);
        }
    }
}
