package ai.mutuus.common.payload;

import ai.mutuus.common.core.SensitiveDataMasker;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * {@link RequestPayloadLogger} 기본 구현 — 구조화 JSON 한 줄로 입력 파라미터를 남긴다.
 * <p>로거 이름 {@code ai.mutuus.common.payload}(레벨 개별 제어 가능). 추적ID/appCode 등은 MDC 로 자동 포함.
 * 출력 필드/형식을 바꾸려면 이 클래스를 갱신하거나 소비 측에서 {@link RequestPayloadLogger} 빈을 재정의한다.
 * <p>{@link SensitiveDataMasker}(masking on 시 주입)가 있으면 본문을 로그에 남기기 전에 PII 를 마스킹한다.
 */
public class DefaultRequestPayloadLogger implements RequestPayloadLogger {

    public static final String LOGGER_NAME = "ai.mutuus.common.payload";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    private final SensitiveDataMasker masker;

    public DefaultRequestPayloadLogger() {
        this(null);
    }

    public DefaultRequestPayloadLogger(SensitiveDataMasker masker) {
        this.masker = masker;
    }

    @Override
    public void onRequest(HttpServletRequest request, String body) {
        // SLF4J 2 fluent API 로 구조화 필드를 쌓는다(logstash 인코더가 각 addKeyValue 를 JSON 필드로 렌더).
        LoggingEventBuilder ev = log.atInfo()
                .addKeyValue("event", "request.payload")           // 이벤트 종류(입력)
                .addKeyValue("httpMethod", request.getMethod())    // GET/POST 등
                .addKeyValue("httpPath", request.getRequestURI()); // 요청 경로
        // 쿼리스트링은 존재할 때만 필드 추가(빈 값 노이즈 방지)
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            ev.addKeyValue("httpQuery", request.getQueryString());
        }
        // 본문 해석의 근거가 되는 Content-Type 을 함께 남긴다
        if (request.getContentType() != null) {
            ev.addKeyValue("contentType", request.getContentType());
        }
        // 본문(필터에서 크기/타입 정책을 적용해 넘어온 값)이 있을 때만 추가. null 이면 필드 자체를 생략.
        // 마스커가 있으면 PII(카드/주민번호 등)를 로그에 남기기 전에 마스킹한다.
        if (body != null && !body.isBlank()) {
            ev.addKeyValue("requestBody", masker != null ? masker.mask(body) : body);
        }
        // 메시지 + 위 필드로 한 줄 출력. 추적ID/appCode/instanceCode 는 MDC 에 있어 자동 포함된다.
        ev.log("controller request payload");
    }
}
