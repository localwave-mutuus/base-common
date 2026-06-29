package ai.mutuus.common.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 기본 날짜/시간 유틸리티 (UTC 기준 ISO-8601).
 */
public final class DateUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private DateUtils() {
    }

    public static String nowIso() {
        return ZonedDateTime.now(UTC).format(ISO);
    }

    public static String toIso(Instant instant) {
        return instant == null ? null : instant.atZone(UTC).format(ISO);
    }

    public static long epochMilli() {
        return Instant.now().toEpochMilli();
    }
}
