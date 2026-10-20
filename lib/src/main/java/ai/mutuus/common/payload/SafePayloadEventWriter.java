package ai.mutuus.common.payload;
/** Typed results only. No raw KV/body overload. */
public final class SafePayloadEventWriter {
 public static final String KEY="__common_safe_payload";
 public void write(SafePayloadPolicy.Result result){
  org.slf4j.LoggerFactory.getLogger("ai.mutuus.common.payload").atInfo()
   .addKeyValue("event.dataset","mutuus.payload").addKeyValue("event.action","payload.evaluated")
   .addKeyValue(KEY,java.util.Objects.requireNonNull(result)).log("payload.evaluated");
 }
}
