package ai.mutuus.common.logging;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.*;
import org.slf4j.event.KeyValuePair;
import com.fasterxml.jackson.core.*;
class BoundedEnvelopeSafetyTest {
 @Test void lowerLimitPreservesWholeJsonAndRequiredIdentifiers()throws Exception{
  var fields=new LinkedHashMap<String,Object>();
  for(String k:List.of("message","log.logger","service.name","service.version","deployment.environment.name","golmok.scope","golmok.auth.state","event.action","event.dataset","event.outcome"))fields.put(k,"\n\"\\".repeat(2000));
  fields.put("event.id","12345678-1234-1234-1234-123456789abc");fields.put("golmok.schema.version",2);fields.put("@timestamp","2026-10-20T22:00:00.123456789Z");fields.put("trace.id","1234567890abcdef1234567890abcdef");fields.put("span.id","1234567890abcdef");fields.put("parent.id","abcdef1234567890");
  var encoder=new CommonJsonEncoder();encoder.setMaxEventBytes(2048);byte[] out=encoder.encodeSnapshot(fields);assertTrue(out.length<=2048);
  String json=new String(out,java.nio.charset.StandardCharsets.UTF_8);assertTrue(json.contains("12345678-1234-1234-1234-123456789abc"));assertTrue(json.contains("1234567890abcdef1234567890abcdef"));
  try(var p=new JsonFactory().createParser(out)){assertEquals(JsonToken.START_OBJECT,p.nextToken());p.skipChildren();assertNull(p.nextToken());}
 }
 @Test void unapprovedErrorCodeClassAndSecurityFreeTextAreBlocked(){
  var event=new LoggingEvent();event.setLoggerName("test");event.setLevel(Level.ERROR);event.setTimeStamp(System.currentTimeMillis());event.setMessage("private");event.setMDCPropertyMap(Map.of());
  for(String k:List.of("error.code","error.type","golmok.security.reason","golmok.security.detail","url.full"))event.addKeyValuePair(new KeyValuePair(k,"private@example.invalid"));
  var snapshot=CanonicalLogSnapshot.capture(event);assertEquals("OPERATION_FAILED",snapshot.get("error.code"));assertFalse(snapshot.toString().contains("private@example.invalid"));
 }
}
