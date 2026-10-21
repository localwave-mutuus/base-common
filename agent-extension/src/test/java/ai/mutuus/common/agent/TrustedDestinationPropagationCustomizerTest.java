package ai.mutuus.common.agent;

import org.junit.jupiter.api.Test;
import io.opentelemetry.context.*;
import io.opentelemetry.context.propagation.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import ai.mutuus.common.core.TelemetryEnvelope;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class TrustedDestinationPropagationCustomizerTest {
    @Test void storageEnvelopePreservesW3cParentWithoutOpeningMapInjection() {
        var policy=new TrustedDestinationPropagationCustomizer.DestinationPropagator(
                W3CTraceContextPropagator.getInstance(),List.of());
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(OpenTelemetry.propagating(ContextPropagators.create(policy)));
        try(var sdk=SdkTracerProvider.builder().build()) {
            var parent=sdk.get("fixture").spanBuilder("ingress").setSpanKind(SpanKind.SERVER).startSpan();
            try(var scope=parent.makeCurrent();
                var baggage=Baggage.builder().put("secret","excluded").build().makeCurrent()) {
                long before=TrustedDestinationPropagationCustomizer.blockedInjectionCount();
                var envelope=TelemetryEnvelope.captureCurrentVerified("operation","attempt-1");
                assertThat(TrustedDestinationPropagationCustomizer.blockedInjectionCount()).isEqualTo(before);
                var restored=Span.fromContext(envelope.extractFromOwnerValidatedStorage()).getSpanContext();
                assertThat(restored.isRemote()).isTrue();
                assertThat(restored.getTraceId()).isEqualTo(parent.getSpanContext().getTraceId());
                assertThat(restored.getSpanId()).isEqualTo(parent.getSpanContext().getSpanId());
                assertThat(restored.getTraceFlags()).isEqualTo(parent.getSpanContext().getTraceFlags());
                assertThat(restored.getTraceState()).isEqualTo(parent.getSpanContext().getTraceState());
                assertThat(envelope.toStorageString()).doesNotContain("secret","excluded");
                var headers=new LinkedHashMap<String,String>();
                policy.inject(Context.current(),headers,Map::put);
                assertThat(headers).isEmpty();
                assertThat(TrustedDestinationPropagationCustomizer.blockedInjectionCount()).isEqualTo(before+1);
            } finally { parent.end(); }
        } finally { GlobalOpenTelemetry.resetForTest(); }
    }

    @Test void exactHostsUnknownDestinationsCallerAndWildcardFailClosed() {
        var original=W3CTraceContextPropagator.getInstance();
        assertThatThrownBy(()->new TrustedDestinationPropagationCustomizer.DestinationPropagator(original,List.of("*.local"))).isInstanceOf(IllegalArgumentException.class);
        var policy=new TrustedDestinationPropagationCustomizer.DestinationPropagator(original,List.of("allowed.local"));
        try(var sdk=SdkTracerProvider.builder().build()) {
            for(String url:List.of("http://allowed.local/a","http://allowed.local.evil/a","http://denied.local/a","invalid")) {
                var span=sdk.get("fixture").spanBuilder("client").setSpanKind(SpanKind.CLIENT).setAttribute("url.full",url).startSpan();
                var headers=new HashMap<String,String>();policy.inject(Context.root().with(span),headers,Map::put);
                assertThat(headers).isEmpty();span.end();
            }
            var caller=sdk.get("fixture").spanBuilder("caller").setAttribute("url.full","http://allowed.local/a").startSpan();
            var headers=new HashMap<String,String>();policy.inject(Context.root().with(caller),headers,Map::put);assertThat(headers).isEmpty();caller.end();
            var unknown=sdk.get("fixture").spanBuilder("unknown").setSpanKind(SpanKind.CLIENT).startSpan();
            policy.inject(Context.root().with(unknown),headers,Map::put);assertThat(headers).isEmpty();unknown.end();
        }
        var remote=policy.extract(Context.root(),Map.of("traceparent","00-1234567890abcdef1234567890abcdef-abcdef1234567890-01"),new TextMapGetter<Map<String,String>>() {
            public Iterable<String> keys(Map<String,String> value){return value.keySet();} public String get(Map<String,String> value,String key){return value.get(key);}
        });
        assertThat(Span.fromContext(remote).getSpanContext().getSpanId()).isEqualTo("abcdef1234567890");
    }
}
