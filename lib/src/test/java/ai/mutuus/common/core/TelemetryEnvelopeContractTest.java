package ai.mutuus.common.core;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import io.opentelemetry.api.*;
import io.opentelemetry.api.trace.*;
import java.util.*;
class TelemetryEnvelopeContractTest {
 @BeforeEach void setup(){C3TelemetryFixture.init();}
 @Test void storageRoundTripRetryKeepsIngressParentAndFreshExecution(){
  var ingress=GlobalOpenTelemetry.getTracer("test").spanBuilder("ingress").setSpanKind(SpanKind.SERVER).startSpan();TelemetryEnvelope envelope;
  try(var ignored=ingress.makeCurrent()){envelope=TelemetryEnvelope.captureCurrentVerified("single","initial");}finally{ingress.end();}
  String storage=envelope.toStorageString();assertTrue(storage.length()<=8192);assertEquals(envelope,TelemetryEnvelope.fromStorageString(storage));
  Set<String> executions=new HashSet<>(),spans=new HashSet<>();
  var scheduler=GlobalOpenTelemetry.getTracer("test").spanBuilder("scheduler").setNoParent().startSpan();
  try(var ignored=scheduler.makeCurrent()){
   for(String attempt:List.of("first","retry"))try(var consumer=TelemetryEnvelope.resumeStored(storage,1,"batch","batch",attempt)){
    assertNull(consumer.reason());assertEquals(ingress.getSpanContext().getTraceId(),Span.current().getSpanContext().getTraceId());
    String execution=TelemetryMetadata.current().get("execution.id");assertNotNull(execution);assertTrue(executions.add(execution));assertTrue(spans.add(Span.current().getSpanContext().getSpanId()));
    assertEquals(attempt,TelemetryMetadata.current().get("golmok.attempt.id"));
    try(var job=consumer.beginJob("single","single")){assertEquals(execution,job.executionId());job.success();}
   }
   assertEquals(scheduler.getSpanContext(),Span.current().getSpanContext());
  }finally{scheduler.end();}
  for(var s:C3TelemetryFixture.EXPORTER.getFinishedSpanItems())if(s.getKind()==SpanKind.CONSUMER){assertEquals(ingress.getSpanContext().getSpanId(),s.getParentSpanId());assertEquals(ingress.getSpanContext().getTraceId(),s.getTraceId());}
 }
 @Test void invalidMissingOwnerMismatchAlwaysStartNewRoot(){
  for(String mode:List.of("missing","invalid","owner")){
   var ambient=GlobalOpenTelemetry.getTracer("test").spanBuilder("ambient").startSpan();
   try(var ignored=ambient.makeCurrent();var c=TelemetryEnvelope.resumeStored(mode.equals("missing")?null:"invalid",1,"batch",mode.equals("owner")?"other":"batch","attempt")){
    assertEquals(mode.equals("missing")?"ENVELOPE_MISSING":mode.equals("owner")?"ENVELOPE_OWNER_MISMATCH":"ENVELOPE_INVALID",c.reason());assertNotEquals(ambient.getSpanContext().getTraceId(),Span.current().getSpanContext().getTraceId());assertNotNull(TelemetryMetadata.current().get("execution.id"));
   }finally{ambient.end();}
  }
  for(var s:C3TelemetryFixture.EXPORTER.getFinishedSpanItems())if(s.getKind()==SpanKind.CONSUMER)assertEquals("0000000000000000",s.getParentSpanId());
  assertThrows(IllegalArgumentException.class,()->TelemetryEnvelope.fromStorageString("x".repeat(8193)));
 }
 @Test void decoratorRestoresSnapshotAndThreadContextEvenOnFailure(){
  var parent=GlobalOpenTelemetry.getTracer("test").spanBuilder("submit").startSpan();Runnable wrapped;
  try(var ignored=parent.makeCurrent()){wrapped=new TelemetryTaskDecorator().decorate(()->{assertEquals(parent.getSpanContext(),Span.current().getSpanContext());throw new IllegalStateException("fixed");});}
  var previous=Span.current().getSpanContext();assertThrows(IllegalStateException.class,wrapped::run);assertEquals(previous,Span.current().getSpanContext());parent.end();assertEquals(1,C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size());
 }
}
