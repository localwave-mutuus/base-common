package ai.mutuus.common.security.audit;

import java.util.List;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.LogFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/** 보안 이벤트를 canonical 단일 schema로 기록한다. 사용자/trace는 검증된 발생 시점 Context에서 공급한다.
 * <p>원문 JWT/Authorization·비밀번호는 남기지 않으며 기존 마스킹·심각도 정책을 유지한다.
 */public class SecurityAuditLogger {

    private static LoggingEventBuilder exceptionType(LoggingEventBuilder event, Throwable exception) {
        if (exception != null) event.addKeyValue(EcsFields.ERROR_TYPE, exception.getClass().getName());
        return event;
    }

    public static final String LOGGER_NAME = "ai.mutuus.common.security.audit";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private static final List<String> CAT_AUTH = List.of("authentication", "iam");
    private static final List<String> CAT_NETWORK = List.of("network");
    private static final List<String> CAT_CONFIG = List.of("configuration");
    private static final List<String> TYPE_DENIED = List.of("denied");
    private static final List<String> TYPE_INFO = List.of("info");

    public SecurityAuditLogger() {
        // canonical 단일 출력.
    }

    public SecurityAuditLogger(LogFormat format) {
        // 생성자 인자는 소스/바이너리 호환용이며 출력은 canonical 한 가지다.
    }

    /** 인증 실패(401) — 토큰 없음 등(토큰 검증 실패는 {@link #jwtRejected}). */
    public void authnFailed(String path, String clientIp, String reason) {
        authnFailed(path, clientIp, reason, null);
    }
    /** 실제 예외가 있는 보안 처리 경로에서 클래스만 추가한다. */
    public void authnFailed(String path, String clientIp, String reason, Throwable exception) {
        authnFailed(path, clientIp, reason, exception, authenticationErrorCode(exception));
    }
    public void authnFailed(String path, String clientIp, String reason, Throwable exception, String errorCode) {
        exceptionType(base(log.atWarn(), "security.authn.failed", "denied", path, clientIp, CAT_AUTH), exception)
                .addKeyValue("golmok.security.reason", nullToDash(reason))
                .addKeyValue(EcsFields.ERROR_CODE, errorCode)
                .log("Authentication failed");
    }

    /**
     * JWT <b>토큰 검증 실패</b>(서명/만료/issuer/<b>audience</b> 등) — 401. {@code errorCode}(예 invalid_token)와
     * 사유 설명을 남긴다(원문 토큰은 절대 남기지 않는다).
     */
    public void jwtRejected(String path, String clientIp, String errorCode, String description) {
        jwtRejected(path, clientIp, errorCode, description, null);
    }
    public void jwtRejected(String path, String clientIp, String errorCode, String description, Throwable exception) {
        exceptionType(base(log.atWarn(), "security.jwt.rejected", "rejected", path, clientIp, CAT_AUTH), exception)
                .addKeyValue("error.code", nullToDash(errorCode))
                .addKeyValue("golmok.security.reason", nullToDash(description))
                .log("JWT rejected");
    }

    /** 인가 거부(403) — 인증은 됐으나 권한 부족. */
    public void authzDenied(String path, String clientIp, String principal, String reason) {
        authzDenied(path, clientIp, principal, reason, null);
    }
    public void authzDenied(String path, String clientIp, String principal, String reason, Throwable exception) {
        LoggingEventBuilder ev = exceptionType(base(log.atWarn(), "security.authz.denied", "denied", path, clientIp, CAT_AUTH), exception);
        if (principal != null) {
            ev.addKeyValue("golmok.security.principal", principal);
        }
        ev.addKeyValue("golmok.security.reason", nullToDash(reason)).log("Access denied");
    }

    /**
     * 인입 {@code X-User-Id}(위장 가능한 신뢰 불가 헤더)를 <b>폐기</b>했다 — 신뢰 프록시 경유가 아니고 인증도 없어,
     * 이 주장 사용자로는 감사/로그를 오염시키지 않는다(감사 위조 방지). claimedUserId 는 마지막 4자만 남긴다.
     */
    public void identityHeaderIgnored(String path, String clientIp, String claimedUserId) {
        base(log.atWarn(), "security.identity.header_ignored", "rejected", path, clientIp, CAT_AUTH)
                .addKeyValue("golmok.security.claimed_user_id", tail(claimedUserId))
                .log("Ignored untrusted X-User-Id header");
    }

    /**
     * 인입 {@code X-User-Id} 주장값이 <b>검증된 인증 주체와 다름</b> — 신원 위·변조 시도로 보고 ERROR 로 남긴다.
     */
    public void identityMismatch(String path, String clientIp, String claimedUserId, String authenticatedUserId) {
        base(log.atError(), "security.identity.mismatch", "suspected", path, clientIp, CAT_AUTH)
                .addKeyValue("golmok.security.claimed_user_id", tail(claimedUserId))
                .addKeyValue("golmok.security.authenticated_user_id", authenticatedUserId)
                .log("Claimed X-User-Id does not match authenticated principal");
    }

    /**
     * 아웃바운드 호출에서 <b>식별/민감 헤더 전파를 차단</b>했다 — 대상이 신뢰 호스트 allowlist 밖이라 외부 유출을 막았다.
     * (상관용 trace/span/locale 은 그대로 전파)
     */
    public void propagationBlocked(String targetHost, String droppedHeaders) {
        LoggingEventBuilder ev = base(log.atWarn(), "security.propagation.blocked", "blocked", null, null, CAT_NETWORK);
        ev.addKeyValue(EcsFields.ERROR_CODE, "AUTH_PROPAGATION_BLOCKED")
                .addKeyValue("golmok.security.target_host", nullToDash(targetHost))
                .addKeyValue("golmok.security.dropped_headers", nullToDash(droppedHeaders))
                .log("Blocked identity header propagation to untrusted host");
    }

    /**
     * 인증은 됐으나 <b>부여된 권한(role)이 없음</b> — roles 클레임이 비었거나 클레임 경로가 어긋난 것(→ 사실상 모든
     * 보호 리소스 403, fail-closed). 진단을 돕도록 주체와 시도한 클레임 경로를 남긴다.
     */
    public void authzNoAuthorities(String subject, String rolesClaimPath) {
        LoggingEventBuilder ev = base(log.atWarn(), "security.authz.no_authorities", "warn", null, null, CAT_AUTH);
        ev.addKeyValue("golmok.security.subject", nullToDash(subject))
                .addKeyValue("golmok.security.roles_claim_path", nullToDash(rolesClaimPath))
                .log("Authenticated principal has no granted authorities");
    }

    /** 시작 시 <b>보안 설정 위험</b> 탐지(예: 본문 로깅+마스킹 off, CSRF+세션, permit-all 과대). */
    public void configRisk(String event, String detail) {
        LoggingEventBuilder ev = base(log.atWarn(), event, "warn", null, null, CAT_CONFIG);
        ev.addKeyValue("golmok.security.detail", nullToDash(detail))
                .log("Security configuration risk");
    }

    /**
     * 공통 필드는 canonical 이름으로만 기록한다. 검증된 사용자/trace는 발생 시점 Context에서 공급한다.
     * disposition(denied/blocked/rejected/suspected/warn)은
     * {@code mutuus.security.disposition} 에 보존하고, {@code event.outcome} 은 failure/unknown 으로 정규화한다.
     */
    private LoggingEventBuilder base(LoggingEventBuilder ev, String action, String disposition,
                                     String path, String clientIp, List<String> category) {

            ev.addKeyValue(EcsFields.EVENT_DATASET, EcsFields.DATASET_SECURITY_AUDIT)

                    .addKeyValue(EcsFields.EVENT_ACTION, action)
                    .addKeyValue(EcsFields.EVENT_CATEGORY, category)
                    .addKeyValue(EcsFields.EVENT_TYPE, "warn".equals(disposition) ? TYPE_INFO : TYPE_DENIED)
                    .addKeyValue(EcsFields.EVENT_OUTCOME,
                            "warn".equals(disposition) ? EcsFields.OUTCOME_UNKNOWN : EcsFields.OUTCOME_FAILURE)
                    .addKeyValue(EcsFields.MUTUUS_SECURITY_DISPOSITION, disposition)
                    .addKeyValue("golmok.security.event", true);
            if (path != null) {
                ev.addKeyValue(EcsFields.URL_PATH, path);
            }
            if (clientIp != null) {
                ev.addKeyValue(EcsFields.CLIENT_IP, clientIp);
            }

        return ev;
    }

    /** 로그 분류만 수행한다. 일반 메시지는 추측하지 않고 표준 JWT 검증 근거만 사용한다. */
    public static String authenticationErrorCode(Throwable exception) {
        if (exception instanceof AuthenticationCredentialsNotFoundException) return "AUTH_TOKEN_MISSING";
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof JwtValidationException jwt && jwt.getErrors().stream()
                    .anyMatch(error -> error.getDescription() != null
                            && error.getDescription().startsWith("Jwt expired at "))) {
                return "AUTH_TOKEN_EXPIRED";
            }
        }
        if (exception instanceof OAuth2AuthenticationException oauth
                && "invalid_token".equals(oauth.getError().getErrorCode())) return "AUTH_TOKEN_INVALID";
        return "AUTHN_FAILED";
    }

    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    /** 사용자 식별자를 마지막 4자만 남기고 마스킹(로그의 PII 최소화). */
    private static String tail(String s) {
        if (s == null || s.isBlank()) {
            return "-";
        }
        int keep = 4;
        return s.length() <= keep ? "*".repeat(s.length()) : "*".repeat(s.length() - keep) + s.substring(s.length() - keep);
    }
}
