package ai.mutuus.common.logging;

import java.util.List;
import java.util.Map;
import java.util.Set;
import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.LogFormat;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/** API 생명주기 로그의 canonical 단일 출력. 신원/trace는 실제 OTel Context snapshot에서 공급한다. */
public class AccessLogger {
    public static final String LOGGER_NAME = "ai.mutuus.common.access";
    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private static final List<String> CAT_WEB = List.of("web");
    private static final List<String> TYPE_ACCESS = List.of("access"), TYPE_ERROR = List.of("error");
    public AccessLogger() { }
    /** 생성자 인자는 호출부 호환용이며 모든 포맷에서 canonical 필드만 출력한다. */
    public AccessLogger(LogFormat format) { }

    /** 보안 scope 종료 전 고정한 문맥으로 요청 완료를 한 번 기록한다. */
    public void serverComplete(String method, String path, String route, int status, long nanos,
                               String errorCode, boolean slow, Map<String, Object> snapshot) {
        var event = status >= 500 || slow ? log.atWarn() : log.atInfo();
        dataset(event, EcsFields.DATASET_ACCESS).addKeyValue("event.action", "http.server.complete")
                .addKeyValue("event.category", CAT_WEB).addKeyValue("event.type", TYPE_ACCESS)
                .addKeyValue("event.outcome", outcome(status)).addKeyValue("event.duration", nanos)
                .addKeyValue("http.request.method", method).addKeyValue("http.response.status_code", status)
                .addKeyValue("url.path", path).addKeyValue(CanonicalLogSnapshot.CONTEXT_KEY, Map.copyOf(snapshot));
        if (route != null) event.addKeyValue("http.route", route);
        if (slow) event.addKeyValue("golmok.slow", true);
        if (hasText(errorCode)) event.addKeyValue("error.code", errorCode);
        if (snapshot.get("error.type") instanceof String type) event.addKeyValue("error.type", type);
        event.log("http.server.complete");
    }
    public void requestReceived(String method, String path, String query, String clientIp) {
        var event = dataset(log.atInfo(), EcsFields.DATASET_ACCESS).addKeyValue("event.action", "request.received")
                .addKeyValue("event.category", CAT_WEB).addKeyValue("event.type", TYPE_ACCESS)
                .addKeyValue("http.request.method", method).addKeyValue("url.path", path);
        // Raw query values are never emitted by the access facade.
        if (clientIp != null) event.addKeyValue("client.ip", clientIp);
        event.log("API request received");
    }
    public void requestCompleted(String method, String path, int status, long durationMillis,
                                 long durationNanos, String clientIp, boolean slow) {
        var event = dataset(status >= 500 || slow ? log.atWarn() : log.atInfo(), EcsFields.DATASET_ACCESS)
                .addKeyValue("event.action", "request.completed").addKeyValue("event.category", CAT_WEB)
                .addKeyValue("event.type", TYPE_ACCESS).addKeyValue("event.outcome", outcome(status))
                .addKeyValue("http.request.method", method).addKeyValue("url.path", path)
                .addKeyValue("http.response.status_code", status).addKeyValue("event.duration", durationNanos);
        if (clientIp != null) event.addKeyValue("client.ip", clientIp);
        if (slow) event.addKeyValue("golmok.slow", true);
        event.log("API request completed");
    }
    public void httpPair(String action, String requestId, String traceId, String method, String path,
                         int status, long nanos, String errorCode, String user, boolean slow, String query) {
        httpPair(action, requestId, traceId, method, path, status, nanos, errorCode, user, slow, query, Map.of());
    }
    public void httpPair(String action, String requestId, String traceId, String method, String path,
                         int status, long nanos, String errorCode, String user, boolean slow, String query, Map<String, String> screen) {
        httpPair(action, requestId, traceId, method, path, status, nanos, errorCode, user, slow, query, screen, null, null);
    }
    public void httpPair(String action, String requestId, String traceId, String method, String path,
                         int status, long nanos, String errorCode, String user, boolean slow, String query,
                         Map<String, String> screen, String spanId, String parentId) {
        var event = dataset(status >= 500 || slow ? log.atWarn() : log.atInfo(), EcsFields.DATASET_ACCESS)
                .addKeyValue("event.action", action).addKeyValue("golmok.request.id", requestId)
                .addKeyValue("http.request.method", method).addKeyValue("url.path", path)
                .addKeyValue("http.response.status_code", status).addKeyValue("event.duration", nanos)
                .addKeyValue("event.outcome", hasText(errorCode) ? "failure" : "success")
                .addKeyValue("event.category", CAT_WEB).addKeyValue("event.type", TYPE_ACCESS);
        if (hasText(errorCode)) event.addKeyValue("error.code", errorCode);
        // Raw query values are never emitted by the access facade.
        if (slow) event.addKeyValue("golmok.slow", true);
        // 구 인자의 trace/span/user는 신뢰하지 않는다. 실제 OTel/검증 사용자 Context만 출력한다.
        ai.mutuus.common.core.ScreenMetadata.fromHeaders(screen::get).forEach((key, value) ->
                event.addKeyValue(ai.mutuus.common.core.ScreenMetadata.FIELDS.get(key), value));
        event.log(action);
    }
    public void authFailure(String path, String reason) { authFailure(path, reason, null); }
    public void authFailure(String path, String reason, Throwable exception) {
        authFailure(path, reason, exception, SecurityAuditLogger.authenticationErrorCode(exception));
    }
    public void authFailure(String path, String reason, Throwable exception, String errorCode) {
        var event = failure(EcsFields.DATASET_ACCESS, "auth.failure", path)
                .addKeyValue(EcsFields.ERROR_CODE, errorCode);
        exceptionType(event, exception);
        if (hasText(reason)) event.addKeyValue("golmok.security.reason", reason);
        event.log("Authentication failed");
    }
    public void accessDenied(String path, String reason) { accessDenied(path, reason, null); }
    public void accessDenied(String path, String reason, Throwable exception) {
        var event = failure(EcsFields.DATASET_ACCESS, "auth.denied", path);
        exceptionType(event, exception);
        if (hasText(reason)) event.addKeyValue("golmok.security.reason", reason);
        event.log("Access denied");
    }
    public void businessError(String errorCode, String path, String detail) { businessError(errorCode, path, detail, null); }
    /** 처리된 예외는 클래스만 추가한다. 새 cause/스택이나 응답 정보를 노출하지 않는다. */
    public void businessError(String errorCode, String path, String detail, Throwable exception) {
        var event = failure(EcsFields.DATASET_ERROR, "error.business", path)
                .addKeyValue("event.category", CAT_WEB).addKeyValue("event.type", TYPE_ERROR)
                .addKeyValue("error.code", errorCode).addKeyValue("error.message", detail);
        exceptionType(event, exception); event.log("Business error");
    }
    public void serverError(String path, Throwable ex) { serverError(path, null, ex); }
    /** 기존 서버 오류의 cause/스택 정책은 그대로 유지한다. */
    public void serverError(String path, String errorCode, Throwable ex) {
        var event = dataset(log.atError(), EcsFields.DATASET_ERROR).addKeyValue("event.action", "error.server")
                .addKeyValue("event.category", CAT_WEB).addKeyValue("event.type", TYPE_ERROR)
                .addKeyValue("event.outcome", "failure").addKeyValue("error.type", ex.getClass().getName()).addKeyValue("url.path", path);
        if (hasText(errorCode)) event.addKeyValue("error.code", errorCode);
        event.setCause(ex).log("Unhandled server error");
    }
    private static LoggingEventBuilder failure(String dataset, String action, String path) {
        return dataset(log.atWarn(), dataset).addKeyValue("event.action", action)
                .addKeyValue("event.outcome", "failure").addKeyValue("url.path", path);
    }
    private static void exceptionType(LoggingEventBuilder event, Throwable exception) {
        if (exception != null) event.addKeyValue("error.type", exception.getClass().getName());
    }
    private static LoggingEventBuilder dataset(LoggingEventBuilder event, String dataset) { return event.addKeyValue("event.dataset", dataset); }
    private static String outcome(int status) { return status >= 400 ? "failure" : "success"; }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}