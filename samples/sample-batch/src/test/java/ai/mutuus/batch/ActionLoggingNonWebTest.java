package ai.mutuus.batch;

import java.util.List;
import ai.mutuus.common.logging.ActionLogger;
import ai.mutuus.common.logging.ActionRecord;
import ai.mutuus.common.logging.CommonActionAutoConfiguration;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class ActionLoggingNonWebTest {
    @Test void action_writer_is_available_without_web_security_or_spring_tx() {
        for (var name : List.of("jakarta.servlet.Filter", "org.springframework.web.servlet.DispatcherServlet",
                "org.springframework.security.web.SecurityFilterChain", "org.springframework.transaction.support.TransactionSynchronizationManager"))
            assertThatThrownBy(() -> Class.forName(name)).isInstanceOf(ClassNotFoundException.class);
        var logger = (Logger) LoggerFactory.getLogger(ActionLogger.LOGGER_NAME);
        var captured = new ListAppender<ILoggingEvent>(); captured.setContext(logger.getLoggerContext()); captured.start(); logger.addAppender(captured);
        try {
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CommonActionAutoConfiguration.class))
                    .run(context -> {
                        assertThat(context).hasSingleBean(ActionLogger.class).doesNotHaveBean("actionCompletionHandler");
                        context.getBean(ActionLogger.class).record(new ActionRecord("batch.reconcile", ActionRecord.Outcome.FAILURE,
                                "OWNER_FAILED", "invocation-1", "run-1", null, List.of("member-1"), "invocation-trace"));
                    });
            assertThat(captured.list).hasSize(1);
            assertThat(captured.list.getFirst().getKeyValuePairs()).anyMatch(p -> p.key.equals("actorId") && p.value == null)
                    .anyMatch(p -> p.key.equals("relatedMemberIds") && p.value.equals(List.of("member-1")));
        } finally { logger.detachAppender(captured); captured.stop(); }
    }
}
