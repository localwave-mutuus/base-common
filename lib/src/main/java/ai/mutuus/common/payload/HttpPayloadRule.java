package ai.mutuus.common.payload;
public record HttpPayloadRule(String method,String routeTemplate,Direction direction,String projectorId){
 public enum Direction { INPUT, OUTPUT }
 public HttpPayloadRule{
  if(method==null || !method.matches("GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS") || routeTemplate==null || !routeTemplate.matches("/[A-Za-z0-9_/{}/.-]*") || routeTemplate.contains(".."))throw new IllegalArgumentException("Exact HTTP rule required");
  java.util.Objects.requireNonNull(direction);java.util.Objects.requireNonNull(projectorId);
 }
}
