package ai.mutuus.common.aop;
import ai.mutuus.common.core.SensitiveDataMasker;
/** Compatibility facade: legacy opt-in cannot authorize DTO rendering. */
public class DefaultMethodLoggingWriter implements MethodLoggingWriter {
 public static final String LOGGER_NAME="ai.mutuus.common.method";
 public DefaultMethodLoggingWriter(int maxLength){this(maxLength,null);}
 public DefaultMethodLoggingWriter(int maxLength,SensitiveDataMasker masker) { }
 @Override public void onEnter(String type,String method,Object[] args){emit("method.enter",type,method);}
 @Override public void onExit(String type,String method,Object result){emit("method.exit",type,method);}
 private void emit(String action,String type,String method){
  org.slf4j.LoggerFactory.getLogger(LOGGER_NAME).atInfo().addKeyValue("event.dataset","mutuus.method")
   .addKeyValue("event.action",action).addKeyValue("code.namespace",type).addKeyValue("code.function",method).log(action);
 }
}
