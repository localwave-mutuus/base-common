package ai.mutuus.common.aop;

import java.util.Arrays;

import ai.mutuus.common.core.SensitiveDataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MethodLoggingWriter} 기본 구현 — 구조화 JSON 한 줄로 인자/리턴을 남긴다.
 * <p>로거 이름 {@code ai.mutuus.common.method}. 추적ID/appCode 등은 MDC 로 자동 포함된다.
 * 값이 길면 {@code maxLength} 로 절단한다. 출력 형식을 바꾸려면 이 클래스를 갱신하거나
 * 소비 측에서 {@link MethodLoggingWriter} 빈을 재정의한다.
 * <p>{@link SensitiveDataMasker}(masking on 시 주입)가 있으면 인자/리턴을 남기기 전에 PII 를 마스킹한다.
 */
public class DefaultMethodLoggingWriter implements MethodLoggingWriter {

    public static final String LOGGER_NAME = "ai.mutuus.common.method";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    private final int maxLength;
    private final SensitiveDataMasker masker;

    public DefaultMethodLoggingWriter(int maxLength) {
        this(maxLength, null);
    }

    public DefaultMethodLoggingWriter(int maxLength, SensitiveDataMasker masker) {
        this.maxLength = maxLength;
        this.masker = masker;
    }

    @Override
    public void onEnter(String type, String method, Object[] args) {
        // 진입: 역직렬화된 실제 인자 배열을 문자열로(절단). 필터/인터셉터가 못 보는 "메서드 인자"가 여기서 보인다.
        log.atInfo()
                .addKeyValue("event", "method.enter")
                .addKeyValue("class", type)
                .addKeyValue("method", method)
                .addKeyValue("args", render(Arrays.deepToString(args)))
                .log("controller method enter");
    }

    @Override
    public void onExit(String type, String method, Object result) {
        // 종료: 리턴 객체(직렬화 전)를 문자열로(절단).
        log.atInfo()
                .addKeyValue("event", "method.exit")
                .addKeyValue("class", type)
                .addKeyValue("method", method)
                .addKeyValue("return", render(String.valueOf(result)))
                .log("controller method exit");
    }

    /** 마스킹(masker 있으면) 후 절단. */
    private String render(String value) {
        return truncate(masker != null ? masker.mask(value) : value);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated " + (value.length() - maxLength) + " chars)";
    }
}
