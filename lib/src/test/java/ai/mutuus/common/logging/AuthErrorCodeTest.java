package ai.mutuus.common.logging;

import ai.mutuus.common.security.LoggingAuthenticationEntryPoint;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuthErrorCodeTest {
    @Test void threeEventsAndAuthenticationClassificationsReachJson() throws Exception {
        Logger access = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        Logger audit = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        var snapshot = new SnapshotAppender();
        snapshot.setContext(access.getLoggerContext());
        snapshot.addAppender(appender);
        snapshot.start();
        access.addAppender(snapshot);
        audit.addAppender(snapshot);
        try {
            var security = new SecurityAuditLogger();
            var response = new MockHttpServletResponse();
            var request = new MockHttpServletRequest("GET", "/protected");
            var exception = new InsufficientAuthenticationException("Authentication required");
            var entryPoint = new LoggingAuthenticationEntryPoint((req, res, ex) -> {
                assertSame(request, req);
                assertSame(exception, ex);
                res.setStatus(401);
            }, new AccessLogger(), security);
            entryPoint.commence(request, response, exception);
            assertEquals(401, response.getStatus());
            security.propagationBlocked("untrusted.invalid", "Authorization");
            check(appender.list.get(0), "auth.failure", "AUTH_TOKEN_MISSING");
            check(appender.list.get(1), "security.authn.failed", "AUTH_TOKEN_MISSING");
            check(appender.list.get(2), "security.propagation.blocked", "AUTH_PROPAGATION_BLOCKED");

            var expired = new InvalidBearerTokenException("Rejected", new JwtValidationException("Rejected",
                    List.of(new OAuth2Error("invalid_token", "Jwt expired at 2026-10-21T00:00:00Z", null))));
            for (var failure : List.of(new InvalidBearerTokenException("Rejected"), expired,
                    new InsufficientAuthenticationException("Unknown"))) {
                String expected = failure == expired ? "AUTH_TOKEN_EXPIRED"
                        : failure instanceof InvalidBearerTokenException ? "AUTH_TOKEN_INVALID" : "AUTHN_FAILED";
                int start = appender.list.size();
                new AccessLogger().authFailure("/protected", "Rejected", failure);
                security.authnFailed("/protected", "127.0.0.1", "Rejected", failure);
                check(appender.list.get(start), "auth.failure", expected);
                check(appender.list.get(start + 1), "security.authn.failed", expected);
            }
        } finally {
            access.detachAppender(snapshot);
            audit.detachAppender(snapshot);
            snapshot.stop();
            appender.stop();
        }
    }

    private static void check(ILoggingEvent event, String action, String code) throws Exception {
        byte[] bytes = new CommonJsonEncoder().encode(event);
        String foundAction = null, foundCode = null;
        try (var parser = new JsonFactory().createParser(bytes)) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    if (name.equals("action")) foundAction = parser.nextTextValue();
                    if (name.equals("code")) foundCode = parser.nextTextValue();
                }
            }
        }
        assertEquals(action, foundAction);
        assertEquals(code, foundCode);
        System.out.println("AUTH_ERROR_JSON " + new String(bytes, StandardCharsets.UTF_8).strip());
    }
}
