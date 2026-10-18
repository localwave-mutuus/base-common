package ai.mutuus.common.security;

import java.util.List;
import java.util.Optional;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.VerifiedIdentityContext;
import ai.mutuus.common.core.VerifiedIdentityContext.Identity;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 인증 owner 경계와 요청/스레드 정리 계약을 소비자 샘플에서 검증한다. */
class OtelIdentityContractTest {
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TraceContext.clear();
        MDC.clear();
    }

    @Test
    void jwtSubjectOverridesNameAndForgedHeadersAndRetainsAuthenticated403Snapshot() throws Exception {
        Jwt jwt = Jwt.withTokenValue("synthetic").header("alg", "RS256").subject("member-42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "display-name"));
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var exporter = InMemorySpanExporter.create();
        MDC.put("outer", "retained");
        try (var provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()) {
            Span span = provider.get("identity-contract").spanBuilder("authenticated-request").startSpan();
            try (Scope scope = span.makeCurrent()) {
                filter(new JwtSubjectIdentityAdapter()).doFilter(request, response, (req, res) -> {
                    assertThat(TraceContext.userId()).isEqualTo("member-42");
                    assertThat(VerifiedIdentityContext.current().realm()).isEqualTo("member");
                    assertThat(MDC.get("user.id")).isEqualTo("member-42");
                    response.setStatus(403);
                });
            } finally {
                span.end();
            }
            assertThat(exporter.getFinishedSpanItems()).hasSize(1);
            var recorded = exporter.getFinishedSpanItems().getFirst();
            assertThat(recorded.getAttributes().get(AttributeKey.stringKey("user.id"))).isEqualTo("member-42");
            assertThat(recorded.getAttributes().get(AttributeKey.stringKey("golmok.identity.realm"))).isEqualTo("member");
            assertThat(recorded.getAttributes().get(AttributeKey.stringKey("golmok.auth.state"))).isEqualTo("authenticated");
        }
        assertThat(request.getAttribute(VerifiedIdentityContext.REQUEST_ATTRIBUTE))
                .isEqualTo(new Identity("member-42", "member", "authenticated"));
        assertThat(TraceContext.userId()).isNull();
        assertThat(MDC.get("user.id")).isNull();
        assertThat(MDC.get("outer")).isEqualTo("retained");
    }
    @Test
    void anonymousNeverTrustsHeaderMdcOrBaggage() throws Exception {
        TraceContext.put(HeaderNames.USER_ID, "forged");
        MDC.put("user.id", "forged");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter(new JwtSubjectIdentityAdapter()).doFilter(request, response, (req, res) -> {
            assertThat(TraceContext.userId()).isNull();
            assertThat(MDC.get("user.id")).isNull();
            assertThat(VerifiedIdentityContext.current().authState()).isEqualTo("anonymous");
            response.setStatus(401);
        });
        assertThat(request.getAttribute(VerifiedIdentityContext.REQUEST_ATTRIBUTE))
                .isEqualTo(new Identity(null, null, "failed"));
    }

    @Test
    void customPrincipalRequiresExplicitAccountIdAdapter() throws Exception {
        record Account(String accountId, String name) { }
        Account account = new Account("operator-7", "display");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(account, null, List.of()));
        filter(new JwtSubjectIdentityAdapter()).doFilter(request(), new MockHttpServletResponse(), (req, res) -> {
            assertThat(TraceContext.userId()).isNull();
            assertThat(VerifiedIdentityContext.current().authState()).isEqualTo("authenticated");
        });
        AuthenticationIdentityAdapter adapter = auth -> auth.getPrincipal() instanceof Account verified
                ? Optional.of(new Identity(verified.accountId(), "operator", "authenticated")) : Optional.empty();
        filter(adapter).doFilter(request(), new MockHttpServletResponse(), (req, res) -> {
            assertThat(TraceContext.userId()).isEqualTo("operator-7");
            assertThat(VerifiedIdentityContext.current().realm()).isEqualTo("operator");
        });
    }

    @Test
    void exceptionRestoresPreviousIdentityScope() {
        Identity outer = new Identity("outer-member", "member", "authenticated");
        try (Scope ignored = VerifiedIdentityContext.makeCurrent(outer)) {
            assertThatThrownBy(() -> filter(new JwtSubjectIdentityAdapter()).doFilter(request(),
                    new MockHttpServletResponse(), (req, res) -> {
                        assertThat(TraceContext.userId()).isNull();
                        throw new IllegalStateException("synthetic");
                    })).isInstanceOf(IllegalStateException.class);
            assertThat(VerifiedIdentityContext.current()).isEqualTo(outer);
        }
        assertThat(TraceContext.userId()).isNull();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/synthetic");
        request.addHeader("X-User-Id", "forged");
        request.addHeader("baggage", "user.id=forged");
        request.setAttribute(HeaderNames.CLAIMED_USER_ATTR, "forged");
        return request;
    }

    private static AuthenticatedUserContextFilter filter(AuthenticationIdentityAdapter adapter) {
        return new AuthenticatedUserContextFilter(new SecurityAuditLogger(), adapter);
    }
}