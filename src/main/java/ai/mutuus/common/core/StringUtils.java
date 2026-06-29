package ai.mutuus.common.core;

/**
 * 기본 문자열 유틸리티.
 */
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean hasText(String s) {
        return !isBlank(s);
    }

    public static String defaultIfBlank(String s, String defaultValue) {
        return isBlank(s) ? defaultValue : s;
    }

    /** 민감정보 마스킹 (앞 visible 글자만 노출). */
    public static String mask(String s, int visible) {
        if (isBlank(s)) {
            return s;
        }
        if (s.length() <= visible) {
            return "*".repeat(s.length());
        }
        return s.substring(0, visible) + "*".repeat(s.length() - visible);
    }
}
