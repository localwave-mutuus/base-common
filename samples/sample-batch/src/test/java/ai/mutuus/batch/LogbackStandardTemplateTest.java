package ai.mutuus.batch;

import java.nio.file.*;
import java.time.Duration;
import java.util.zip.GZIPInputStream;
import ai.mutuus.common.logging.ClosedFileRollingPolicy;
import ai.mutuus.common.logging.ClosingRollingFileAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class LogbackStandardTemplateTest {
    @TempDir Path directory;
    @Test void 비웹_소비자가_표준_전체XML을_복사해_크기값을_바꾸고_종료하면_닫힌파일만_공개한다() throws Exception {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        context.putProperty("mutuus.logDirectory", directory.toString());
        context.putProperty("mutuus.logFileBase", "local-batch-1234abcd-123");
        context.putProperty("mutuus.logRollInterval", "PT5M");
        context.putProperty("mutuus.logMaxFileSize", "1");
        Path projectFile = directory.resolve("logback-spring.xml");
        try (var source = getClass().getResourceAsStream("/logback-standard-template.xml")) {
            assertThat(source).isNotNull(); Files.copy(source, projectFile);
        }
        JoranConfigurator configurator = new JoranConfigurator(); configurator.setContext(context);
        configurator.doConfigure(projectFile.toFile()); context.start();
        var root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var async = (ch.qos.logback.classic.AsyncAppender) root.getAppender("ASYNC_JSON_FILE");
        var file = (ClosingRollingFileAppender<?>) async.getAppender("JSON_FILE");
        var policy = (ClosedFileRollingPolicy<?>) file.getRollingPolicy();
        assertThat(policy.getRollInterval()).isEqualTo(Duration.ofMinutes(5));
        assertThat(policy.getMaxFileSize()).isEqualTo(1);
        root.info("first"); root.info("last"); context.stop();
        assertThat(context.getStatusManager().getCopyOfStatusList()).noneMatch(s -> s.getLevel() == Status.ERROR);
        java.util.List<Path> files;
        try (var listing = Files.list(directory)) { files = listing.filter(p -> p.toString().endsWith(".log.gz")).toList(); }
        assertThat(files).hasSize(2);
        StringBuilder records = new StringBuilder();
        for (Path path : files) {
            assertThat(path.getFileName().toString()).matches("local-batch-1234abcd-123\\.[0-9]{8}-[0-9]{2}\\.[0-9]+\\.log\\.gz");
            try (var stream = new GZIPInputStream(Files.newInputStream(path))) {
                records.append(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        assertThat(records.toString()).containsOnlyOnce("\"message\":\"first\"").containsOnlyOnce("\"message\":\"last\"");
        assertThat(Files.size(directory.resolve("local-batch-1234abcd-123.log"))).isZero();
        assertThatThrownBy(() -> Class.forName("jakarta.servlet.Servlet")).isInstanceOf(ClassNotFoundException.class);
    }
}
