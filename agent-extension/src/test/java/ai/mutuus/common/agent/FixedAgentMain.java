package ai.mutuus.common.agent;

import ai.mutuus.common.web.*;
import ai.mutuus.common.logging.CanonicalLogSnapshot;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

public final class FixedAgentMain {
    static void require(boolean condition, String reason) { if (!condition) throw new AssertionError(reason); }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        var wire = new LinkedBlockingQueue<Map<String,String>>();
        server.createContext("/", exchange -> {
            var headers = exchange.getRequestHeaders();
            wire.add(Map.of("traceparent",Objects.toString(headers.getFirst("traceparent"),""),
                "baggage",Objects.toString(headers.getFirst("baggage"),""),"tracestate",Objects.toString(headers.getFirst("tracestate"),""),
                "count",Integer.toString(headers.getOrDefault("traceparent",List.of()).size())));
            exchange.sendResponseHeaders(204,-1); exchange.close();
        }); server.start();
        var logger=(Logger)LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
        var events = new LinkedBlockingQueue<ILoggingEvent>();
        var appender = new ListAppender<ILoggingEvent>() { @Override protected void append(ILoggingEvent event) { events.add(event); } };
        appender.start();logger.addAppender(appender);
        var root=GlobalOpenTelemetry.getTracer("harness").spanBuilder("caller").startSpan();
        try (var client=new ObservedJdkHttpClient(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build());
             var scope=root.makeCurrent(); var baggage=Baggage.builder().put("fixture","must-not-propagate").build().makeCurrent()) {
            int port=server.getAddress().getPort();
            for (String host : List.of("localhost","127.0.0.1")) {
                var request=HttpRequest.newBuilder(URI.create("http://"+host+":"+port+"/local")).build();
                require(client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode()==204,"sync response");
                check(wire,events,host.equals("localhost"),root.getSpanContext().getSpanId());
                require(client.sendAsync(request,HttpResponse.BodyHandlers.discarding()).get(10,TimeUnit.SECONDS).statusCode()==204,"async response");
                check(wire,events,host.equals("localhost"),root.getSpanContext().getSpanId());
            }
            var rest = new RestTemplate(new SimpleClientHttpRequestFactory());
            rest.setInterceptors(List.of(new HttpClientLoggingInterceptor(ai.mutuus.common.core.LogFormat.DUAL,0)));
            for (String host : List.of("localhost","127.0.0.1")) {
                require(rest.getForEntity("http://"+host+":"+port+"/local",Void.class).getStatusCode().value()==204,"Spring response");
                check(wire,events,host.equals("localhost"),root.getSpanContext().getSpanId());
            }
            require(events.isEmpty() && wire.isEmpty(),"no duplicate completion/wire");
        } finally { root.end(); server.stop(0); logger.detachAppender(appender); appender.stop(); }
        var mbs=ManagementFactory.getPlatformMBeanServer();
        require(((Integer)mbs.getAttribute(new ObjectName("ai.mutuus.common:type=SpanLineage"),"Active"))==0,"live cleanup");
        var exported=(List<Map<String,String>>)mbs.getAttribute(new ObjectName("ai.mutuus.test:type=Exported"),"Spans");
        require(exported.stream().filter(s->s.get("kind").equals("CLIENT")).count()==6,"exactly six CLIENT spans");
        System.out.println("HARNESS PASS: six JDK sync/async and Spring attempts; fail-closed fallback (approved host also blocked), CLIENT exporter/log/wire lineage, loader bridge, cleanup");
    }
    @SuppressWarnings("unchecked") static void check(LinkedBlockingQueue<Map<String,String>> wire, LinkedBlockingQueue<ILoggingEvent> events,
            boolean allowed,String parent) throws Exception {
        var received=wire.poll(10,TimeUnit.SECONDS); var event=events.poll(10,TimeUnit.SECONDS);
        require(received!=null && event!=null,"wire/completion present");
        Map<String,Object> snapshot=null;
        for(var pair:event.getKeyValuePairs()) if(pair.key.equals(CanonicalLogSnapshot.CONTEXT_KEY)) snapshot=(Map<String,Object>)pair.value;
        require(snapshot!=null && parent.equals(snapshot.get("parent.id")),"actual CLIENT parent in completion: "+snapshot);
        var exported=(List<Map<String,String>>)ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("ai.mutuus.test:type=Exported"),"Spans");
        var fields=snapshot;
        require(exported.stream().anyMatch(s->s.get("kind").equals("CLIENT") && s.get("span.id").equals(fields.get("span.id"))
            && s.get("trace.id").equals(fields.get("trace.id")) && s.get("parent.id").equals(fields.get("parent.id"))),"exporter/log actual CLIENT match");
        require(received.get("baggage").isEmpty(),"baggage blocked");
        require(received.get("traceparent").isEmpty() && received.get("tracestate").isEmpty(),"no final destination proof: fail closed, including configured host");
    }
}
