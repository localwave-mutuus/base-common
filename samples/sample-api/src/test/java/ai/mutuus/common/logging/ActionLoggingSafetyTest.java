package ai.mutuus.common.logging;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.IdGenerator;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

class ActionLoggingSafetyTest {
    @Test void malformed_metadata_and_logging_failures_never_change_business_commit_or_leak_raw_trace() {
        String requestId = UUID.randomUUID().toString();
        String untrusted = "UNTRUSTED_TRACE\r\nPRIVATE_TRACE_VALUE@invalid";
        var logger = (Logger) LoggerFactory.getLogger(ActionLogger.LOGGER_NAME);
        var captured = new ListAppender<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) { event.prepareForDeferredProcessing(); super.append(event); }
        };
        captured.setContext(logger.getLoggerContext()); captured.start(); logger.addAppender(captured);
        var manager = new DataSourceTransactionManager(new DriverManagerDataSource("jdbc:h2:mem:action-safety", "sa", ""));
        var tx = new TransactionTemplate(manager);
        var actionLogger = new ActionLogger(new TransactionActionCompletionHandler());
        TurboFilter brokenWriter = new TurboFilter() {
            @Override public FilterReply decide(Marker marker, Logger target, Level level, String format, Object[] args, Throwable failure) {
                if (target.getName().equals(ActionLogger.LOGGER_NAME)) throw new IllegalStateException("PRIVATE_EXCEPTION_MESSAGE");
                return FilterReply.NEUTRAL;
            }
        };
        try {
            TraceContext.put(RequestIds.CONTEXT_KEY, requestId); TraceContext.put(HeaderNames.TRACE_ID, untrusted);
            MDC.put(EcsFields.TRACE_ID, untrusted); MDC.put(HeaderNames.TRACE_ID, untrusted);
            ActionRecord result = ActionRecord.capture("member.signup", ActionRecord.Outcome.SUCCESS, null,
                    "operation-1", "member-1", Arrays.asList("member-2", null, "bad@member"));
            assertThat(result.requestId()).isEqualTo(requestId); assertThat(result.traceId()).isNull();
            assertThat(result.relatedMemberIds()).containsExactly("member-2");
            assertThat(new ActionRecord("member.signup", ActionRecord.Outcome.SUCCESS, null, requestId,
                    null, null, null, IdGenerator.newTraceId()).traceId()).hasSize(32);
            var boundRef = new java.util.concurrent.atomic.AtomicReference<ActionLogger.BoundAction>();
            var commit = new java.util.concurrent.atomic.AtomicInteger();
            assertThatCode(() -> tx.executeWithoutResult(status -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { commit.incrementAndGet(); }
                });
                var bound = actionLogger.bind(); boundRef.set(bound); bound.record(result);
                bound.record(result); bound.record(null); actionLogger.record(null);
                actionLogger.record(ActionRecord.capture("invalid raw@action", null, "private message", null, null, null));
                assertThat(status.isRollbackOnly()).isFalse();
            })).doesNotThrowAnyException();
            assertThat(commit.get()).isEqualTo(1);
            assertThatCode(() -> boundRef.get().record(result)).doesNotThrowAnyException();
            assertThat(captured.list).filteredOn(event -> event.getKeyValuePairs().stream()
                    .anyMatch(p -> p.key.equals("event.dataset") && p.value.equals("mutuus.action"))).hasSize(1);
            var confirmed = captured.list.stream().filter(event -> event.getKeyValuePairs().stream()
                    .anyMatch(p -> p.key.equals("event.dataset") && p.value.equals("mutuus.action"))).findFirst().orElseThrow();
            assertThat(confirmed.getKeyValuePairs()).anyMatch(p -> p.key.equals("requestId") && p.value.equals(requestId))
                    .anyMatch(p -> p.key.equals("outcome") && p.value.equals("SUCCESS"));
            var encoder = new LogstashEncoder(); encoder.setContext(logger.getLoggerContext()); encoder.start();
            try {
                for (var event : captured.list) {
                    String json = new String(encoder.encode(event), StandardCharsets.UTF_8);
                    assertThat(json).doesNotContain("UNTRUSTED_TRACE", "PRIVATE_TRACE_VALUE", "invalid raw@action", "private message", "PRIVATE_EXCEPTION_MESSAGE");
                }
            } finally { encoder.stop(); }
            assertThat(MDC.get(EcsFields.TRACE_ID)).isEqualTo(untrusted);
            assertThatCode(() -> {
                var brokenCompletion = new ActionLogger(callback -> { throw new AssertionError("PRIVATE_COMPLETION_MESSAGE"); });
                brokenCompletion.record(result); brokenCompletion.bind().record(result);
                new TransactionActionCompletionHandler().complete(completion -> { throw new AssertionError("PRIVATE_CALLBACK_MESSAGE"); });
            }).doesNotThrowAnyException();
            brokenWriter.start(); logger.getLoggerContext().addTurboFilter(brokenWriter);
            assertThatCode(() -> tx.executeWithoutResult(status -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { commit.incrementAndGet(); }
                });
                actionLogger.record(result); actionLogger.record(null);
                actionLogger.bind().record(result);
                assertThat(status.isRollbackOnly()).isFalse();
            })).doesNotThrowAnyException();
            assertThat(commit.get()).isEqualTo(2);
        } finally {
            logger.getLoggerContext().getTurboFilterList().remove(brokenWriter); brokenWriter.stop();
            logger.detachAppender(captured); captured.stop(); TraceContext.clear(); MDC.clear();
        }
    }
}
