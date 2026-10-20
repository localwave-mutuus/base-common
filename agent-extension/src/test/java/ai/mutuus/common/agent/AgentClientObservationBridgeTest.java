package ai.mutuus.common.agent;

import ai.mutuus.common.observability.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AgentClientObservationBridgeTest {
    @Test void capturesOnlyActualClientDetachesAndSurvivesEndAndThreadChange() throws Exception {
        var processor=new SpanLineageSpanProcessor(10); processor.register();
        try(var sdk=SdkTracerProvider.builder().addSpanProcessor(processor).build()) {
            var parent=sdk.get("fixture").spanBuilder("parent").startSpan();
            AgentClientObservation observed;
            Span client;
            try(var ignored=parent.makeCurrent()) {
                observed=new AgentClientObservation();
                var unrelated=sdk.get("fixture").spanBuilder("internal").startSpan();unrelated.end();
                client=sdk.get("fixture").spanBuilder("client").setSpanKind(SpanKind.CLIENT).startSpan();
                observed.detach();client.end();
            }
            var result=java.util.concurrent.CompletableFuture.supplyAsync(observed::finish).get();
            assertThat(result).containsEntry("span.id",client.getSpanContext().getSpanId()).containsEntry("parent.id",parent.getSpanContext().getSpanId());
            assertThat(observed.finish()).isEmpty();
            try(var ignored=parent.makeCurrent()) {
                var empty=new AgentClientObservation();empty.detach();
                var later=sdk.get("fixture").spanBuilder("later").setSpanKind(SpanKind.CLIENT).startSpan();later.end();
                assertThat(empty.finish()).isEmpty();
                var current=sdk.get("fixture").spanBuilder("already-client").setSpanKind(SpanKind.CLIENT).startSpan();
                try(var active=current.makeCurrent()) {
                    var captured=new AgentClientObservation();captured.detach();current.end();
                    assertThat(captured.finish()).containsEntry("span.id",current.getSpanContext().getSpanId());
                }
            }
            parent.end();
        }
    }
}
