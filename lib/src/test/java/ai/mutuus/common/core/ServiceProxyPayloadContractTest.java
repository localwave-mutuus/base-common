package ai.mutuus.common.core;
import ai.mutuus.common.aop.*;
import ai.mutuus.common.payload.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
class ServiceProxyPayloadContractTest {
 record TechnicalInput(String status)implements ApprovedInput{}
 @Service public static class Business {
  final CompletableFuture<TechnicalInput> result=new CompletableFuture<>();
  @ObservedServiceOperation public CompletionStage<TechnicalInput> submit(TechnicalInput input){return result;}
 }
 @Test void realSpringProxyAndAsyncSafePayloadKeepOneInvocation(){
  C3TelemetryFixture.init();var props=new ServiceMethodLoggingProperties();props.setEnabled(true);props.setPayloadEnabled(true);props.setIncludePackages(List.of("ai.mutuus.common.core"));props.setIncludeMethods(List.of("submit"));
  var registry=new PayloadPolicyRegistry();registry.register(new ProjectorSchema("technical",List.of(new AllowedScalarField("status",AllowedScalarField.ScalarType.STRING,16,Set.of("OK")))));registry.register("technical",i->Map.of("status",((TechnicalInput)i).status()));
  for(var direction:HttpPayloadRule.Direction.values())registry.register(new ServicePayloadRule(Business.class.getName(),"submit",direction,"technical"));registry.validateAndSeal();
  var pp=new SafePayloadProperties();pp.setEnabled(true);pp.setMode(SafePayloadProperties.Mode.ALL);var policy=new SafePayloadPolicy(registry,pp,new SensitiveDataMasker(List.of()));
  var events=new ArrayList<Map<String,Object>>();var target=new Business();var factory=new AspectJProxyFactory(target);factory.addAspect(new ServiceMethodLoggingAspect(props,(a,c,n,f,o,e,t)->events.add(c),policy));Business proxy=factory.getProxy();
  var stage=proxy.submit(new TechnicalInput("OK"));assertSame(target.result,stage);assertEquals(1,events.size());
  CompletableFuture.runAsync(()->target.result.complete(new TechnicalInput("OK"))).join();assertEquals(2,events.size());assertEquals(events.get(0),events.get(1));assertEquals(1,C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size());assertNull(ServiceInvocationScope.currentInvocationId());
 }
}
