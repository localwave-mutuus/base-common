package ai.mutuus.batch;

import java.nio.file.*;
import java.util.concurrent.CancellationException;
import ai.mutuus.common.logging.JobPairLogger;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.assertThat;

/** 비웹 소비 클래스패스에서 성공·실패·취소·예상 밖 종료·interrupt 및 중복 close를 확인한다. */
class JobPairLoggingTest {
    @Test void 작업범위는_각_invocation에_새로운_정확한_짝을_남긴다() throws Exception {
        assertThatThrownClassMissing();
        Logger logger = (Logger) LoggerFactory.getLogger(JobPairLogger.LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>(); capture.setContext(logger.getLoggerContext()); capture.start(); logger.addAppender(capture);
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
                assertThat(field(capture.list.get(i), "jobExecutionId")).isEqualTo(field(capture.list.get(i + 1), "jobExecutionId"));
                assertThat(field(capture.list.get(i), "trace.id")).isEqualTo(field(capture.list.get(i + 1), "trace.id"));
                ids.add(field(capture.list.get(i), "jobExecutionId"));
            }
            assertThat(ids).hasSize(5);
            assertThat(field(capture.list.get(1), "outcome")).isEqualTo("OK");
            assertThat(field(capture.list.get(3), "outcome")).isEqualTo("ERROR");
            assertThat(field(capture.list.get(5), "outcome")).isEqualTo("CANCELLED");
            assertThat(field(capture.list.get(7), "error.code")).isEqualTo("JOB_SCOPE_UNFINISHED");
            assertThat(field(capture.list.get(9), "outcome")).isEqualTo("CANCELLED");
            LogstashEncoder encoder = new LogstashEncoder(); encoder.setContext(logger.getLoggerContext()); encoder.start();
            Path output = Path.of("target/wave18/job-pairs.jsonl"); Files.createDirectories(output.getParent());
            try (var stream = Files.newOutputStream(output)) { for (var event : capture.list) stream.write(encoder.encode(event)); }
            assertThat(Files.readString(output)).doesNotContain("PRIVATE_DO_NOT_LOG"); encoder.stop();
        } finally { logger.detachAppender(capture); }
    }
    static Object field(ILoggingEvent event, String key) {
        return event.getKeyValuePairs().stream().filter(p -> p.key.equals(key)).map(p -> p.value).findFirst().orElse(null);
    }
    static void assertThatThrownClassMissing() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Class.forName("jakarta.servlet.Filter")).isInstanceOf(ClassNotFoundException.class);
    }
}
