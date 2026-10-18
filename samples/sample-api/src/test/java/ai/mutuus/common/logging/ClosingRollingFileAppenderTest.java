package ai.mutuus.common.logging;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPInputStream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.FileSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** 소비자에서 실제 scheduler와 파일을 사용한다. heartbeat 로그로 굴림을 유발하지 않는다. */
class ClosingRollingFileAppenderTest {
    @TempDir Path directory;

    @Test void 조용한_활성파일도_예약된_굴림으로_닫히고_같은_시간에_순번만_증가한다() throws Exception {
        try (var fixture = fixture("quiet", 1, new PatternLayoutEncoder())) {
            assertThat(new ClosingRollingFileAppender<ILoggingEvent>().getForceRollSeconds()).isEqualTo(300);
            fixture.logger.info("quiet-first");
            await(() -> archives().size() == 1);
            assertThat(lines()).containsExactly("quiet-first");
            assertThat(archives().getFirst().getFileName().toString()).isEqualTo("quiet.20261019-10.0.log.gz");

            int nextTick = fixture.scheduler.completed.get() + 1;
            await(() -> fixture.scheduler.completed.get() >= nextTick);
            assertThat(archives()).hasSize(1); // 실제 추가 tick에도 빈 archive가 생기지 않는다.
            await(() -> activeEmpty("quiet"));
            assertThat(Files.size(directory.resolve("quiet.log"))).isZero();

            fixture.logger.info("quiet-second");
            await(() -> archives().size() == 2);
            assertThat(archives().stream().map(path -> path.getFileName().toString())).containsExactlyInAnyOrder(
                    "quiet.20261019-10.0.log.gz", "quiet.20261019-10.1.log.gz");
            fixture.logger.info("shutdown-last");
            fixture.file.stop();
            assertThat(archives()).hasSize(3);
            assertThat(archives().stream().map(path -> path.getFileName().toString())).contains(
                    "quiet.20261019-10.2.log.gz");
            assertThat(lines()).containsExactlyInAnyOrder("quiet-first", "quiet-second", "shutdown-last");
            assertThat(Files.size(directory.resolve("quiet.log"))).isZero();
            assertNoPartialFiles();

            int stoppedTicks = fixture.scheduler.started.get();
            // An actual later scheduler task witnesses a full period after stop, without sending another log.
            fixture.scheduler.schedule(() -> { }, 1200, TimeUnit.MILLISECONDS).get(4, TimeUnit.SECONDS);
            assertThat(fixture.scheduler.started.get()).isEqualTo(stoppedTicks + 1);
            assertThat(archives()).hasSize(3);
            assertNoErrors(fixture.context);
        }
    }

    @Test void 영초로_끄면_조용한파일은_자동으로_닫히지_않지만_종료시에는_닫힌다() throws Exception {
        try (var fixture = fixture("disabled", 0, new PatternLayoutEncoder())) {
            fixture.logger.info("off-until-shutdown");
            fixture.scheduler.schedule(() -> { }, 1200, TimeUnit.MILLISECONDS).get(4, TimeUnit.SECONDS);
            assertThat(fixture.scheduler.started.get()).isEqualTo(1); // 관찰용 작업만 실행, appender timer 없음.
            assertThat(archives()).isEmpty();
            assertThat(Files.readString(directory.resolve("disabled.log"))).contains("off-until-shutdown");
            fixture.file.stop();
            assertThat(lines()).containsExactly("off-until-shutdown");
            assertNoPartialFiles();
            assertNoErrors(fixture.context);
        }
    }

    @Test void 기록중_timer와_stop이_겹쳐도_받은_이벤트는_정확히_한번_유효한_gzip에_남는다() throws Exception {
        var encoder = new BlockingEncoder();
        var workers = Executors.newFixedThreadPool(3);
        try (var fixture = fixture("concurrent", 1, encoder)) {
            var first = workers.submit(() -> fixture.logger.info("admitted-before-stop"));
            assertThat(encoder.entered.await(3, TimeUnit.SECONDS)).isTrue();
            // beforeExecute observes the real periodic task before it waits for the appender's write monitor.
            await(() -> fixture.scheduler.started.get() >= 1);
            var next = workers.submit(() -> {
                for (int i = 0; i < 40; i++) fixture.logger.info("racing-{}", i);
            });
            var stopEntered = new CountDownLatch(1);
            var stopped = workers.submit(() -> { stopEntered.countDown(); fixture.file.stop(); });
            assertThat(stopEntered.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(stopped.isDone()).isFalse(); // 이미 수용한 encoder/write를 stop이 추월하지 않는다.
            encoder.release.countDown();
            first.get(5, TimeUnit.SECONDS); next.get(5, TimeUnit.SECONDS); stopped.get(5, TimeUnit.SECONDS);
            assertThat(encoder.accepted).contains("admitted-before-stop");
            assertThat(lines()).containsExactlyInAnyOrderElementsOf(encoder.accepted);
            assertThat(archives()).isNotEmpty();
            assertThat(Files.size(directory.resolve("concurrent.log"))).isZero();
            assertNoPartialFiles();
            assertNoErrors(fixture.context);
        } finally {
            encoder.release.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Fixture fixture(String base, long seconds, PatternLayoutEncoder encoder) {
        var scheduler = new ObservedScheduler();
        var context = new LoggerContext() {
            @Override public ScheduledExecutorService getScheduledExecutorService() { return scheduler; }
        };
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        context.start();
        var file = new ClosingRollingFileAppender<ILoggingEvent>();
        file.setContext(context); file.setName("FILE"); file.setForceRollSeconds(seconds);
        file.setFile(directory.resolve(base + ".log").toString());
        encoder.setContext(context); encoder.setPattern("%msg%n"); encoder.start(); file.setEncoder(encoder);
        var policy = new ClosedFileRollingPolicy<ILoggingEvent>();
        policy.setContext(context); policy.setParent(file);
        policy.setClock(Clock.fixed(Instant.parse("2026-10-19T10:02:00Z"), ZoneOffset.UTC));
        policy.setRollInterval("PT1H"); policy.setMaxFileSize(new FileSize(1_000_000));
        policy.start(); file.setRollingPolicy(policy); file.start();
        var logger = context.getLogger(base); logger.setLevel(Level.INFO); logger.setAdditive(false); logger.addAppender(file);
        return new Fixture(context, scheduler, file, logger);
    }

    private List<Path> archives() {
        try (var paths = Files.list(directory)) {
            return paths.filter(path -> path.toString().endsWith(".log.gz")).sorted().toList();
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }

    private boolean activeEmpty(String base) {
        try { return Files.size(directory.resolve(base + ".log")) == 0; }
        catch (java.io.IOException failure) { return false; }
    }

    private List<String> lines() throws Exception {
        var result = new ArrayList<String>();
        for (Path path : archives()) {
            try (var gzip = new GZIPInputStream(Files.newInputStream(path))) {
                var contents = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(contents).as("published archive must be complete and nonempty: %s", path).isNotEmpty();
                result.addAll(contents.lines().toList());
            }
        }
        return result;
    }

    private void assertNoPartialFiles() throws Exception {
        try (var paths = Files.list(directory)) {
            assertThat(paths.map(path -> path.getFileName().toString()))
                    .noneMatch(name -> name.contains(".part-") || name.endsWith(".pending"));
        }
    }

    private static void assertNoErrors(LoggerContext context) {
        assertThat(context.getStatusManager().getCopyOfStatusList()).noneMatch(status -> status.getLevel() == Status.ERROR);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(20);
        assertThat(condition.getAsBoolean()).as("condition within five seconds").isTrue();
    }

    private record Fixture(LoggerContext context, ObservedScheduler scheduler,
                           ClosingRollingFileAppender<ILoggingEvent> file, Logger logger) implements AutoCloseable {
        @Override public void close() {
            file.stop(); context.stop(); scheduler.shutdownNow();
        }
    }

    private static final class ObservedScheduler extends ScheduledThreadPoolExecutor {
        final AtomicInteger started = new AtomicInteger(), completed = new AtomicInteger();
        ObservedScheduler() { super(1); setRemoveOnCancelPolicy(true); }
        @Override protected void beforeExecute(Thread thread, Runnable task) { started.incrementAndGet(); super.beforeExecute(thread, task); }
        @Override protected void afterExecute(Runnable task, Throwable failure) { completed.incrementAndGet(); super.afterExecute(task, failure); }
    }

    private static final class BlockingEncoder extends PatternLayoutEncoder {
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        final ConcurrentLinkedQueue<String> accepted = new ConcurrentLinkedQueue<>();
        @Override public byte[] encode(ILoggingEvent event) {
            accepted.add(event.getFormattedMessage());
            if (event.getFormattedMessage().equals("admitted-before-stop")) {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("write release timed out");
                } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
            }
            return super.encode(event);
        }
    }
}
