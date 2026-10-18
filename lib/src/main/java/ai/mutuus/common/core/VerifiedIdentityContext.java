package ai.mutuus.common.core;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;

/** 검증된 인증 owner만 설치하는 불변 사용자 문맥. 인입 baggage/MDC에서 복원하지 않는다. */
public final class VerifiedIdentityContext {
    public static final String REQUEST_ATTRIBUTE = VerifiedIdentityContext.class.getName() + ".identity";
    private static final ContextKey<Identity> KEY = ContextKey.named("common.verified-identity");
    private static final Identity ANONYMOUS = new Identity(null, null, "anonymous");

    private VerifiedIdentityContext() { }

    public record Identity(String userId, String realm, String authState, String source) {
        public Identity(String userId, String realm, String authState) { this(userId, realm, authState, null); }
        public Identity {
            if (source != null && (!"verified_initiator".equals(source) || userId == null)) {
                throw new IllegalArgumentException("Invalid verified identity source");
            }
            if (!java.util.Set.of("anonymous", "failed", "authenticated", "workload").contains(authState)) {
                throw new IllegalArgumentException("Unsupported authentication state");
            }
            if ((userId == null) != (realm == null)
                    || (userId != null && (userId.isBlank() || realm.isBlank()))
                    || (userId != null && !"authenticated".equals(authState))) {
                throw new IllegalArgumentException("Verified user ID and realm must be paired and authenticated");
            }
        }

        public Map<String, String> fields() {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("golmok.auth.state", authState);
            if (userId != null) {
                fields.put("user.id", userId);
                fields.put("golmok.identity.realm", realm);
            }
            if (source != null) fields.put("golmok.identity.source", source);
            return Map.copyOf(fields);
        }
    }

    public static Identity current() {
        Identity identity = Context.current().get(KEY);
        return identity == null ? ANONYMOUS : identity;
    }

    public static String currentUserId() { return current().userId(); }

    public static Scope makeCurrent(Identity identity) {
        return Context.current().with(KEY, Objects.requireNonNull(identity)).makeCurrent();
    }
}
