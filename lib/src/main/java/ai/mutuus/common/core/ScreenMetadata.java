package ai.mutuus.common.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/** 선택 화면 헤더의 로그용 형식 검사. 인증/인가/업무 입력 판정이나 외부 전파에 사용하지 않는다. */
public final class ScreenMetadata {
    private static final Map<String, Pattern> FORMATS = Map.of(
            HeaderNames.SCREEN_CATALOG_VERSION, Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}\\.[0-9]+"),
            HeaderNames.BUSINESS_SCREEN_NO, Pattern.compile("[A-Z]{3}-[0-9]{3}"),
            HeaderNames.INTERNAL_SCREEN_ID, Pattern.compile("GM-SCR-[0-9]{6}"));
    private ScreenMetadata() {}

    /** 잘못된 값은 로그에서만 생략하며 요청은 그대로 처리한다. 과도한 길이는 로그에 싣지 않는다. */
    public static Map<String, String> fromHeaders(Function<String, String> reader) {
        Map<String, String> values = new LinkedHashMap<>();
        FORMATS.forEach((name, pattern) -> {
            String value = reader.apply(name);
            if (value != null && value.length() <= 64 && pattern.matcher(value).matches()) values.put(name, value);
        });
        return Map.copyOf(values);
    }
}
