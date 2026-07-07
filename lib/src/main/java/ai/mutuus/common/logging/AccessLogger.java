package ai.mutuus.common.logging;

import java.util.List;

import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.LogFormat;
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
 * <p><b>ECS 전환</b>: {@link LogFormat} 에 따라 기존 필드/ECS 필드/둘 다({@code DUAL}, 기본)를 남긴다.
 * 요청 접근/응답은 {@code mutuus.access}, 예외는 {@code mutuus.error} dataset 으로 분리한다(로거가 직접 명시).
 */
public class AccessLogger {

    public static final String LOGGER_NAME = "ai.mutuus.common.access";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private static final List<String> CAT_WEB = List.of("web");
    private static final List<String> TYPE_ACCESS = List.of("access");
    private static final List<String> TYPE_ERROR = List.of("error");

    private final LogFormat format;

    public AccessLogger() {
        this(LogFormat.DUAL);
    }

    public AccessLogger(LogFormat format) {
        this.format = format == null ? LogFormat.DUAL : format;
    }

    /** 요청 수신(핸들러 진입 전). 호출 시스템/단말 정보(where)를 함께 남긴다. */
    public void requestReceived(String method, String path, String query, String clientIp) {
        LoggingEventBuilder ev = log.atInfo();
        if (format.legacy()) {
            ev.addKeyValue("event", "request.received")
                    .addKeyValue("httpMethod", method)
                    .addKeyValue("httpPath", path);
            if (hasText(query)) {
                ev.addKeyValue("httpQuery", query);
            }
            if (clientIp != null) {
                ev.addKeyValue("clientIp", clientIp);
            }
        }
        if (format.ecs()) {
            dataset(ev, EcsFields.DATASET_ACCESS)
                    .addKeyValue(EcsFields.EVENT_ACTION, "request.received")
                    .addKeyValue(EcsFields.EVENT_CATEGORY, CAT_WEB)
                    .addKeyValue(EcsFields.EVENT_TYPE, TYPE_ACCESS)
                    .addKeyValue(EcsFields.HTTP_REQUEST_METHOD, method)
                    .addKeyValue(EcsFields.URL_PATH, path);
            if (hasText(query)) {
                ev.addKeyValue(EcsFields.URL_QUERY, query);
            }
            if (clientIp != null) {
                ev.addKeyValue(EcsFields.CLIENT_IP, clientIp);
            }
        }
        addDevice(addUser(ev)).log("API request received");
    }

    /**
     * 응답 완료(상태/소요시간). 5xx 또는 느린 요청은 WARN. 운영 대시보드의 중심 이벤트로,
     * {@code client.ip}·{@code event.outcome}·{@code event.duration}(nanos)까지 남긴다.
     */
    public void requestCompleted(String method, String path, int status, long durationMillis,
                                 long durationNanos, String clientIp, boolean slow) {
        LoggingEventBuilder ev = (status >= 500 || slow) ? log.atWarn() : log.atInfo();
        if (format.legacy()) {
            ev.addKeyValue("event", "request.completed")
                    .addKeyValue("httpMethod", method)
                    .addKeyValue("httpPath", path)
                    .addKeyValue("httpStatus", status)
                    .addKeyValue("durationMs", durationMillis);
            if (slow) {
                ev.addKeyValue("slow", true);
            }
        }
        if (format.ecs()) {
            dataset(ev, EcsFields.DATASET_ACCESS)
                    .addKeyValue(EcsFields.EVENT_ACTION, "request.completed")
                    .addKeyValue(EcsFields.EVENT_CATEGORY, CAT_WEB)
                    .addKeyValue(EcsFields.EVENT_TYPE, TYPE_ACCESS)
                    .addKeyValue(EcsFields.EVENT_OUTCOME, outcome(status))
                    .addKeyValue(EcsFields.HTTP_REQUEST_METHOD, method)
                    .addKeyValue(EcsFields.URL_PATH, path)
                    .addKeyValue(EcsFields.HTTP_RESPONSE_STATUS_CODE, status)
                    .addKeyValue(EcsFields.EVENT_DURATION, durationNanos);
            if (clientIp != null) {
                ev.addKeyValue(EcsFields.CLIENT_IP, clientIp);
            }
        }
        addUser(ev).log("API request completed");
    }

    /** 인증 실패(401) — 토큰 없음/만료/위조 등. */
    public void authFailure(String path, String reason) {
        LoggingEventBuilder ev = log.atWarn();
        if (format.legacy()) {
            ev.addKeyValue("event", "auth.failure")
                    .addKeyValue("httpPath", path)
                    .addKeyValue("reason", reason);
        }
        if (format.ecs()) {
            dataset(ev, EcsFields.DATASET_ACCESS)
                    .addKeyValue(EcsFields.EVENT_ACTION, "auth.failure")
                    .addKeyValue(EcsFields.EVENT_OUTCOME, EcsFields.OUTCOME_FAILURE)
                    .addKeyValue(EcsFields.URL_PATH, path);
        }
        ev.log("Authentication failed");
    }

    /** 인가 거부(403) — 인증은 됐으나 권한 부족. */
    public void accessDenied(String path, String reason) {
        LoggingEventBuilder ev = log.atWarn();
        if (format.legacy()) {
            ev.addKeyValue("event", "auth.denied")
                    .addKeyValue("httpPath", path)
                    .addKeyValue("reason", reason);
        }
        if (format.ecs()) {
            dataset(ev, EcsFields.DATASET_ACCESS)
                    .addKeyValue(EcsFields.EVENT_ACTION, "auth.denied")
                    .addKeyValue(EcsFields.EVENT_OUTCOME, EcsFields.OUTCOME_FAILURE)
                    .addKeyValue(EcsFields.URL_PATH, path);
        }
        addUser(ev).log("Access denied");
    }

    /** 비즈니스 예외(4xx) — 정상 흐름의 예상된 예외. {@code mutuus.error} dataset. */
    public void businessError(String errorCode, String path, String detail) {
        LoggingEventBuilder ev = log.atWarn();
        if (format.legacy()) {
            ev.addKeyValue("event", "error.business")
                    .addKeyValue("errorCode", errorCode)
                    .addKeyValue("httpPath", path)
                    .addKeyValue("detail", detail);
        }
        if (format.ecs()) {
            dataset(ev, EcsFields.DATASET_ERROR)
                    .addKeyValue(EcsFields.EVENT_ACTION, "error.business")
                    .addKeyValue(EcsFields.EVENT_CATEGORY, CAT_WEB)
                    .addKeyValue(EcsFields.EVENT_TYPE, TYPE_ERROR)
                    .addKeyValue(EcsFields.EVENT_OUTCOME, EcsFields.OUTCOME_FAILURE)
                    .addKeyValue(EcsFields.ERROR_CODE, errorCode)
                    .addKeyValue(EcsFields.ERROR_MESSAGE, detail)
                    .addKeyValue(EcsFields.URL_PATH, path);
        }
        addUser(ev).log("Business error");
    }

    /** 처리되지 않은 서버 예외(5xx) — 스택트레이스 포함. */
    public void serverError(String path, Throwable ex) {
        serverError(path, null, ex);
    }

    /** 처리되지 않은 서버 예외(5xx) — 오류 코드/타입/스택트레이스 포함. {@code mutuus.error} dataset. */
    public void serverError(String path, String errorCode, Throwable ex) {
        LoggingEventBuilder ev = log.atError();
        if (format.legacy()) {
            ev.addKeyValue("event", "error.server")
                    .addKeyValue("httpPath", path)
                    .addKeyValue("exception", ex.getClass().getName());
        }
        if (format.ecs()) {
            dataset(ev, EcsFields.DATASET_ERROR)
                    .addKeyValue(EcsFields.EVENT_ACTION, "error.server")
                    .addKeyValue(EcsFields.EVENT_CATEGORY, CAT_WEB)
                    .addKeyValue(EcsFields.EVENT_TYPE, TYPE_ERROR)
                    .addKeyValue(EcsFields.EVENT_OUTCOME, EcsFields.OUTCOME_FAILURE)
                    .addKeyValue(EcsFields.ERROR_TYPE, ex.getClass().getName())
                    .addKeyValue(EcsFields.URL_PATH, path);
            if (hasText(errorCode)) {
                ev.addKeyValue(EcsFields.ERROR_CODE, errorCode);
            }
        }
        // 스택트레이스는 인코더가 렌더한다(logstash 기본 stack_trace → ingest 에서 error.stack_trace 로 정규화).
        addUser(ev).setCause(ex).log("Unhandled server error");
    }

    /** {@code event.dataset} + {@code data_stream.dataset}(수집기 라우팅) 을 함께 명시(dataset per-logger). */
    private static LoggingEventBuilder dataset(LoggingEventBuilder ev, String dataset) {
        return ev.addKeyValue(EcsFields.EVENT_DATASET, dataset)
                .addKeyValue(EcsFields.DATA_STREAM_DATASET, dataset);
    }

    /** HTTP 상태코드 → ECS outcome(4xx/5xx=failure). */
    private static String outcome(int status) {
        return status >= 400 ? EcsFields.OUTCOME_FAILURE : EcsFields.OUTCOME_SUCCESS;
    }

    /** 사용자 식별(legacy {@code userId}). ECS {@code user.id} 는 MDC alias 로 자동 포함되므로 legacy 모드에서만 싣는다. */
    private LoggingEventBuilder addUser(LoggingEventBuilder ev) {
        if (format.legacy()) {
            String user = TraceContext.userId();
            if (user != null) {
                ev.addKeyValue("userId", user);
            }
        }
        return ev;
    }

    /** 호출 시스템/단말 식별(where) — legacy 필드(ECS 표준 외라 유지). */
    private LoggingEventBuilder addDevice(LoggingEventBuilder ev) {
        if (format.legacy()) {
            TraceContext.get(HeaderNames.DEVICE_LEVEL).ifPresent(v -> ev.addKeyValue("deviceLevel", v));
            TraceContext.get(HeaderNames.DEVICE_ID).ifPresent(v -> ev.addKeyValue("deviceId", v));
        }
        return ev;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
