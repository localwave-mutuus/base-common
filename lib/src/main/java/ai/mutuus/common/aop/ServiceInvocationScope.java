package ai.mutuus.common.aop;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
/** Explicit INTERNAL boundary; bind only on the thread executing business code. */
public final class ServiceInvocationScope implements AutoCloseable {
 private static final ContextKey<String> ID=ContextKey.named("common.service.invocation");
 private final Span span;
 private final Context context;
 private final Map<String,Object> fields;
 private final ServiceMethodLoggingWriter writer;
 private final String namespace,function;
 private final long started=System.nanoTime();
 private final AtomicBoolean ended=new AtomicBoolean();
 private ServiceInvocationScope(String namespace,String function,ServiceMethodLoggingWriter writer){
  this.namespace=Objects.requireNonNull(namespace);this.function=Objects.requireNonNull(function);this.writer=Objects.requireNonNull(writer);
  String id=UUID.randomUUID().toString();
  span=GlobalOpenTelemetry.getTracer("ai.mutuus.common.service").spanBuilder("service.operation").setSpanKind(SpanKind.INTERNAL).setParent(Context.current()).startSpan();
  context=Context.current().with(span).with(ID,id);
  try(var ignored=context.makeCurrent()) {fields=ai.mutuus.common.logging.CanonicalLogSnapshot.context();}
  try {writer.write("service.method.enter",fields,namespace,function,null,null,0);}
  catch(RuntimeException|Error ex){span.end();throw ex;}
 }
 public static ServiceInvocationScope open(String namespace,String function,ServiceMethodLoggingWriter writer){return new ServiceInvocationScope(namespace,function,writer);}
 public static String currentInvocationId(){return Context.current().get(ID);}
 public Scope makeCurrent(){return context.makeCurrent();}
 public void success(){finish("success",null);}
 public void fail(Throwable error){
  while((error instanceof CompletionException || error instanceof ExecutionException) && error.getCause()!=null)error=error.getCause();
  if(error instanceof CancellationException || error instanceof InterruptedException)finish("cancelled","SERVICE_CANCELLED");
  else finish("failure","SERVICE_FAILED");
 }
 private void finish(String outcome,String code){if(ended.compareAndSet(false,true)){
  try {if(!"success".equals(outcome))span.setStatus(StatusCode.ERROR);
   try(var ignored=context.makeCurrent()){writer.write("service.method.exit",fields,namespace,function,outcome,code,System.nanoTime()-started);}
  }finally{span.end();}
 }}
 @Override public void close(){finish("failure","SERVICE_SCOPE_UNFINISHED");}
}
