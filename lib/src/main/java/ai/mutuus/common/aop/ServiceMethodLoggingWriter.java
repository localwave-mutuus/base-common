package ai.mutuus.common.aop;
import java.util.Map;
@FunctionalInterface
public interface ServiceMethodLoggingWriter {
 void write(String action, Map<String,Object> context, String namespace, String function, String outcome, String errorCode, long nanos);
 static ServiceMethodLoggingWriter standard() {
  return (action,context,namespace,function,outcome,error,nanos) -> {
   var e=org.slf4j.LoggerFactory.getLogger("ai.mutuus.common.service").atInfo()
    .addKeyValue(ai.mutuus.common.logging.CanonicalLogSnapshot.CONTEXT_KEY,context)
    .addKeyValue("event.dataset","mutuus.service").addKeyValue("event.action",action)
    .addKeyValue("code.namespace",namespace).addKeyValue("code.function",function);
   if(outcome!=null)e.addKeyValue("event.outcome",outcome).addKeyValue("event.duration",nanos);
   if(error!=null)e.addKeyValue("error.code",error);
   e.log(action);
  };
 }
}
