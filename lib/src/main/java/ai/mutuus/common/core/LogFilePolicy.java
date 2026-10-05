package ai.mutuus.common.core;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;

/** 웹/Logback 의존성 없이 파일 정책을 검증한다. 값은 소비 프로젝트가 지정한다. */
public final class LogFilePolicy {
    public static final String DEFAULT_ROLL_INTERVAL = "PT1H";
    public static final String DEFAULT_MAX_FILE_SIZE = "100MB";
    private static final Duration MIN = Duration.ofMinutes(1);
    private static final Duration MAX = Duration.ofHours(24);
    private static final long HOUR_NANOS = Duration.ofHours(1).toNanos();
    private static final Pattern SIZE = Pattern.compile("([0-9]+)\\s*(B|KB|MB|GB|TB)?", Pattern.CASE_INSENSITIVE);

    private LogFilePolicy() { }

    public static Duration rollInterval(String value) {
        try {
            Duration interval = Duration.parse(value);
            if (interval.compareTo(MIN) < 0 || interval.compareTo(MAX) > 0
                    || HOUR_NANOS % interval.toNanos() != 0) throw new IllegalArgumentException();
            return interval;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("mutuus.common.logging.file.roll-interval (GS_LOG_ROLL_INTERVAL): "
                    + "ISO-8601 PT1M..PT24H 범위이며 1시간을 나누어야 합니다", ex);
        }
    }

    public static long maxFileSize(String value) {
        try {
            var match = SIZE.matcher(value.trim());
            if (!match.matches()) throw new IllegalArgumentException();
            String unit = match.group(2) == null ? "B" : match.group(2).toUpperCase(Locale.ROOT);
            int power = switch (unit) { case "KB" -> 1; case "MB" -> 2; case "GB" -> 3; case "TB" -> 4; default -> 0; };
            long bytes = Long.parseLong(match.group(1));
            for (int i = 0; i < power; i++) bytes = Math.multiplyExact(bytes, 1024);
            if (bytes <= 0) throw new IllegalArgumentException();
            return bytes;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("mutuus.common.logging.file.max-file-size (GS_LOG_MAX_FILE_SIZE): "
                    + "양의 정수와 B/KB/MB/GB/TB 단위 필요 (long 범위)", ex);
        }
    }

    /** UTC 시각의 해당 굴림 구간 시작. 파일명은 별도로 HH까지만 표기한다. */
    public static Instant period(Instant instant, Duration interval) {
        Instant hour = instant.truncatedTo(ChronoUnit.HOURS);
        long elapsed = Duration.between(hour, instant).toNanos();
        return hour.plusNanos(elapsed / interval.toNanos() * interval.toNanos());
    }
}
