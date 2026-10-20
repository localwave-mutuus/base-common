package ai.mutuus.common.logging;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.io.*;
class RegistryV5ContractTest {
 @Test void v5AddsOnlyFourTypedFieldsAndRetainsV4NamesAndTypes()throws Exception{
  var r=TelemetryFieldRegistry.load();assertEquals(5,r.version());assertEquals(94,r.fields().size());
  // Fixture is the v4 registry from the preserved input patch, independent of v5 generation.
  try(var input=getClass().getResourceAsStream("/common-platform/log-fields.v4.fixture.json")){
   var old=TelemetryFieldRegistry.from(input);assertEquals(4,old.version());assertEquals(90,old.fields().size());
   old.fields().forEach((n,f)->assertEquals(f.type(),r.fields().get(n).type(),n));
   var added=new HashSet<>(r.fields().keySet());added.removeAll(old.fields().keySet());assertEquals(Set.of("golmok.service.invocation.id","golmok.payload.policy.id","golmok.payload.omitted.reason","golmok.payload.truncated"),added);
   for(String n:added){assertTrue(r.fields().get(n).protectedField());assertFalse(r.fields().get(n).mdcInput());assertFalse(r.fields().get(n).structuredInput());}
  }
  assertFalse(r.fields().containsKey("golmok.screen.internal_id"));assertTrue(r.fields().containsKey("golmok.ui.element.id"));assertTrue(r.fields().containsKey("golmok.ui.action"));
  assertThrows(IllegalArgumentException.class,()->r.validateSnapshot(Map.of("golmok.payload.omitted.reason","PRIVATE")));
  assertThrows(IllegalArgumentException.class,()->r.validateSnapshot(Map.of("golmok.payload.truncated","true")));
 }
}
