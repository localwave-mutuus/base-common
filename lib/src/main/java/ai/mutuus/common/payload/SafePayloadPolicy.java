package ai.mutuus.common.payload;
import java.util.*;
import java.nio.charset.StandardCharsets;
import ai.mutuus.common.core.SensitiveDataMasker;
import com.fasterxml.jackson.core.*;
/** Only schema-checked scalar projection can reach a typed event writer. */
public final class SafePayloadPolicy {
 public static final Set<String> REASONS=Set.of("POLICY_DENIED","PARSE_ERROR","MASKER_UNAVAILABLE","SCHEMA_MISMATCH","BODY_TOO_LARGE","EVENT_TOO_LARGE");
 public static final class Result {
  private final String policyId,reason;private final Map<String,Object> fields;
  private Result(String id,String reason,Map<String,Object> fields){this.policyId=id;this.reason=reason;this.fields=Map.copyOf(fields);}
  public static Result omitted(String reason){if(!REASONS.contains(reason))throw new IllegalArgumentException("Invalid omission reason");return new Result(null,reason,Map.of());}
  public String policyId(){return policyId;}public String reason(){return reason;}public Map<String,Object> fields(){return fields;}
 }
 private final PayloadPolicyRegistry registry;private final SafePayloadProperties props;private final SensitiveDataMasker masker;
 public SafePayloadPolicy(PayloadPolicyRegistry r,SafePayloadProperties p,SensitiveDataMasker m){registry=r;props=p;masker=m;}
 public Result service(ServicePayloadRule rule,ApprovedInput input,boolean failure){return project("service:"+rule.namespace()+":"+rule.function()+":"+rule.direction(),input,failure);}
 Result http(String method,String route,HttpPayloadRule.Direction direction,byte[] bytes,boolean overflow,boolean failure){
  if(overflow || bytes.length>props.getMaxBodyBytes())return Result.omitted("BODY_TOO_LARGE");
  if(!props.isEnabled() || (props.getMode()==SafePayloadProperties.Mode.ERROR_ONLY && !failure))return Result.omitted("POLICY_DENIED");
  String key="http:"+method+":"+route+":"+direction;
  if(registry.lookup(key)==null)return Result.omitted("POLICY_DENIED");
  try {
   // Decoder rejects incomplete UTF-8 rather than replacing or logging a prefix.
   String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
   Map<String,Object> values=new LinkedHashMap<>();
   JsonFactory factory=JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
   try(JsonParser parser=factory.createParser(text)){
    if(parser.nextToken()!=JsonToken.START_OBJECT)return Result.omitted("PARSE_ERROR");
    while(parser.nextToken()!=JsonToken.END_OBJECT){
     if(parser.currentToken()!=JsonToken.FIELD_NAME || values.size()>=props.getMaxFields())return Result.omitted("SCHEMA_MISMATCH");
     String name=parser.currentName();JsonToken token=parser.nextToken();
     Object value=switch(token){case VALUE_STRING -> parser.getText();case VALUE_NUMBER_INT -> parser.getLongValue();case VALUE_TRUE -> true;case VALUE_FALSE -> false;default -> null;};
     if(value==null || AllowedScalarField.forbidden(name))return Result.omitted("SCHEMA_MISMATCH");
     values.put(name,value);
    }
    if(parser.nextToken()!=null)return Result.omitted("PARSE_ERROR");
   }
   var schema=registry.schema(registry.lookup(key));
   if(values.keySet().stream().anyMatch(n->schema.fields().stream().noneMatch(f->f.name().equals(n))))return Result.omitted("SCHEMA_MISMATCH");
   return project(key,new JsonInput(values),failure);
  }catch(Exception invalid){return Result.omitted("PARSE_ERROR");}
 }
 public static final class JsonInput implements ApprovedInput {
  private final Map<String,Object> fields;private JsonInput(Map<String,Object> fields){this.fields=Map.copyOf(fields);}
  public Object scalar(String name){return fields.get(name);}
 }
 private Result project(String key,ApprovedInput input,boolean failure){
  if(!props.isEnabled() || (props.getMode()==SafePayloadProperties.Mode.ERROR_ONLY && !failure))return Result.omitted("POLICY_DENIED");
  String id=registry.lookup(key);if(id==null)return Result.omitted("POLICY_DENIED");
  if(masker==null)return Result.omitted("MASKER_UNAVAILABLE");
  try {
   Map<String,Object> output=registry.projector(id).project(input);var schema=registry.schema(id);
   if(output==null || output.size()>props.getMaxFields())return Result.omitted("SCHEMA_MISMATCH");
   Map<String,Object> safe=new LinkedHashMap<>();
   for(var item:output.entrySet()){
    var field=schema.fields().stream().filter(f->f.name().equals(item.getKey())).findFirst().orElse(null);
    if(field==null || !field.accepts(item.getValue()))return Result.omitted("SCHEMA_MISMATCH");
    Object v=item.getValue();
    if(v instanceof String text){String masked=masker.mask(text);if(!field.accepts(masked))return Result.omitted("SCHEMA_MISMATCH");v=masked;}
    safe.put(item.getKey(),v);
   }
   var out=new java.io.ByteArrayOutputStream();
   try(var json=new JsonFactory().createGenerator(out)){json.writeStartObject();for(var e:safe.entrySet()){json.writeFieldName(e.getKey());if(e.getValue() instanceof String s)json.writeString(s);else if(e.getValue() instanceof Boolean b)json.writeBoolean(b);else json.writeNumber(((Number)e.getValue()).longValue());}json.writeEndObject();}
   if(out.size()>props.getMaxBodyBytes())return Result.omitted("BODY_TOO_LARGE");
   return new Result(id,null,safe);
  }catch(Exception invalid){return Result.omitted("SCHEMA_MISMATCH");}
 }
}
