package ai.mutuus.common.payload;
import java.util.*;
/** Registration closes at application startup; no runtime widening. */
public final class PayloadPolicyRegistry {
 private final Map<String,ProjectorSchema> schemas=new LinkedHashMap<>();
 private final Map<String,SafePayloadProjector> projectors=new LinkedHashMap<>();
 private final Map<String,String> rules=new LinkedHashMap<>();
 private boolean sealed;
 public synchronized void register(ProjectorSchema schema){mutable();if(schemas.putIfAbsent(schema.projectorId(),schema)!=null)throw new IllegalArgumentException("Duplicate schema");}
 public synchronized void register(String id,SafePayloadProjector projector){mutable();if(projectors.putIfAbsent(id,Objects.requireNonNull(projector))!=null)throw new IllegalArgumentException("Duplicate projector");}
 public synchronized void register(HttpPayloadRule rule){rule("http:"+rule.method()+":"+rule.routeTemplate()+":"+rule.direction(),rule.projectorId());}
 public synchronized void register(ServicePayloadRule rule){rule("service:"+rule.namespace()+":"+rule.function()+":"+rule.direction(),rule.projectorId());}
 private void rule(String key,String id){mutable();if(rules.putIfAbsent(key,id)!=null)throw new IllegalArgumentException("Duplicate/conflicting rule");}
 private void mutable(){if(sealed)throw new IllegalStateException("Payload registry sealed");}
 public synchronized void validateAndSeal(){
  if(!schemas.keySet().equals(projectors.keySet()) || !schemas.keySet().containsAll(rules.values()))throw new IllegalArgumentException("Unregistered payload projector/schema");
  sealed=true;
 }
 synchronized String lookup(String key){if(!sealed)throw new IllegalStateException("Payload registry not sealed");return rules.get(key);}
 ProjectorSchema schema(String id){return schemas.get(id);} SafePayloadProjector projector(String id){return projectors.get(id);}
}
