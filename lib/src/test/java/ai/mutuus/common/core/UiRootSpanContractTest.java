package ai.mutuus.common.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class UiRootSpanContractTest {
 @Test void uiHintHasExplicitSystemAndRootSpanShape(){
  var alias=ai.mutuus.common.uicatalog.UiCodeAliases.ALIASES.stream().filter(a->a.system().equals("BO") && a.status().equals("RESOLVED") && a.target().matches("BO-[A-Z]{3}-[0-9]{3}")).findFirst().orElseThrow();
  var h=new HashMap<String,String>();h.put(HeaderNames.SCREEN_ID,alias.target());h.put(HeaderNames.SCREEN_CATALOG_VERSION,alias.version());h.put(HeaderNames.EVENT_ID,"1234567890abcdef");
  assertTrue(ScreenMetadata.fromHeaders((String)null,h::get).isEmpty());
  assertEquals("1234567890abcdef",ScreenMetadata.fromHeaders("BO",h::get).get(HeaderNames.EVENT_ID));
  for(String invalid:List.of("0000000000000000","ABCDEF1234567890","1234567890abcdef1234567890abcdef",UUID.randomUUID().toString())){h.put(HeaderNames.EVENT_ID,invalid);assertFalse(ScreenMetadata.fromHeaders("BO",h::get).containsKey(HeaderNames.EVENT_ID));}
  h.put(HeaderNames.SCREEN_CATALOG_VERSION,"wrong");assertTrue(ScreenMetadata.fromHeaders("BO",h::get).isEmpty());
 }
}
