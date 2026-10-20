package ai.mutuus.common.payload;
public record ServicePayloadRule(String namespace,String function,HttpPayloadRule.Direction direction,String projectorId){
 public ServicePayloadRule{
  if(namespace==null || !namespace.matches("[A-Za-z_$][A-Za-z0-9_$.]*") || function==null || !function.matches("[A-Za-z_$][A-Za-z0-9_$]*"))throw new IllegalArgumentException("Exact service rule required");
  java.util.Objects.requireNonNull(direction);java.util.Objects.requireNonNull(projectorId);
 }
}
