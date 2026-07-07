package ai.mutuus.common.security.audit;

import java.util.List;

import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.LogFormat;
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
 * <p><b>ECS 전환</b>: {@link LogFormat} 에 따라 기존 필드/ECS 필드/둘 다({@code DUAL}, 기본)를 남긴다.
 * ECS 는 {@code mutuus.security_audit} dataset, {@code event.category=[authentication,iam]},
 * {@code event.outcome=failure}(위협)로 정규화하고, 기존의 세분값(denied/blocked/rejected/suspected)은
 * {@code mutuus.security.disposition} 으로 보존한다. 요청자 IP 는 {@code client.ip} 로 통일한다.
 * <p><b>로그 보안 규율</b>: 원문 JWT/Authorization·비밀번호는 절대 남기지 않는다(주체/사유/클레임명만).
 * 심각도: 잠재 공격=WARN, 확정적 위·변조 시도=ERROR, 설정 위험(startup)=WARN.
 */
public class SecurityAuditLogger {

    public static final String LOGGER_NAME = "ai.mutuus.common.security.audit";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private static final List<String> CAT_AUTH = List.of("authentication", "iam");
    private static final List<String> CAT_NETWORK = List.of("network");
    private static final List<String> CAT_CONFIG = List.of("configuration");
    private static final List<String> TYPE_DENIED = List.of("denied");
    private static final List<String> TYPE_INFO = List.of("info");

    private final LogFormat format;

    public SecurityAuditLogger() {
        this(LogFormat.DUAL);
    }

    public SecurityAuditLogger(LogFormat format) {
        this.format = format == null ? LogFormat.DUAL : format;
    }

    /** 인증 실패(401) — 토큰 없음 등(토큰 검증 실패는 {@link #jwtRejected}). */
    public void authnFailed(String path, String clientIp, String reason) {
        base(log.atWarn(), "security.authn.failed", "denied", path, clientIp, CAT_AUTH)
                .addKeyValue("reason", nullToDash(reason))
                .log("Authentication failed");
    }

    /**
     * JWT <b>토큰 검증 실패</b>(서명/만료/issuer/<b>audience</b> 등) — 401. {@code errorCode}(예 invalid_token)와
     * 사유 설명을 남긴다(원문 토큰은 절대 남기지 않는다).
     */
    public void jwtRejected(String path, String clientIp, String errorCode, String description) {
        base(log.atWarn(), "security.jwt.rejected", "rejected", path, clientIp, CAT_AUTH)
                .addKeyValue("errorCode", nullToDash(errorCode))
                .addKeyValue("reason", nullToDash(description))
                .log("JWT rejected");
    }

    /** 인가 거부(403) — 인증은 됐으나 권한 부족. */
    public void authzDenied(String path, String clientIp, String principal, String reason) {
        LoggingEventBuilder ev = base(log.atWarn(), "security.authz.denied", "denied", path, clientIp, CAT_AUTH);
        if (principal != null) {
            ev.addKeyValue("principal", principal);
        }
        ev.addKeyValue("reason", nullToDash(reason)).log("Access denied");
    }

    /**
     * 인입 {@code X-User-Id}(위장 가능한 신뢰 불가 헤더)를 <b>폐기</b>했다 — 신뢰 프록시 경유가 아니고 인증도 없어,
     * 이 주장 사용자로는 감사/로그를 오염시키지 않는다(감사 위조 방지). claimedUserId 는 마지막 4자만 남긴다.
     */
    public void identityHeaderIgnored(String path, String clientIp, String claimedUserId) {
        base(log.atWarn(), "security.identity.header_ignored", "rejected", path, clientIp, CAT_AUTH)
                .addKeyValue("claimedUserId", tail(claimedUserId))
                .log("Ignored untrusted X-User-Id header");
    }

    /**
     * 인입 {@code X-User-Id} 주장값이 <b>검증된 인증 주체와 다름</b> — 신원 위·변조 시도로 보고 ERROR 로 남긴다.
     */
    public void identityMismatch(String path, String clientIp, String claimedUserId, String authenticatedUserId) {
        base(log.atError(), "security.identity.mismatch", "suspected", path, clientIp, CAT_AUTH)
                .addKeyValue("claimedUserId", tail(claimedUserId))
                .addKeyValue("authenticatedUserId", authenticatedUserId)
                .log("Claimed X-User-Id does not match authenticated principal");
    }

    /**
     * 아웃바운드 호출에서 <b>식별/민감 헤더 전파를 차단</b>했다 — 대상이 신뢰 호스트 allowlist 밖이라 외부 유출을 막았다.
     * (상관용 trace/span/locale 은 그대로 전파)
     */
    public void propagationBlocked(String targetHost, String droppedHeaders) {
        LoggingEventBuilder ev = base(log.atWarn(), "security.propagation.blocked", "blocked", null, null, CAT_NETWORK);
        ev.addKeyValue("targetHost", nullToDash(targetHost))
                .addKeyValue("droppedHeaders", nullToDash(droppedHeaders))
                .log("Blocked identity header propagation to untrusted host");
    }

    /**
     * 인증은 됐으나 <b>부여된 권한(role)이 없음</b> — roles 클레임이 비었거나 클레임 경로가 어긋난 것(→ 사실상 모든
     * 보호 리소스 403, fail-closed). 진단을 돕도록 주체와 시도한 클레임 경로를 남긴다.
     */
    public void authzNoAuthorities(String subject, String rolesClaimPath) {
        LoggingEventBuilder ev = base(log.atWarn(), "security.authz.no_authorities", "warn", null, null, CAT_AUTH);
        ev.addKeyValue("subject", nullToDash(subject))
                .addKeyValue("rolesClaimPath", nullToDash(rolesClaimPath))
                .log("Authenticated principal has no granted authorities");
    }

    /** 시작 시 <b>보안 설정 위험</b> 탐지(예: 본문 로깅+마스킹 off, CSRF+세션, permit-all 과대). */
    public void configRisk(String event, String detail) {
        LoggingEventBuilder ev = base(log.atWarn(), event, "warn", null, null, CAT_CONFIG);
        ev.addKeyValue("detail", nullToDash(detail))
                .log("Security configuration risk");
    }

    /**
     * 공통 필드 채움(dual). userId/traceId 는 MDC 경유 자동 포함되므로 여기선 싣지 않는다.
     * disposition(denied/blocked/rejected/suspected/warn)은 legacy {@code outcome} 및 ECS
     * {@code mutuus.security.disposition} 에 보존하고, ECS {@code event.outcome} 은 failure/unknown 으로 정규화한다.
     */
    private LoggingEventBuilder base(LoggingEventBuilder ev, String action, String disposition,
                                     String path, String clientIp, List<String> category) {
        if (format.legacy()) {
            ev.addKeyValue("event", action)
                    .addKeyValue("securityEvent", true)
                    .addKeyValue("outcome", disposition);
            if (path != null) {
                ev.addKeyValue("httpPath", path);
            }
            if (clientIp != null) {
                ev.addKeyValue("clientIp", clientIp);
            }
        }
        if (format.ecs()) {
            ev.addKeyValue(EcsFields.EVENT_DATASET, EcsFields.DATASET_SECURITY_AUDIT)
                    .addKeyValue(EcsFields.DATA_STREAM_DATASET, EcsFields.DATASET_SECURITY_AUDIT)
                    .addKeyValue(EcsFields.EVENT_ACTION, action)
                    .addKeyValue(EcsFields.EVENT_CATEGORY, category)
                    .addKeyValue(EcsFields.EVENT_TYPE, "warn".equals(disposition) ? TYPE_INFO : TYPE_DENIED)
                    .addKeyValue(EcsFields.EVENT_OUTCOME,
                            "warn".equals(disposition) ? EcsFields.OUTCOME_UNKNOWN : EcsFields.OUTCOME_FAILURE)
                    .addKeyValue(EcsFields.MUTUUS_SECURITY_DISPOSITION, disposition);
            if (path != null) {
                ev.addKeyValue(EcsFields.URL_PATH, path);
            }
            if (clientIp != null) {
                ev.addKeyValue(EcsFields.CLIENT_IP, clientIp);
            }
        }
        return ev;
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
