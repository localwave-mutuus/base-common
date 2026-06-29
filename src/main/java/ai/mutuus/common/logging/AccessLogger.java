package ai.mutuus.common.logging;

import ai.mutuus.common.core.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * API 생명주기 횡단 로깅의 단일 진입점.
 * <p>소비 서비스는 이 클래스를 직접 호출하지 않는다 — 라이브러리의 필터/보안 핸들러/예외 핸들러가
 * 적절한 시점에 호출한다. SLF4J 2 의 fluent API({@code addKeyValue})로 구조화 필드를 남기며,
 * logstash 인코더가 이를 JSON 필드로 렌더한다. 추적ID/사용자 등은 이미 MDC 에 있어 자동 포함된다.
 * <p>로거 이름은 {@code ai.mutuus.common.access} — 소비 측에서 레벨을 개별 제어할 수 있다.
 */
public class AccessLogger {

    public static final String LOGGER_NAME = "ai.mutuus.common.access";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    /** 요청 수신(핸들러 진입 전). */
    public void requestReceived(String method, String path, String query, String clientIp) {
        LoggingEventBuilder ev = log.atInfo()
                .addKeyValue("event", "request.received")
                .addKeyValue("httpMethod", method)
                .addKeyValue("httpPath", path);
        if (query != null && !query.isBlank()) {
            ev.addKeyValue("httpQuery", query);
        }
        if (clientIp != null) {
            ev.addKeyValue("clientIp", clientIp);
        }
        addUser(ev).log("API request received");
    }

    /** 응답 완료(상태/소요시간). 5xx 또는 느린 요청은 WARN. */
    public void requestCompleted(String method, String path, int status, long durationMillis, boolean slow) {
        LoggingEventBuilder ev = (status >= 500 || slow) ? log.atWarn() : log.atInfo();
        ev.addKeyValue("event", "request.completed")
                .addKeyValue("httpMethod", method)
                .addKeyValue("httpPath", path)
                .addKeyValue("httpStatus", status)
                .addKeyValue("durationMs", durationMillis);
        if (slow) {
            ev.addKeyValue("slow", true);
        }
        addUser(ev).log("API request completed");
    }

    /** 인증 실패(401) — 토큰 없음/만료/위조 등. */
    public void authFailure(String path, String reason) {
        log.atWarn()
                .addKeyValue("event", "auth.failure")
                .addKeyValue("httpPath", path)
                .addKeyValue("reason", reason)
                .log("Authentication failed");
    }

    /** 인가 거부(403) — 인증은 됐으나 권한 부족. */
    public void accessDenied(String path, String reason) {
        addUser(log.atWarn()
                .addKeyValue("event", "auth.denied")
                .addKeyValue("httpPath", path)
                .addKeyValue("reason", reason))
                .log("Access denied");
    }

    /** 비즈니스 예외(4xx) — 정상 흐름의 예상된 예외. */
    public void businessError(String errorCode, String path, String detail) {
        addUser(log.atWarn()
                .addKeyValue("event", "error.business")
                .addKeyValue("errorCode", errorCode)
                .addKeyValue("httpPath", path)
                .addKeyValue("detail", detail))
                .log("Business error");
    }

    /** 처리되지 않은 서버 예외(5xx) — 스택트레이스 포함. */
    public void serverError(String path, Throwable ex) {
        addUser(log.atError()
                .addKeyValue("event", "error.server")
                .addKeyValue("httpPath", path)
                .addKeyValue("exception", ex.getClass().getName())
                .setCause(ex))
                .log("Unhandled server error");
    }

    private LoggingEventBuilder addUser(LoggingEventBuilder ev) {
        String user = TraceContext.userId();
        if (user != null) {
            ev.addKeyValue("userId", user);
        }
        return ev;
    }
}
