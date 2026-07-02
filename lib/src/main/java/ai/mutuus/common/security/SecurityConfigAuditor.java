package ai.mutuus.common.security;

import java.util.List;

import ai.mutuus.common.security.audit.SecurityAuditLogger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * 시작(ApplicationReadyEvent) 시 <b>보안 설정 위험 조합</b>을 1회 점검해 경고 이벤트를 남긴다 — 운영 오설정을
 * 조기에 드러낸다. 응답/동작은 바꾸지 않고 로그만 남긴다.
 * <ul>
 *   <li><b>본문 로깅 + 마스킹 off</b> → {@code security.config.masking_disabled}(자격/PII 평문 로깅 위험)</li>
 *   <li><b>세션(쿠키) 사용 + CSRF 비활성</b> → {@code security.config.csrf_disabled_with_session}</li>
 *   <li><b>permit-all 과대</b>({@code /**}·{@code /*}) → {@code security.config.permit_all_broad}</li>
 * </ul>
 */
public class SecurityConfigAuditor implements ApplicationListener<ApplicationReadyEvent> {

    private final SecurityAuditLogger audit;
    private final CommonSecurityProperties securityProps;

    public SecurityConfigAuditor(SecurityAuditLogger audit, CommonSecurityProperties securityProps) {
        this.audit = audit;
        this.securityProps = securityProps;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        Environment env = ctx.getEnvironment();

        // 1) 본문 로깅 ON + 마스킹 OFF → 자격/PII 평문 로깅 위험
        boolean payload = env.getProperty("mutuus.common.payload-logging.enabled", Boolean.class, false);
        boolean method = env.getProperty("mutuus.common.method-logging.enabled", Boolean.class, false);
        boolean masking = env.getProperty("mutuus.common.logging.masking.enabled", Boolean.class, false);
        if ((payload || method) && !masking) {
            audit.configRisk("security.config.masking_disabled",
                    "본문 로깅 활성(payload=" + payload + ", method=" + method + ")인데 마스킹 off — 로그에 자격/PII 평문 노출 위험");
        }

        // 2) 세션(쿠키) 사용 + CSRF 비활성(라이브러리 기본) → CSRF 노출 가능
        if (hasSessionRepository(ctx)) {
            audit.configRisk("security.config.csrf_disabled_with_session",
                    "쿠키 기반 세션 저장소 사용 중 + 기본 SecurityFilterChain 은 CSRF 비활성 — 세션 사용 시 CSRF 보호 검토 필요");
        }

        // 3) permit-all 과대(/** · /*) → 인증 우회 표면 과다
        List<String> broad = securityProps.getPermitAll().stream()
                .filter(p -> p != null && (p.equals("/**") || p.equals("/*"))).toList();
        if (!broad.isEmpty()) {
            audit.configRisk("security.config.permit_all_broad",
                    "permit-all 패턴이 과도하게 넓음: " + broad);
        }
    }

    /** spring-session 의 SessionRepository 빈 존재 여부(클래스 없으면 미사용으로 간주 — optional 의존성). */
    private static boolean hasSessionRepository(ApplicationContext ctx) {
        try {
            Class<?> type = Class.forName("org.springframework.session.SessionRepository");
            return ctx.getBeanNamesForType(type).length > 0;
        } catch (ClassNotFoundException e) {
            return false; // spring-session 미탑재 → 세션 미사용
        }
    }
}
