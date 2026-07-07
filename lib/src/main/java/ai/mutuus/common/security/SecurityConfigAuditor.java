package ai.mutuus.common.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ai.mutuus.common.security.audit.SecurityAuditLogger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * Emits security configuration risk events once the application is ready.
 *
 * <p>This auditor is intentionally diagnostic-only: it never blocks startup or changes runtime behavior.
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

        boolean payload = env.getProperty("mutuus.common.payload-logging.enabled", Boolean.class, false);
        boolean method = env.getProperty("mutuus.common.method-logging.enabled", Boolean.class, false);
        boolean masking = env.getProperty("mutuus.common.logging.masking.enabled", Boolean.class, false);
        if ((payload || method) && !masking) {
            audit.configRisk("security.config.masking_disabled",
                    "Payload or method logging is enabled without masking; logs may expose secrets or PII.");
        }

        if (hasSessionRepository(ctx) && !securityProps.isCsrfEnabled()) {
            audit.configRisk("security.config.csrf_disabled_with_session",
                    "Cookie-backed session storage is present while CSRF protection is disabled.");
        }

        List<String> broad = securityProps.getPermitAll().stream()
                .filter(p -> p != null && (p.equals("/**") || p.equals("/*"))).toList();
        if (!broad.isEmpty()) {
            audit.configRisk("security.config.permit_all_broad",
                    "permit-all patterns are too broad: " + broad);
        }

        auditClasspathSecrets(env);
    }

    private void auditClasspathSecrets(Environment env) {
        if (!(env instanceof ConfigurableEnvironment configurable)) {
            return;
        }

        List<String> findings = new ArrayList<>();
        for (PropertySource<?> source : configurable.getPropertySources()) {
            if (!isClasspathApplicationConfig(source) || !(source instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String name : enumerable.getPropertyNames()) {
                if (isSecretPropertyName(name) && hasText(enumerable.getProperty(name))) {
                    findings.add(name + "@" + source.getName());
                }
            }
        }

        if (!findings.isEmpty()) {
            audit.configRisk("security.config.secret_in_classpath_config",
                    "Secret-like properties are defined in classpath application config: "
                            + findings
                            + ". Move real values to an external spring.config.import file, "
                            + "for example an external local.yml via spring.config.import or a runtime secret mount.");
        }
    }

    private static boolean isClasspathApplicationConfig(PropertySource<?> source) {
        String name = source.getName().toLowerCase(Locale.ROOT);
        return name.contains("classpath") && name.contains("application");
    }

    private static boolean isSecretPropertyName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.equals("spring.datasource.password")
                || normalized.equals("spring.data.redis.password")
                || (normalized.startsWith("mutuus.common.datasource.") && normalized.endsWith(".password"));
    }

    private static boolean hasText(Object value) {
        return value != null && !value.toString().isBlank();
    }

    private static boolean hasSessionRepository(ApplicationContext ctx) {
        try {
            Class<?> type = Class.forName("org.springframework.session.SessionRepository");
            return ctx.getBeanNamesForType(type).length > 0;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
