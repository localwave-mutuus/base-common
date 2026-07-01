package ai.mutuus.common.core;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 민감정보 마스킹 유틸(무의존). 설정된 정규식에 매칭되는 부분을 <b>마지막 4자만 남기고</b> {@code *} 로 가린다.
 * <p>로깅(payload/method)에서 카드번호·주민등록번호 등 PII 를 로그에 남기기 전에 마스킹하는 데 쓴다.
 * 패턴은 소비/자동구성 측에서 주입한다(기본 패턴은 {@code CommonMaskingAutoConfiguration} 참조).
 */
public class SensitiveDataMasker {

    private final List<Pattern> patterns;

    public SensitiveDataMasker(List<Pattern> patterns) {
        this.patterns = List.copyOf(patterns);
    }

    /** 모든 패턴 매칭 구간을 마스킹한 문자열을 반환한다. 입력이 비면 그대로. */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String result = value;
        for (Pattern p : patterns) {
            result = p.matcher(result).replaceAll(m -> maskMatch(m.group()));
        }
        return result;
    }

    /** 매칭 텍스트를 마지막 4자만 남기고 앞을 {@code *} 로. 4자 이하면 전부 마스킹. */
    private static String maskMatch(String s) {
        int keep = 4;
        if (s.length() <= keep) {
            return "*".repeat(s.length());
        }
        return "*".repeat(s.length() - keep) + s.substring(s.length() - keep);
    }
}
