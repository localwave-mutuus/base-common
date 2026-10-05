package ai.mutuus.common.logging;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.util.FileSize;

/** 닫힌 파일만 동기 압축하고 원자적으로 공개한다. 순번 장부는 수집·삭제 대상이 아니다. */
public class ClosedFileRollingPolicy<E> extends RollingPolicyBase implements TriggeringPolicy<E> {
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyyMMdd-HH").withZone(ZoneOffset.UTC);
    private long maxFileSize = 100 * 1024 * 1024;
    private Clock clock = Clock.systemUTC();
    private Instant period;

    public void setMaxFileSize(FileSize value) { maxFileSize = value.getSize(); }
    void setClock(Clock value) { clock = value; }

    @Override public void start() {
        compressionMode = CompressionMode.GZ;
        Path active = Path.of(getActiveFileName());
        try {
            period = (Files.exists(active) && Files.size(active) > 0
                    ? Files.getLastModifiedTime(active).toInstant() : clock.instant()).truncatedTo(ChronoUnit.HOURS);
            super.start();
        } catch (IOException ex) { addError("로그 굴림 초기화 실패", ex); }
    }

    @Override public String getActiveFileName() { return getParentsRawFileProperty(); }

    @Override public boolean isTriggeringEvent(File active, E event) {
        return !clock.instant().truncatedTo(ChronoUnit.HOURS).equals(period) || active.length() >= maxFileSize;
    }

    @Override public void rollover() throws RolloverFailure {
        Path active = Path.of(getActiveFileName());
        try {
            if (Files.exists(active) && Files.size(active) > 0) {
                String base = active.getFileName().toString().replaceFirst("\\.log$", "");
                Path ledger = active.resolveSibling("." + base + ".sequence");
                // 여러 프로세스/재시작에도 같은 번호를 재사용하지 않는다. 예약은 압축보다 먼저 영속화한다.
                try (FileChannel lock = FileChannel.open(ledger.resolveSibling(ledger.getFileName() + ".lock"),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     var ignored = lock.lock()) {
                    long next = Files.exists(ledger) ? Math.addExact(Long.parseLong(Files.readString(ledger).trim()), 1) : 0;
                    Path closed, pending;
                    do { closed = active.resolveSibling(base + "." + HOUR.format(period) + "." + next + ".log.gz");
                        pending = closed.resolveSibling(closed.getFileName().toString().replace(".log.gz", ".log.pending"));
                        if (!Files.exists(closed) && !Files.exists(pending)) break;
                        next = Math.addExact(next, 1);
                    } while (true);
                    Path reservation = ledger.resolveSibling(ledger.getFileName() + ".part-" + UUID.randomUUID());
                    Files.writeString(reservation, Long.toString(next), StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE, StandardOpenOption.SYNC);
                    Files.move(reservation, ledger, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    // 활성 파일을 먼저 별도 pending으로 옮겨, 압축/삭제 실패가 다음 active에 중복되지 않게 한다.
                    Files.move(active, pending, StandardCopyOption.ATOMIC_MOVE);
                    publish(pending, closed);
                }
            }
            period = clock.instant().truncatedTo(ChronoUnit.HOURS);
        } catch (IOException | RuntimeException ex) {
            addError("LOG_ARCHIVE_FAILED: pending/part 원본 보존; 수집 이름 미공개", ex);
            throw new RolloverFailure("닫힌 로그 공개 실패; 원본/부분 파일 보존", ex);
        }
    }

    private void publish(Path pending, Path closed) throws IOException {
        Path partial = closed.resolveSibling(closed.getFileName() + ".part-" + UUID.randomUUID());
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(partial, StandardOpenOption.CREATE_NEW))) {
            Files.copy(pending, gzip);
        }
        // 압축 스트림 close 이후에만 수집 정규식에 맞는 이름이 나타난다.
        Files.move(partial, closed, StandardCopyOption.ATOMIC_MOVE);
        try { Files.delete(pending); }
        catch (IOException ex) { addWarn("LOG_PENDING_CLEANUP_FAILED: " + pending.getFileName()); }
    }
}
