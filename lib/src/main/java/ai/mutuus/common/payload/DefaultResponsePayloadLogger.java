package ai.mutuus.common.payload;

import ai.mutuus.common.core.SensitiveDataMasker;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * {@link ResponsePayloadLogger} 기본 구현 — 구조화 JSON 한 줄로 리턴값(응답 본문)을 남긴다.
 * <p>로거 이름은 입력과 동일한 {@code ai.mutuus.common.payload}. 출력 형식을 바꾸려면 이 클래스를
 * 갱신하거나 소비 측에서 {@link ResponsePayloadLogger} 빈을 재정의한다.
 * <p>{@link SensitiveDataMasker}(masking on 시 주입)가 있으면 본문을 남기기 전에 PII 를 마스킹한다.
 */
public class DefaultResponsePayloadLogger implements ResponsePayloadLogger {

    private static final Logger log = LoggerFactory.getLogger(DefaultRequestPayloadLogger.LOGGER_NAME);

    private final SensitiveDataMasker masker;

    public DefaultResponsePayloadLogger() {
        this(null);
    }

    public DefaultResponsePayloadLogger(SensitiveDataMasker masker) {
        this.masker = masker;
    }

    @Override
    public void onResponse(HttpServletRequest request, int status, String body) {
        // 입력과 동일 로거/스타일. 원 요청 정보(method/path)로 입력 로그와 상관관계를 맞춘다.
        LoggingEventBuilder ev = log.atInfo()
                .addKeyValue("event", "response.payload")          // 이벤트 종류(리턴)
                .addKeyValue("httpMethod", request.getMethod())
                .addKeyValue("httpPath", request.getRequestURI())
                .addKeyValue("httpStatus", status);                // 최종 응답 상태코드
        // 응답 본문(정책 적용 후)이 있을 때만 추가. 마스커가 있으면 PII 를 남기기 전에 마스킹.
        if (body != null && !body.isBlank()) {
            ev.addKeyValue("responseBody", masker != null ? masker.mask(body) : body);
        }
        // 같은 요청의 request.payload 와 동일 X-Trace-Id(MDC)로 묶여 한 트랜잭션으로 조회된다.
        ev.log("controller response payload");
    }
}
