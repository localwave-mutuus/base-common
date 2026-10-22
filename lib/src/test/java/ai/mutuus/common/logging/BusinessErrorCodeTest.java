package ai.mutuus.common.logging;

import ai.mutuus.common.exception.*;
import ai.mutuus.common.i18n.MessageResolver;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class BusinessErrorCodeTest {
    @Test void businessCodeSurvivesResponseExceptionAndCompletionJson() throws Exception {
        exercise(true);
    }
    @Test void exceptionWithoutCodeKeepsGeneralFallback() throws Exception {
        exercise(false);
    }
    private void exercise(boolean business) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        var events = new ListAppender<ILoggingEvent>();
        events.start();
        var snapshot = new SnapshotAppender();
        snapshot.setContext(logger.getLoggerContext());
        snapshot.addAppender(events);
        snapshot.start();
        logger.addAppender(snapshot);
        try {
            var access = new AccessLogger();
            var handler = new GlobalExceptionHandler(new MessageResolver(new StaticMessageSource()), access);
            var request = new MockHttpServletRequest("GET", "/orders");
            var response = new MockHttpServletResponse();
            var failure = business ? new BusinessException(new ErrorCode() {
                public String code() { return "ORDER_ALREADY_SHIPPED"; }
                public HttpStatus status() { return HttpStatus.CONFLICT; }
                public String messageKey() { return "order.shipped"; }
            }) : new IllegalStateException("internal detail");
            new AccessLogFilter(access, new CommonLoggingProperties()).doFilter(request, response, (req, res) -> {
                var result = business ? handler.handleBusiness((BusinessException) failure,
                        (jakarta.servlet.http.HttpServletRequest) req)
                        : handler.handleUnexpected(failure, (jakarta.servlet.http.HttpServletRequest) req);
                assertEquals(business ? "ORDER_ALREADY_SHIPPED" : "INTERNAL_ERROR", result.getBody().error().code());
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(result.getStatusCode().value());
            });
            assertEquals(2, events.list.size());
            assertJson(events.list.get(0), business ? "ORDER_ALREADY_SHIPPED" : "OPERATION_FAILED", failure);
            assertJson(events.list.get(1), business ? "ORDER_ALREADY_SHIPPED" : "HTTP_500", failure);
        } finally {
            logger.detachAppender(snapshot);
            snapshot.stop();
            events.stop();
        }
    }
    private void assertJson(ILoggingEvent event, String code, Exception failure) throws Exception {
        byte[] json = new CommonJsonEncoder().encode(event);
        String foundCode = null, foundType = null;
        try (var parser = new JsonFactory().createParser(json)) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == com.fasterxml.jackson.core.JsonToken.FIELD_NAME
                        && parser.currentName().equals("error")) {
                    assertEquals(com.fasterxml.jackson.core.JsonToken.START_OBJECT, parser.nextToken());
                    while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) {
                        String field = parser.currentName();
                        parser.nextToken();
                        if (field.equals("code")) foundCode = parser.getText();
                        else if (field.equals("type")) foundType = parser.getText();
                        else parser.skipChildren();
                    }
                }
            }
        }
        assertEquals(code, foundCode);
        assertEquals(failure.getClass().getName(), foundType);
        System.out.println("BUSINESS_ERROR_JSON " + new String(json, java.nio.charset.StandardCharsets.UTF_8).strip());
    }
}
