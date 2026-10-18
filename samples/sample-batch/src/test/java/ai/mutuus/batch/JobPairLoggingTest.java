package ai.mutuus.batch;

import java.nio.file.*;
import java.util.concurrent.CancellationException;
import ai.mutuus.common.logging.JobPairLogger;
import ai.mutuus.common.logging.CommonJsonEncoder;
import ai.mutuus.common.logging.SnapshotAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.assertThat;

/** 비웹 소비 클래스패스에서 성공·실패·취소·예상 밖 종료·interrupt 및 중복 close를 확인한다. */
class JobPairLoggingTest {
    @Test void 작업범위는_각_invocation에_새로운_정확한_짝을_남긴다() throws Exception {
        assertThatThrownClassMissing();
        Logger logger = (Logger) LoggerFactory.getLogger(JobPairLogger.LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>(); capture.setContext(logger.getLoggerContext()); capture.start();
        var snapshot = new SnapshotAppender(); snapshot.setContext(logger.getLoggerContext());
        snapshot.addAppender(capture); snapshot.start(); logger.addAppender(snapshot);
        try {
            var jobs = new JobPairLogger();
            try (var scope = jobs.begin("run", "same-operation")) { scope.success(); scope.success(); }
            try (var scope = jobs.begin("run", "same-operation")) { scope.fail("OWNER_FAILED"); }
            try (var scope = jobs.begin("decision", "operation")) { scope.fail(new CancellationException("PRIVATE_DO_NOT_LOG"), "OWNER_FAILED"); }
            try (var scope = jobs.begin("run", "operation")) { /* 예외/Error로 결과 표시 없이 종료한 경우 */ }
            try (var scope = jobs.begin("decision", "operation")) { Thread.currentThread().interrupt(); }
            finally { Thread.interrupted(); }
            assertThat(capture.list).hasSize(10);
            java.util.Set<Object> ids = new java.util.HashSet<>();
            for (int i = 0; i < 10; i += 2) {
                assertThat(field(capture.list.get(i), "event.action")).isEqualTo("job.start");
                assertThat(field(capture.list.get(i + 1), "event.action")).isEqualTo("job.end");
                assertThat(field(capture.list.get(i), "execution.id")).isNotNull()
                        .isEqualTo(field(capture.list.get(i + 1), "execution.id"));
                assertThat(field(capture.list.get(i), "golmok.scope")).isEqualTo("job");
                // 이 비웹 소비자에는 agent/SDK가 없다. 실행 ID와 달리 trace는 만들어내지 않는다.
                assertThat(field(capture.list.get(i), "trace.id")).isNull();
                assertThat(field(capture.list.get(i + 1), "trace.id")).isNull();
                assertThat(field(capture.list.get(i), "jobExecutionId")).isNull();
                ids.add(field(capture.list.get(i), "execution.id"));
            }
            assertThat(ids).hasSize(5);
            assertThat(field(capture.list.get(1), "event.outcome")).isEqualTo("success");
            assertThat(field(capture.list.get(3), "event.outcome")).isEqualTo("failure");
            assertThat(field(capture.list.get(3), "error.code")).isEqualTo("OWNER_FAILED");
            assertThat(field(capture.list.get(5), "event.outcome")).isEqualTo("failure");
            assertThat(field(capture.list.get(5), "error.code")).isEqualTo("JOB_CANCELLED");
            assertThat(field(capture.list.get(7), "error.code")).isEqualTo("JOB_SCOPE_UNFINISHED");
            assertThat(field(capture.list.get(9), "event.outcome")).isEqualTo("failure");
            assertThat(field(capture.list.get(9), "error.code")).isEqualTo("JOB_CANCELLED");
            CommonJsonEncoder encoder = new CommonJsonEncoder(); encoder.setContext(logger.getLoggerContext()); encoder.start();
            Path output = Path.of("target/wave18/job-pairs.jsonl"); Files.createDirectories(output.getParent());
            try (var stream = Files.newOutputStream(output)) { for (var event : capture.list) stream.write(encoder.encode(event)); }
            assertThat(Files.readString(output)).contains("\"execution\":{\"id\":")
                    .doesNotContain("PRIVATE_DO_NOT_LOG", "jobExecutionId", "\"outcome\":\"CANCELLED\""); encoder.stop();
        } finally { logger.detachAppender(snapshot); snapshot.stop(); }
    }
    static Object field(ILoggingEvent event, String key) {
        return event.getKeyValuePairs().stream()
                .filter(p -> p.key.equals("__common_platform_snapshot_v2"))
                .map(p -> (java.util.Map<?, ?>) p.value).findFirst().orElseThrow().get(key);
    }
    static void assertThatThrownClassMissing() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Class.forName("jakarta.servlet.Filter")).isInstanceOf(ClassNotFoundException.class);
    }
}
