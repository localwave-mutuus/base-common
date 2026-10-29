package ai.mutuus.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DevExceptionDetailsTest {
    @Test void oneThrownServerExceptionIsMaskedBoundedAndDevOnly() {
        Exception failure;
        try {
            throw new IllegalStateException("card=1234567890123456 secret=supersecret",
                    new IllegalArgumentException("cause card=1234567890123456 secret=supersecret"));
        } catch (Exception ex) { failure = ex; }
        var frames = IntStream.rangeClosed(1, 7)
                .mapToObj(i -> new StackTraceElement("demo.Checkout", "execute", "Checkout.java", i))
                .toArray(StackTraceElement[]::new);
        failure.setStackTrace(frames);
        failure.getCause().setStackTrace(frames);
        final Exception thrown = failure;
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            new AccessLogger().serverError("/diagnostic", "HTTP_500", failure);
            ILoggingEvent event = appender.list.getFirst();
            var runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                    CommonMaskingAutoConfiguration.class, DevExceptionDetailsAutoConfiguration.class))
                    .withPropertyValues("mutuus.common.logging.masking.enabled=true",
                            "mutuus.common.logging.masking.patterns[0]=supersecret");
            runner.withPropertyValues("spring.profiles.active=dev").run(ctx -> assertHidden(event));
            runner.withPropertyValues("spring.profiles.active=prod",
                    "mutuus.common.logging.exception-details.enabled=true").run(ctx -> assertHidden(event));
            runner.withPropertyValues("spring.profiles.active=dev,prod",
                    "mutuus.common.logging.exception-details.enabled=true").run(ctx -> assertHidden(event));
            runner.withPropertyValues("spring.profiles.active=dev",
                    "mutuus.common.logging.masking.enabled=false",
                    "mutuus.common.logging.exception-details.enabled=true").run(ctx -> assertHidden(event));
            runner.withPropertyValues("spring.profiles.active=dev",
                    "mutuus.common.logging.exception-details.enabled=true").run(ctx -> {
                Map<String, Object> fields = CanonicalLogSnapshot.capture(event);
                assertEquals(IllegalStateException.class.getName(), fields.get("error.type"));
                assertEquals("card=************3456 secret=*******cret", fields.get("error.message"));
                String stack = (String) fields.get("error.stack_trace");
                assertTrue(stack.contains("Caused by: java.lang.IllegalArgumentException: cause card=************3456"));
                assertEquals(10, stack.lines().filter(line -> line.startsWith("\tat ")).count());
                assertTrue(stack.contains("Checkout.java:5"));
                assertFalse(stack.contains("Checkout.java:6"));
                assertEquals("telemetry.event", fields.get("message"));
                TelemetryFieldRegistry.load().validateSnapshot(fields);
                String json = new String(new CommonJsonEncoder().encodeSnapshot(fields), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"stack_trace\""));
                assertFalse(json.contains("1234567890123456"));
                assertFalse(json.contains("supersecret"));
                new AccessLogger().serverComplete("GET", "/diagnostic", "/diagnostic", 500,
                        1L, "HTTP_500", false, Map.of(), thrown);
                var completion = CanonicalLogSnapshot.capture(appender.list.getLast());
                assertEquals(fields.get("error.message"), completion.get("error.message"));
                assertEquals(stack, completion.get("error.stack_trace"));
            });
            assertHidden(event); // Closing the context disables diagnostics again.
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
    private static void assertHidden(ILoggingEvent event) {
        var fields = CanonicalLogSnapshot.capture(event);
        assertFalse(fields.containsKey("error.message"));
        assertFalse(fields.containsKey("error.stack_trace"));
    }
}
