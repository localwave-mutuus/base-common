package ai.mutuus.common.agent.transport;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

/** Bootstrap transport helper: only java.base/java.management and agent APIs, no java.net.http linkage. */
public final class UrlTransportState {
    private static final Map<Object,Attempt> active=Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Instrumenter<URL,Integer> instrumenter=Instrumenter
        .<URL,Integer>builder(GlobalOpenTelemetry.get(),"ai.mutuus.common.url-transport",url->"HTTP")
        .buildInstrumenter(url->SpanKind.CLIENT);
    private static Object invoke(String action,Object[] args,String[] types){
        try{return java.lang.management.ManagementFactory.getPlatformMBeanServer().invoke(
            new javax.management.ObjectName("ai.mutuus.common:type=SpanLineage"),action,args,types);}
        catch(Exception error){throw new IllegalStateException("Transport bridge unavailable",error);}
    }
    private static Object header(Object headers,String method,Class<?>[] types,Object... args){
        try{return headers.getClass().getMethod(method,types).invoke(headers,args);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Pinned MessageHeader access unavailable",failure);}
    }
    public static Attempt begin(Object client,URL url,Object headers){
        var previous=active.remove(client);
        if(previous!=null)previous.finish(new java.io.IOException());
        synchronized(active){
            if(active.size()>=100000)throw new IllegalStateException("Transport attempt capacity exceeded");
            String line=(String)header(headers,"getKey",new Class<?>[]{int.class},0);
            String method=line.substring(0,line.indexOf(' '));
            var attempt=new Attempt(url,method,instrumenter.start(Context.current(),url));
            active.put(client,attempt);
            for(String name:List.of("traceparent","tracestate","baggage"))
                header(headers,"remove",new Class<?>[]{String.class},name);
            if(Boolean.TRUE.equals(invoke("approvedHost",new Object[]{url.getHost()},new String[]{"java.lang.String"})))
                W3CTraceContextPropagator.getInstance().inject(attempt.context,headers,
                    (carrier,key,value)->header(carrier,"set",new Class<?>[]{String.class,String.class},key,value));
            return attempt;
        }
    }
    public static Attempt current(Object client){return active.get(client);}
    public static void finish(Object client,Attempt attempt,Object headers,Throwable error){
        if(attempt==null)return;
        if(error==null){
            String line=(String)header(headers,"getValue",new Class<?>[]{int.class},0);
            if(line!=null && line.startsWith("HTTP/")) {
                int status=Integer.parseInt(line.split(" ",3)[1]);
                if(status<200)return;
                attempt.status=status;
            }
        }
        attempt.finish(error);
        active.remove(client,attempt);
    }
    public static final class Attempt {
        final URL url;final String method;final Context context;final long started=System.nanoTime();
        final AtomicBoolean ended=new AtomicBoolean();volatile Integer status;
        Attempt(URL url,String method,Context context){this.url=url;this.method=method;this.context=context;}
        void finish(Throwable error){
            if(!ended.compareAndSet(false,true))return;
            var id=Span.fromContext(context).getSpanContext();
            Object parent=invoke("parentId",new Object[]{id.getTraceId(),id.getSpanId()},new String[]{"java.lang.String","java.lang.String"});
            instrumenter.end(context,url,status,error);
            var fields=new HashMap<String,Object>();
            fields.put("trace.id",id.getTraceId());fields.put("span.id",id.getSpanId());
            fields.put("trace.flags",Byte.toUnsignedInt(id.getTraceFlags().asByte()));
            if(parent instanceof String text && !text.isEmpty())fields.put("parent.id",text);
            fields.put("golmok.request.id",UUID.randomUUID().toString());
            fields.put("event.duration",Math.max(0,System.nanoTime()-started));fields.put("http.request.method",method);
            String host=url.getHost();
            if(host!=null && !host.isEmpty())fields.put("server.address",host);
            if(status!=null)fields.put("http.response.status_code",status);
            String code=error instanceof java.net.SocketTimeoutException ? "HTTP_TIMEOUT"
                : error!=null ? "HTTP_IO_ERROR" : status!=null && status>=400 ? "HTTP_"+status : null;
            fields.put("event.outcome",code==null && status!=null ? "success":"failure");
            if(code!=null)fields.put("error.code",code);
            invoke("transportComplete",new Object[]{fields},new String[]{"java.util.Map"});
        }
    }
}
