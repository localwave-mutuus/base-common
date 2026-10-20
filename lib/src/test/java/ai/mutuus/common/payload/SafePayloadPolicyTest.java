package ai.mutuus.common.payload;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import ai.mutuus.common.core.SensitiveDataMasker;
class SafePayloadPolicyTest {
 record Input(String status)implements ApprovedInput{}
 ServicePayloadRule rule=new ServicePayloadRule("app.Business","run",HttpPayloadRule.Direction.INPUT,"reviewed");
 PayloadPolicyRegistry registry;SafePayloadProperties props;
 PayloadPolicyRegistry registry(SafePayloadProjector projector){
  var r=new PayloadPolicyRegistry();r.register(new ProjectorSchema("reviewed",List.of(new AllowedScalarField("status",AllowedScalarField.ScalarType.STRING,16,Set.of("OK","FAILED")))));r.register("reviewed",projector);r.register(rule);r.register(new HttpPayloadRule("POST","/work/{id}",HttpPayloadRule.Direction.INPUT,"reviewed"));r.validateAndSeal();return r;
 }
 @BeforeEach void setup(){props=new SafePayloadProperties();props.setEnabled(true);registry=registry(i->Map.of("status",i instanceof Input in?in.status():((SafePayloadPolicy.JsonInput)i).scalar("status")));}
 SafePayloadPolicy policy(SensitiveDataMasker masker){return new SafePayloadPolicy(registry,props,masker);}
 @Test void failClosedAndSuccessfulSchemaProjection(){
  var p=policy(new SensitiveDataMasker(List.of()));assertEquals(Map.of("status","OK"),p.service(rule,new Input("OK"),true).fields());
  assertEquals("SCHEMA_MISMATCH",p.service(rule,new Input("secret@example.invalid"),true).reason());assertEquals("POLICY_DENIED",p.service(rule,new Input("OK"),false).reason());
  assertEquals("MASKER_UNAVAILABLE",policy(null).service(rule,new Input("OK"),true).reason());props.setEnabled(false);assertEquals("POLICY_DENIED",p.service(rule,new Input("OK"),true).reason());
 }
 @Test void rejectsSensitiveNestedAndUnknownOutputAndRegistrations(){
  for(String name:List.of("password","authCode","email","phone","fullName","address","payment","account","file","session","token","mailBody"))assertThrows(IllegalArgumentException.class,()->new AllowedScalarField(name,AllowedScalarField.ScalarType.STRING,16,Set.of("OK")));
  registry=registry(i->Map.of("status",Map.of("nested","OK")));assertEquals("SCHEMA_MISMATCH",policy(new SensitiveDataMasker(List.of())).service(rule,new Input("OK"),true).reason());
  registry=registry(i->Map.of("unregistered",true));assertEquals("SCHEMA_MISMATCH",policy(new SensitiveDataMasker(List.of())).service(rule,new Input("OK"),true).reason());
  var r=new PayloadPolicyRegistry();r.register(rule);assertThrows(IllegalArgumentException.class,r::validateAndSeal);assertThrows(IllegalArgumentException.class,()->r.register(rule));assertThrows(IllegalStateException.class,()->registry.register(rule));
 }
 @Test void utf8InputBoundariesAndParseFailures(){
  var p=policy(new SensitiveDataMasker(List.of()));String json="{\"status\":\"OK\"}";
  for(int size:List.of(8191,8192))assertNull(p.http("POST","/work/{id}",HttpPayloadRule.Direction.INPUT,(json+" ".repeat(size-json.length())).getBytes(StandardCharsets.UTF_8),false,true).reason());
  assertEquals("BODY_TOO_LARGE",p.http("POST","/work/{id}",HttpPayloadRule.Direction.INPUT,(json+" ".repeat(8193-json.length())).getBytes(StandardCharsets.UTF_8),false,true).reason());
  assertEquals("PARSE_ERROR",p.http("POST","/work/{id}",HttpPayloadRule.Direction.INPUT,new byte[]{(byte)0xe3},false,true).reason());
  for(String raw:List.of("{\"status\":{}}","{\"status\":[\"OK\"]}","{\"status\":\"OK\",\"email\":\"private\"}"))assertEquals("SCHEMA_MISMATCH",p.http("POST","/work/{id}",HttpPayloadRule.Direction.INPUT,raw.getBytes(StandardCharsets.UTF_8),false,true).reason());
 }
 @Test void byteLimitsCannotBeRaisedAndLegacyLimitRemains4096(){assertThrows(IllegalArgumentException.class,()->props.setMaxBodyBytes(8193));assertThrows(IllegalArgumentException.class,()->props.setMaxEventBytes(32769));assertEquals(4096,new PayloadLoggingProperties().getMaxBodyBytes());}
}
