package ai.mutuus.common.logging;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import ai.mutuus.common.async.TraceContextPropagation;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** 외부 DB 없이 실제 Spring TX 경계와 소비 encoder를 확인한다. */
class ActionLoggerTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(ActionLogger.LOGGER_NAME);
    private final ListAppender<ILoggingEvent> captured = new ListAppender<>();
    private final ActionLogger writer = new ActionLogger(new TransactionActionCompletionHandler());
    private final DataSourceTransactionManager manager = new DataSourceTransactionManager(
            new DriverManagerDataSource("jdbc:h2:mem:action-record;DB_CLOSE_DELAY=-1", "sa", ""));
    private final TransactionTemplate tx = new TransactionTemplate(manager);

    @BeforeEach void capture() {
        captured.setContext(logger.getLoggerContext()); captured.start(); logger.addAppender(captured);
    }
    @AfterEach void clean() { logger.detachAppender(captured); captured.stop(); TraceContext.clear(); }

    private ActionRecord result(ActionRecord.Outcome outcome, String code) {
        return ActionRecord.capture("member.signup", outcome, code, "operation-1", "member-1", List.of("member-2"));
    }
    private Object field(int index, String name) {
        return captured.list.get(index).getKeyValuePairs().stream().filter(p -> p.key.equals(name))
                .map(p -> p.value).findFirst().orElse(null);
    }

    @Test void commit_waits_and_business_failure_commit_stays_failure() {
        tx.executeWithoutResult(status -> {
            writer.record(result(ActionRecord.Outcome.SUCCESS, null));
            assertThat(captured.list).isEmpty();
        });
        tx.executeWithoutResult(status -> writer.record(result(ActionRecord.Outcome.FAILURE, "OWNER_REJECTED")));
        assertThat(captured.list).hasSize(2);
        assertThat(field(0, "outcome")).isEqualTo("SUCCESS");
        assertThat(field(1, "outcome")).isEqualTo("FAILURE");
        assertThat(field(1, "errorCode")).isEqualTo("OWNER_REJECTED");
    }

    @Test void rollback_only_required_participation_emits_once_after_outer_completion() {
        assertThatThrownBy(() -> tx.executeWithoutResult(outer -> {
            var bound = writer.bind();
            tx.executeWithoutResult(inner -> { bound.record(result(ActionRecord.Outcome.SUCCESS, null)); inner.setRollbackOnly(); });
            assertThat(captured.list).isEmpty();
        })).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(captured.list).hasSize(1);
        assertThat(field(0, "outcome")).isEqualTo("FAILURE");
        assertThat(field(0, "errorCode")).isEqualTo("TRANSACTION_ROLLED_BACK");
    }

    @Test void outer_binding_does_not_claim_success_when_requires_new_commits_but_outer_rolls_back() {
        tx.executeWithoutResult(outer -> {
            var bound = writer.bind();
            var inner = new TransactionTemplate(manager);
            inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            inner.executeWithoutResult(status -> bound.record(result(ActionRecord.Outcome.SUCCESS, null)));
            assertThat(captured.list).isEmpty(); outer.setRollbackOnly();
        });
        assertThat(captured.list).hasSize(1);
        assertThat(field(0, "outcome")).isEqualTo("FAILURE");
    }

    @Test void explicit_owner_error_is_preserved_on_rollback() {
        tx.executeWithoutResult(status -> {
            writer.record(result(ActionRecord.Outcome.FAILURE, "OWNER_REJECTED")); status.setRollbackOnly();
        });
        assertThat(field(0, "errorCode")).isEqualTo("OWNER_REJECTED");
    }

    @Test void no_tx_is_immediate_unknown_is_diagnostic_only_and_callback_is_once() {
        writer.record(result(ActionRecord.Outcome.FAILURE, "INPUT_INVALID"));
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            writer.record(result(ActionRecord.Outcome.SUCCESS, null));
            var callback = TransactionSynchronizationManager.getSynchronizations().getFirst();
            callback.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
            callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
        assertThat(captured.list).hasSize(2);
        assertThat(field(0, "outcome")).isEqualTo("FAILURE");
        assertThat(field(1, "event.dataset")).isEqualTo("mutuus.action_diagnostic");
        assertThat(captured.list.get(1).getKeyValuePairs()).noneMatch(p -> p.key.equals("outcome") || p.key.equals("event.outcome"));
    }

    @Test void async_snapshot_ids_and_related_members_remain_structured_json_array() throws Exception {
        TraceContext.put(RequestIds.CONTEXT_KEY, "request-original");
        TraceContext.put(HeaderNames.TRACE_ID, "trace-original");
        var members = new ArrayList<>(List.of("member-2", "member-3"));
        var snapshot = new ActionRecord("member.signup", ActionRecord.Outcome.SUCCESS, null, RequestIds.current(),
                "operation-1", "member-1", members, TraceContext.traceId());
        var bound = tx.execute(status -> writer.bind());
        members.clear(); TraceContext.clear();
        try (var executor = Executors.newSingleThreadExecutor()) { executor.submit(() -> bound.record(snapshot)).get(); }
        assertThat(field(0, "requestId")).isEqualTo("request-original");
        assertThat(field(0, "trace.id")).isEqualTo("trace-original");
        var encoder = new LogstashEncoder(); encoder.setContext(logger.getLoggerContext()); encoder.start();
        try {
            var json = JsonMapper.builder().build().readTree(new String(encoder.encode(captured.list.getFirst()), StandardCharsets.UTF_8));
            assertThat(json.get("relatedMemberIds").isArray()).isTrue();
            assertThat(json.get("relatedMemberIds").size()).isEqualTo(2);
            assertThat(json.get("actorId").asText()).isEqualTo("member-1");
            assertThat(json.get("event.outcome").asText()).isEqualTo("success");
            // 기존 LogstashEncoder는 null key-value를 생략한다(업무 값 자체는 null).
            assertThat(json.get("errorCode")).isNull();
            assertThat(json.has("@timestamp")).isTrue();
        } finally { encoder.stop(); }
        assertThatThrownBy(() -> bound.record(snapshot)).isInstanceOf(IllegalStateException.class);
    }

    @Test void propagation_and_autoconfiguration_reuse_context_without_inferring_actor() throws Exception {
        TraceContext.put(RequestIds.CONTEXT_KEY, "request-original");
        TraceContext.put(HeaderNames.USER_ID, "auth-subject");
        var wrapped = TraceContextPropagation.wrap((java.util.concurrent.Callable<ActionRecord>) () -> ActionRecord.capture("member.signup",
                ActionRecord.Outcome.FAILURE, "INPUT_INVALID", null, null, List.of()));
        TraceContext.clear();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var record = executor.submit(wrapped).get();
            assertThat(record.requestId()).isEqualTo("request-original"); assertThat(record.actorId()).isNull();
        }
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CommonActionAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ActionLogger.class).hasSingleBean(ActionCompletionHandler.class);
                    tx.executeWithoutResult(status -> {
                        context.getBean(ActionLogger.class).record(result(ActionRecord.Outcome.SUCCESS, null));
                        assertThat(captured.list).isEmpty();
                    });
                    assertThat(captured.list).hasSize(1);
                });
    }
}
