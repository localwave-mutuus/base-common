package ai.mutuus.common.logging;

import java.nio.file.*;
import java.time.*;
import java.util.zip.GZIPInputStream;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.util.FileSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class ClosedFileRollingPolicyTest {
    @TempDir Path directory;
    @Test void 크기_시간_종료_굴림은_닫힌_gzip만_공개하고_재시작도_이름을_재사용하지_않는다() throws Exception {
        String base = "local-member-1234abcd-123";
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        context.start();
        var policy = new ClosedFileRollingPolicy<ILoggingEvent>();
        var appender = appender(context, policy, base);
        var first = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC);
        policy.setClock(first); policy.setMaxFileSize(new FileSize(1));
        appender.start();
        Logger logger = context.getLogger("test"); logger.addAppender(appender);
        logger.info("size-first"); logger.info("size-second");
        assertThat(closed()).hasSize(1);
        policy.setMaxFileSize(new FileSize(100000));
        policy.setClock(Clock.fixed(first.instant().plusSeconds(3600), ZoneOffset.UTC));
        logger.info("hour-third"); assertThat(closed()).hasSize(2);
        appender.stop(); assertThat(closed()).hasSize(3);
        var oldNames = closed().stream().map(p -> p.getFileName().toString()).toList();
        // 수집자가 닫힌 파일을 삭제해도 영속 순번 장부를 보존한다.
        for (Path path : closed()) {
            assertThat(path.getFileName().toString()).matches("local-member-1234abcd-123\\.[0-9]{8}-[0-9]{2}\\.[0-9]+\\.log\\.gz");
            try (var gzip = new GZIPInputStream(Files.newInputStream(path))) { assertThat(gzip.readAllBytes()).isNotEmpty(); }
            Files.delete(path);
        }
        logger.detachAppender(appender);
        var nextPolicy = new ClosedFileRollingPolicy<ILoggingEvent>();
        var next = appender(context, nextPolicy, base); next.start(); logger.addAppender(next);
        logger.info("restart-last"); next.stop();
        assertThat(closed()).hasSize(1);
        assertThat(oldNames).doesNotContain(closed().getFirst().getFileName().toString());
        assertThat(Files.readString(directory.resolve(base + ".log"))).isEmpty();
        assertThat(Files.list(directory).anyMatch(p -> p.getFileName().toString().contains(".part-"))).isFalse();
        context.stop();
    }
    private java.util.List<Path> closed() throws Exception {
        try (var files = Files.list(directory)) { return files.filter(p -> p.toString().endsWith(".log.gz")).toList(); }
    }
    private ClosingRollingFileAppender<ILoggingEvent> appender(LoggerContext context, ClosedFileRollingPolicy<ILoggingEvent> policy, String base) {
        var appender = new ClosingRollingFileAppender<ILoggingEvent>(); appender.setContext(context); appender.setName("FILE");
        appender.setFile(directory.resolve(base + ".log").toString());
        var encoder = new PatternLayoutEncoder(); encoder.setContext(context); encoder.setPattern("%msg%n"); encoder.start();
        appender.setEncoder(encoder); policy.setContext(context); policy.setParent(appender); policy.start(); appender.setRollingPolicy(policy);
        return appender;
    }
}
