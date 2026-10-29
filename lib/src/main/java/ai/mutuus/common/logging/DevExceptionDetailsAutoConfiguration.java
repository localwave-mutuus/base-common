package ai.mutuus.common.logging;

import ai.mutuus.common.core.SensitiveDataMasker;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/** Opt-in dev diagnostics. Missing masking policy fails closed; raw structured fields stay blocked. */
@AutoConfiguration(after = CommonMaskingAutoConfiguration.class)
@Profile("dev & !prod & !production")
@ConditionalOnProperty(prefix = "mutuus.common.logging.exception-details", name = "enabled", havingValue = "true")
public class DevExceptionDetailsAutoConfiguration {
    private static volatile Policy active;

    @Bean
    @ConditionalOnBean(SensitiveDataMasker.class)
    Policy devExceptionDetailsPolicy(SensitiveDataMasker masker) { return new Policy(masker); }

    static boolean enabled() { return active != null; }

    static void capture(IThrowableProxy error, Map<String, Object> fields) {
        Policy policy = active;
        if (error == null || policy == null) return;
        try {
            String message = policy.safe(error.getMessage());
            StringBuilder stack = new StringBuilder();
            append(stack, error, policy);
            // Include the first cause only; this also bounds cyclic/deep cause chains.
            if (error.getCause() != null) {
                stack.append("\nCaused by: ");
                append(stack, error.getCause(), policy);
            }
            fields.put("error.message", message);
            fields.put("error.stack_trace", stack.toString());
        } catch (RuntimeException ignored) {
            // A broken masking policy must neither leak raw text nor affect application behavior.
        }
    }

    private static void append(StringBuilder out, IThrowableProxy error, Policy policy) {
        out.append(policy.safe(error.getClassName())).append(": ").append(policy.safe(error.getMessage()));
        var frames = error.getStackTraceElementProxyArray();
        if (frames == null) return;
        for (int i = 0; i < Math.min(5, frames.length); i++) {
            out.append("\n\tat ").append(policy.safe(frames[i].getStackTraceElement().toString()));
        }
    }

    static final class Policy implements InitializingBean, DisposableBean {
        private final SensitiveDataMasker masker;
        Policy(SensitiveDataMasker masker) { this.masker = masker; }
        String safe(String value) {
            String masked = masker.mask(value == null ? "" : value);
            return masked.substring(0, Math.min(masked.length(), 1024));
        }
        @Override public void afterPropertiesSet() { active = this; }
        @Override public void destroy() { if (active == this) active = null; }
    }
}
