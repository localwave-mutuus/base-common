package ai.mutuus.common.aop;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MethodLoggingWriter} 기본 구현 — 구조화 JSON 한 줄로 인자/리턴을 남긴다.
 * <p>로거 이름 {@code ai.mutuus.common.method}. 추적ID/appCode 등은 MDC 로 자동 포함된다.
 * 값이 길면 {@code maxLength} 로 절단한다. 출력 형식을 바꾸려면 이 클래스를 갱신하거나
 * 소비 측에서 {@link MethodLoggingWriter} 빈을 재정의한다.
 */
public class DefaultMethodLoggingWriter implements MethodLoggingWriter {

    public static final String LOGGER_NAME = "ai.mutuus.common.method";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    private final int maxLength;

    public DefaultMethodLoggingWriter(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void onEnter(String type, String method, Object[] args) {
        // 진입: 역직렬화된 실제 인자 배열을 문자열로(절단). 필터/인터셉터가 못 보는 "메서드 인자"가 여기서 보인다.
        log.atInfo()
                .addKeyValue("event", "method.enter")
                .addKeyValue("class", type)
                .addKeyValue("method", method)
                .addKeyValue("args", truncate(Arrays.deepToString(args)))
                .log("controller method enter");
    }

    @Override
    public void onExit(String type, String method, Object result) {
        // 종료: 리턴 객체(직렬화 전)를 문자열로(절단).
        log.atInfo()
                .addKeyValue("event", "method.exit")
                .addKeyValue("class", type)
                .addKeyValue("method", method)
                .addKeyValue("return", truncate(String.valueOf(result)))
                .log("controller method exit");
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
