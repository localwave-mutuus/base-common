package ai.mutuus.common.agent.transport;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Isolated agent helper; no application SDK, logger, reflection or private module access. */
public final class JdkTransportState {
    private static final Map<Object,Context> calls=Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<Object,String> observations=Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<Object,Attempt> exchanges=Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<HttpRequest,Attempt> requests=Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Set<String> PROPAGATION=Set.of("traceparent","tracestate","baggage");
    private static final Instrumenter<HttpRequest,Integer> instrumenter=Instrumenter
        .<HttpRequest,Integer>builder(GlobalOpenTelemetry.get(),"ai.mutuus.common.jdk-transport",r->"HTTP "+r.method())
        .buildInstrumenter(r->SpanKind.CLIENT);
    private static final int MAX_ACTIVE=100000;
    public static void capture(Object multi) {
        synchronized(calls) {
            if(calls.size()>=MAX_ACTIVE)throw new IllegalStateException("Transport call capacity exceeded");
            calls.put(multi,Context.current());
            Object token=invoke("currentClientObservation",new Object[0],new String[0]);
            if(token instanceof String text && !text.isEmpty())observations.put(multi,text);
        }
    }
    public static void release(Object multi,Throwable error) {
        calls.remove(multi);observations.remove(multi);
        if(error!=null) {
            java.util.List<Attempt> pending;
            synchronized(exchanges) { pending=exchanges.values().stream().filter(a->a.multi==multi).toList(); }
            pending.forEach(a->a.finish(error));
        }
    }
    public static Attempt begin(Object exchange,Object multi,HttpRequest request) {
        Context parent=calls.get(multi);
        if(parent==null) throw new IllegalStateException("Missing pinned JDK call context");
        synchronized(exchanges) {
            if(exchanges.size()>=MAX_ACTIVE)throw new IllegalStateException("Transport attempt capacity exceeded");
            Attempt attempt=new Attempt(multi,request,instrumenter.start(parent,request));
            exchanges.put(exchange,attempt);requests.put(request,attempt);
            String token=observations.get(multi);
            if(token!=null){
                var id=Span.fromContext(attempt.context).getSpanContext();
                invoke("attachTransportClient",new Object[]{token,id.getTraceId(),id.getSpanId()},
                    new String[]{"java.lang.String","java.lang.String","java.lang.String"});
            }
            return attempt;
        }
    }
    public static void status(Object exchange,int status) {
        Attempt attempt=exchanges.get(exchange);if(attempt!=null && status>=200)attempt.status=status;
    }
    public static HttpHeaders headers(HttpRequest request,HttpHeaders original) {
        var result=new TreeMap<String,List<String>>(String.CASE_INSENSITIVE_ORDER);
        original.map().forEach((key,value)->{if(!PROPAGATION.contains(key.toLowerCase(Locale.ROOT)))result.put(key,value);});
        Attempt attempt=requests.get(request);
        // The URI belongs to this transport request, including redirected and retried requests.
        if(attempt!=null && Boolean.TRUE.equals(invoke("approvedHost",new Object[]{request.uri().getHost()},new String[]{"java.lang.String"})))
            W3CTraceContextPropagator.getInstance().inject(attempt.context,result,(carrier,key,value)->carrier.put(key,List.of(value)));
        return HttpHeaders.of(result,(key,value)->true);
    }
    public static Object invoke(String action,Object[] args,String[] types) {
        try { return ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("ai.mutuus.common:type=SpanLineage"),action,args,types); }
        catch(Exception failure) { throw new IllegalStateException("Transport bridge unavailable"); }
    }
    public static final class Attempt {
        final Object multi;final HttpRequest request;final Context context;final long started=System.nanoTime();
        final AtomicBoolean ended=new AtomicBoolean();volatile Integer status;
        Attempt(Object multi,HttpRequest request,Context context) {this.multi=multi;this.request=request;this.context=context;}
        public void finish(Throwable error) {
            if(!ended.compareAndSet(false,true))return;
            var id=Span.fromContext(context).getSpanContext();
            Object parent=invoke("parentId",new Object[]{id.getTraceId(),id.getSpanId()},new String[]{"java.lang.String","java.lang.String"});
            instrumenter.end(context,request,status,error);
            requests.remove(request,this);
            synchronized(exchanges) { exchanges.values().removeIf(value->value==this); }
            var span=Span.fromContext(context).getSpanContext();
            var fields=new HashMap<String,Object>();
            fields.put("trace.id",span.getTraceId());fields.put("span.id",span.getSpanId());
            fields.put("trace.flags",Byte.toUnsignedInt(span.getTraceFlags().asByte()));
            if(parent instanceof String text && !text.isEmpty())fields.put("parent.id",text);
            fields.put("golmok.request.id",UUID.randomUUID().toString());
            fields.put("event.duration",Math.max(0,System.nanoTime()-started));fields.put("http.request.method",request.method());
            String host=request.uri().getHost();
            if(host!=null && !host.isEmpty())fields.put("server.address",host);
            if(status!=null)fields.put("http.response.status_code",status);
            String code=error instanceof java.util.concurrent.CancellationException ? "HTTP_CANCELLED"
                : error instanceof HttpTimeoutException ? "HTTP_TIMEOUT" : error!=null ? "HTTP_IO_ERROR" : status!=null && status>=400 ? "HTTP_"+status : null;
            fields.put("event.outcome",code==null && status!=null ? "success" : "failure");
            if(code!=null)fields.put("error.code",code);
            invoke("transportComplete",new Object[]{fields},new String[]{"java.util.Map"});
        }
    }
}
