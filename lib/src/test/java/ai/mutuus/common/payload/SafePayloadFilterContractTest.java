package ai.mutuus.common.payload;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.web.*;
import org.springframework.web.servlet.HandlerMapping;
import java.util.*;
import ch.qos.logback.classic.*;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;
class SafePayloadFilterContractTest {
 @Test void partialBodyAndNonJsonNeverBecomeApprovedPayload()throws Exception{
  var props=new SafePayloadProperties();props.setEnabled(true);props.setMode(SafePayloadProperties.Mode.ALL);
  var registry=new PayloadPolicyRegistry();registry.register(new ProjectorSchema("status",List.of(new AllowedScalarField("status",AllowedScalarField.ScalarType.STRING,8,Set.of("OK")))));registry.register("status",i->Map.of("status",((SafePayloadPolicy.JsonInput)i).scalar("status")));registry.register(new HttpPayloadRule("POST","/work",HttpPayloadRule.Direction.INPUT,"status"));registry.validateAndSeal();
  var filter=new SafePayloadFilter(new SafePayloadPolicy(registry,props,new ai.mutuus.common.core.SensitiveDataMasker(List.of())),props);
  var logger=(Logger)LoggerFactory.getLogger("ai.mutuus.common.payload");var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
  try{
   var request=new MockHttpServletRequest("POST","/work");request.setContentType("application/json");request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"/work");request.setContent("{\"status\":\"OK\"} private tail".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   filter.doFilter(request,new MockHttpServletResponse(),(r,s)->r.getInputStream().readNBytes(15));
   var first=appender.list.get(0).getKeyValuePairs().stream().filter(k->k.key.equals(SafePayloadEventWriter.KEY)).findFirst().orElseThrow();assertEquals("PARSE_ERROR",((SafePayloadPolicy.Result)first.value).reason());
   appender.list.clear();request=new MockHttpServletRequest("POST","/work");request.setContentType("multipart/form-data");request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"/work");request.setContent("{\"status\":\"OK\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   filter.doFilter(request,new MockHttpServletResponse(),(r,s)->r.getInputStream().readAllBytes());
   var denied=appender.list.get(0).getKeyValuePairs().stream().filter(k->k.key.equals(SafePayloadEventWriter.KEY)).findFirst().orElseThrow();assertEquals("POLICY_DENIED",((SafePayloadPolicy.Result)denied.value).reason());
  }finally{logger.detachAppender(appender);appender.stop();}
 }
}
