package ai.mutuus.common.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * 보안 위배(위협) 이벤트의 단일 진입점 — <b>상세 보안 로그</b>를 일관 스키마로 남긴다.
 * <p>{@link ai.mutuus.common.logging.AccessLogger}(트래픽 로깅)와 <b>분리된</b> 전용 로거
 * ({@value #LOGGER_NAME})로, 각 보안 검사가 위배를 감지하면 여기로 구조화 이벤트를 낸다. SLF4J 2 fluent
 * API({@code addKeyValue})로 필드를 남기고 logstash 인코더가 JSON 으로 렌더한다. {@code traceId}/{@code userId}
 * 등은 이미 MDC 에 있어 자동 포함되므로, 여기서는 이벤트별 추가 컨텍스트만 싣는다.
 *
 * <p><b>공통 스키마</b>: {@code event}(예 {@code security.authn.failed}), {@code securityEvent=true},
 * {@code outcome}(denied/blocked/rejected/suspected), {@code httpPath}, {@code clientIp}, 그리고 이벤트별 필드.
 * <p><b>로그 보안 규율</b>: 원문 JWT/Authorization·비밀번호는 절대 남기지 않는다(주체/사유/클레임명만).
 * 심각도: 잠재 공격=WARN, 확정적 위·변조 시도=ERROR, 설정 위험(startup)=WARN.
 */
public class SecurityAuditLogger {

    public static final String LOGGER_NAME = "ai.mutuus.common.security.audit";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    /** 인증 실패(401) — 토큰 없음/만료/서명 오류 등. */
    public void authnFailed(String path, String clientIp, String reason) {
        base(log.atWarn(), "security.authn.failed", "denied", path, clientIp)
                .addKeyValue("reason", nullToDash(reason))
                .log("Authentication failed");
    }

    /** 인가 거부(403) — 인증은 됐으나 권한 부족. */
    public void authzDenied(String path, String clientIp, String principal, String reason) {
        LoggingEventBuilder ev = base(log.atWarn(), "security.authz.denied", "denied", path, clientIp);
        if (principal != null) {
            ev.addKeyValue("principal", principal);
        }
        ev.addKeyValue("reason", nullToDash(reason)).log("Access denied");
    }

    /** 공통 필드 채움. userId/traceId 는 MDC 경유 자동 포함되므로 여기선 싣지 않는다. */
    private static LoggingEventBuilder base(LoggingEventBuilder ev, String event, String outcome,
                                            String path, String clientIp) {
        ev.addKeyValue("event", event)
                .addKeyValue("securityEvent", true)
                .addKeyValue("outcome", outcome)
                .addKeyValue("httpPath", path);
        if (clientIp != null) {
            ev.addKeyValue("clientIp", clientIp);
        }
        return ev;
    }

    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}
