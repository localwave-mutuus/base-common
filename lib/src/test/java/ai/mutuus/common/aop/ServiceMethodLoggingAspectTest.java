package ai.mutuus.common.aop;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import java.util.*;
import java.util.concurrent.*;
import ai.mutuus.common.core.C3TelemetryFixture;
import io.opentelemetry.api.*;
import io.opentelemetry.api.trace.*;
class ServiceMethodLoggingAspectTest {
 public static class Business {
  @ObservedServiceOperation public Object run(){return null;}
  public Object other(){return null;}
  @ObservedServiceOperation public java.util.concurrent.Flow.Publisher<String> reactive(){return null;}
 }
 record Event(String action,Map<String,Object> context,String outcome,String error,long nanos){}
 List<Event> events;ServiceMethodLoggingProperties p;ServiceMethodLoggingAspect aspect;
 @BeforeEach void setup(){C3TelemetryFixture.init();events=new ArrayList<>();p=new ServiceMethodLoggingProperties();p.setEnabled(true);p.setIncludePackages(List.of("ai.mutuus.common"));p.setIncludeMethods(List.of("*"));aspect=new ServiceMethodLoggingAspect(p,(a,c,n,f,o,e,t)->events.add(new Event(a,c,o,e,t)));}
 ProceedingJoinPoint call(String method,Object result,Throwable failure)throws Throwable{
  var jp=mock(ProceedingJoinPoint.class);var sig=mock(MethodSignature.class);
  when(jp.getSignature()).thenReturn(sig);when(sig.getMethod()).thenReturn(Business.class.getMethod(method));when(jp.getTarget()).thenReturn(new Business());
  if(failure!=null)when(jp.proceed()).thenThrow(failure);else when(jp.proceed()).thenReturn(result);return jp;
 }
 @Test void selectionIsOptInAndExcludeWins()throws Exception{
  var m=Business.class.getMethod("run");assertTrue(aspect.matches(m));assertFalse(aspect.matches(Business.class.getMethod("other")));
  p.setExcludeMethods(List.of("run"));assertFalse(aspect.matches(m));p.setExcludeMethods(List.of());p.setIncludePackages(List.of());assertFalse(aspect.matches(m));
  p.setIncludePackages(List.of("ai.mutuus.common"));p.setIncludeMethods(List.of());assertFalse(aspect.matches(m));p.setIncludeMethods(List.of("*"));p.setEnabled(false);assertFalse(aspect.matches(m));
 }
 @Test void syncExceptionAndCancelEachHaveOneInternalPair()throws Throwable{
  var parent=GlobalOpenTelemetry.getTracer("test").spanBuilder("parent").startSpan();
  try(var ignored=parent.makeCurrent()){
   assertEquals("ok",aspect.observe(call("run","ok",null)));
   assertThrows(IllegalStateException.class,()->aspect.observe(call("run",null,new IllegalStateException("private-body"))));
   assertThrows(CancellationException.class,()->aspect.observe(call("run",null,new CancellationException("private-body"))));
  }finally{parent.end();}
  assertEquals(6,events.size());assertEquals(List.of("success","failure","cancelled"),List.of(events.get(1).outcome(),events.get(3).outcome(),events.get(5).outcome()));
  for(int i=0;i<6;i+=2){assertEquals(events.get(i).context(),events.get(i+1).context());UUID.fromString((String)events.get(i).context().get("golmok.service.invocation.id"));assertTrue(events.get(i+1).nanos()>=0);}
  var spans=C3TelemetryFixture.EXPORTER.getFinishedSpanItems();assertEquals(4,spans.size());
  for(var s:spans)if(!s.getName().equals("parent")){assertEquals(SpanKind.INTERNAL,s.getKind());assertEquals(parent.getSpanContext().getSpanId(),s.getParentSpanId());}
 }
 @Test void completionStageWaitsAndRestoresThreadContext()throws Throwable{
  var future=new CompletableFuture<String>();var before=Span.current().getSpanContext();
  assertSame(future,aspect.observe(call("run",future,null)));assertEquals(1,events.size());assertEquals(0,C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size());assertEquals(before,Span.current().getSpanContext());
  CompletableFuture.runAsync(()->future.complete("ok")).join();assertEquals(2,events.size());assertEquals("success",events.get(1).outcome());assertEquals(events.get(0).context(),events.get(1).context());assertEquals(1,C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size());
 }
 @Test void asyncCancellationAndFailureCloseExactlyOnce()throws Throwable{
  var cancelled=new CompletableFuture<String>();aspect.observe(call("run",cancelled,null));cancelled.cancel(false);cancelled.cancel(false);
  var failure=new CompletableFuture<String>();aspect.observe(call("run",failure,null));failure.completeExceptionally(new CompletionException(new IllegalStateException("private")));
  assertEquals(4,events.size());assertEquals("cancelled",events.get(1).outcome());assertEquals("failure",events.get(3).outcome());assertEquals(2,C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size());
 }
 @Test void reactiveOptInRejectedBeforeBusinessExecution()throws Throwable{
  var jp=call("reactive",null,null);assertThrows(IllegalArgumentException.class,()->aspect.observe(jp));verify(jp,never()).proceed();assertTrue(events.isEmpty());
 }
 @Test void explicitScopeCasAndFinallyRestore(){
  var scope=ServiceInvocationScope.open("Business","run",(a,c,n,f,o,e,t)->events.add(new Event(a,c,o,e,t)));
  try(var ignored=scope.makeCurrent()){assertNotNull(ServiceInvocationScope.currentInvocationId());scope.success();scope.fail(new RuntimeException());scope.close();}
  assertNull(ServiceInvocationScope.currentInvocationId());assertEquals(2,events.size());assertEquals(1,C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size());
 }
}
