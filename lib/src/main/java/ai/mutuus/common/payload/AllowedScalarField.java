package ai.mutuus.common.payload;
import java.util.*;
public record AllowedScalarField(String name, ScalarType scalarType, int maxUtf8Bytes, Set<String> allowedEnumValues) {
 public enum ScalarType { STRING, LONG, BOOLEAN }
 public AllowedScalarField {
  if(name==null || !name.matches("[a-z][a-zA-Z0-9_]{0,63}") || forbidden(name))throw new IllegalArgumentException("Forbidden payload field");
  Objects.requireNonNull(scalarType);
  if(maxUtf8Bytes<1 || maxUtf8Bytes>8192)throw new IllegalArgumentException("Invalid scalar bound");
  allowedEnumValues=Set.copyOf(allowedEnumValues);
  if(scalarType==ScalarType.STRING && allowedEnumValues.isEmpty())throw new IllegalArgumentException("Strings require approved technical enum values");
  for(String value:allowedEnumValues)if(!value.matches("[A-Z][A-Z0-9_]{0,79}") || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>maxUtf8Bytes)throw new IllegalArgumentException("Invalid enum");
 }
 static boolean forbidden(String name){return name.toLowerCase(Locale.ROOT).matches(".*(password|passwd|secret|token|auth|session|cookie|email|phone|mobile|contact|name|address|body|content|payment|account|card|file|binary|multipart|otp|credential|identifier|userid|verification|confirmation|passcode|ssn|birth|gender|salary|iban|bank|latitude|longitude|location).* ".trim());}
 public boolean accepts(Object v){return switch(scalarType){
  case STRING -> v instanceof String s && allowedEnumValues.contains(s) && s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=maxUtf8Bytes;
  case LONG -> v instanceof Byte || v instanceof Short || v instanceof Integer || v instanceof Long;
  case BOOLEAN -> v instanceof Boolean;
 };}
}
