package ai.mutuus.common.logging;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.*;
import org.slf4j.event.KeyValuePair;
import java.util.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.*;
class CanonicalSafeOutputTest {
 @Test void rawMessageStackLegacyBodiesAndProtectedKvCannotLeak(){
  var e=new LoggingEvent();e.setTimeStamp(System.currentTimeMillis());e.setLoggerName("test");e.setLevel(Level.ERROR);e.setMessage("private@example.invalid");e.setMDCPropertyMap(Map.of("golmok.payload.policy.id","forged"));e.setThrowableProxy(new ThrowableProxy(new IllegalStateException("private@example.invalid")));
  for(String k:List.of("http.request.body.content","http.response.body.content","mutuus.method.args","mutuus.method.return","error.message","url.query","golmok.payload.policy.id"))e.addKeyValuePair(new KeyValuePair(k,"private@example.invalid"));
  e.addKeyValuePair(new KeyValuePair(CanonicalLogSnapshot.CONTEXT_KEY,Map.of("golmok.payload.policy.id","forged")));
  var snapshot=CanonicalLogSnapshot.capture(e);assertEquals("telemetry.event",snapshot.get("message"));assertFalse(snapshot.toString().contains("private@example.invalid"));assertFalse(snapshot.containsKey("error.stack_trace"));assertFalse(snapshot.containsKey("golmok.payload.policy.id"));
 }
 @Test void jsonByteBoundaryRemainsValidAndDropsWholeMetadata()throws Exception{
  var encoder=new CommonJsonEncoder();var fields=new LinkedHashMap<String,Object>();
  fields.put("@timestamp","2026-10-20T00:00:00Z");fields.put("event.id",UUID.randomUUID().toString());fields.put("golmok.schema.version",2);fields.put("message","telemetry.event");fields.put("event.action","service.method.exit");
  fields.put("url.path","");int overhead=encoder.encodeSnapshot(fields).length;
  for(int size:new int[]{32767,32768,32769}){
   fields.put("url.path","가".repeat((size-overhead)/3)+"x".repeat((size-overhead)%3));
   byte[] out=encoder.encodeSnapshot(fields);assertTrue(out.length<=32768);assertEquals(size<=32768?size:encoder.encodeSnapshot(fields).length,out.length);
   try(var p=new JsonFactory().createParser(out)){assertEquals(JsonToken.START_OBJECT,p.nextToken());p.skipChildren();assertNull(p.nextToken());}
   String json=new String(out,StandardCharsets.UTF_8);assertEquals(size>32768,json.contains("EVENT_TOO_LARGE"));if(size>32768)assertFalse(json.contains("가"));
  }
 }
}
