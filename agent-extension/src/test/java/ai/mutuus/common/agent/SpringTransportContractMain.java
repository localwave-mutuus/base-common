package ai.mutuus.common.agent;

import ai.mutuus.common.web.*;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

/** New diagnostic inputs; failed contract assertions must remain failures, never fallback success. */
public final class SpringTransportContractMain {
    static final class SpringClient implements AutoCloseable {
        final org.springframework.web.client.RestTemplate rest=new org.springframework.web.client.RestTemplate(new org.springframework.http.client.SimpleClientHttpRequestFactory());
        SpringClient(){rest.setInterceptors(List.of(new HttpClientLoggingInterceptor(ai.mutuus.common.core.LogFormat.DUAL,0)));}
        java.net.http.HttpResponse<Void> send(HttpRequest request,HttpResponse.BodyHandler<Void> ignored) {
            var response=rest.getForEntity(request.uri(),Void.class);
            return new java.net.http.HttpResponse<>() {
                public int statusCode(){return response.getStatusCode().value();}
                public HttpRequest request(){return request;}
                public Optional<java.net.http.HttpResponse<Void>> previousResponse(){return Optional.empty();}
                public java.net.http.HttpHeaders headers(){return java.net.http.HttpHeaders.of(Map.of(),(k,v)->true);}
                public Void body(){return null;}
                public Optional<javax.net.ssl.SSLSession> sslSession(){return Optional.empty();}
                public URI uri(){return request.uri();}
                public HttpClient.Version version(){return HttpClient.Version.HTTP_1_1;}
            };
        }
        public void close(){}
    }
    record Wire(boolean approved, boolean trace, boolean state, boolean baggage) { }
    static Wire wire(boolean approved, com.sun.net.httpserver.Headers h) {
        return new Wire(approved,h.containsKey("traceparent"),h.containsKey("tracestate"),h.containsKey("baggage"));
    }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        var wires=new LinkedBlockingQueue<Wire>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        int port=server.getAddress().getPort();
        server.createContext("/", ex->{
            boolean approved=ex.getRequestHeaders().getFirst("Host").startsWith("localhost:");
            wires.add(wire(approved,ex.getRequestHeaders()));
            String path=ex.getRequestURI().getPath();
            if(path.equals("/forward") || path.equals("/reverse")) {
                String host=path.equals("/forward")?"127.0.0.1":"localhost";
                ex.getResponseHeaders().set("Location","http://"+host+":"+port+"/end");
                ex.sendResponseHeaders(302,-1);
            } else ex.sendResponseHeaders(204,-1);
            ex.close();
        });
        var log=(Logger)LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
        var events=new LinkedBlockingQueue<ILoggingEvent>();
        var appender=new ListAppender<ILoggingEvent>() { @Override protected void append(ILoggingEvent e){events.add(e);} };
        appender.start();log.addAppender(appender);server.start();
        // First transport accepts and reads the GET but drops before response headers. The JDK retries it.
        var retrySocket=new ServerSocket(0,10,InetAddress.getByName("127.0.0.1"));retrySocket.setSoTimeout(10000);
        var worker=Executors.newSingleThreadExecutor();
        var retry=worker.submit(()->{
            for(int i=0;i<2;i++) try(var socket=retrySocket.accept()) {
                socket.setSoTimeout(10000);
                var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
                boolean trace=false,state=false,baggage=false;
                for(String line;(line=reader.readLine())!=null && !line.isEmpty();) {
                    String lower=line.toLowerCase(Locale.ROOT);
                    trace|=lower.startsWith("traceparent:");state|=lower.startsWith("tracestate:");baggage|=lower.startsWith("baggage:");
                }
                wires.add(new Wire(true,trace,state,baggage));
                if(i==1) {socket.getOutputStream().write("HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();}
            }
            return null;
        });
        var root=GlobalOpenTelemetry.getTracer("transport-contract").spanBuilder("caller").startSpan();
        try(var scope=root.makeCurrent();var client=new SpringClient()) {
            for(String path:List.of("/forward","/reverse")) {
                String host=path.equals("/forward")?"localhost":"127.0.0.1";
                if(client.send(HttpRequest.newBuilder(URI.create("http://"+host+":"+port+path)).build(),HttpResponse.BodyHandlers.discarding()).statusCode()!=204)throw new AssertionError("redirect status");
            }
            if(client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+retrySocket.getLocalPort()+"/retry")).build(),HttpResponse.BodyHandlers.discarding()).statusCode()!=204)throw new AssertionError("retry status");
            retry.get(12,TimeUnit.SECONDS);
        } finally {
            root.end();server.stop(0);retrySocket.close();worker.shutdownNow();log.detachAppender(appender);appender.stop();
        }
        var exported=(List<Map<String,String>>)ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("ai.mutuus.test:type=Exported"),"Spans");
        long clients=exported.stream().filter(s->s.get("kind").equals("CLIENT")).count();
        long approved=wires.stream().filter(Wire::approved).count();
        long propagated=wires.stream().filter(w->w.approved && w.trace).count();
        long leaked=wires.stream().filter(w->!w.approved && (w.trace||w.state||w.baggage)).count();
        System.out.printf("SPRING EVIDENCE wire=%d approved=%d approvedTrace=%d deniedLeak=%d completions=%d clientSpans=%d%n",wires.size(),approved,propagated,leaked,events.size(),clients);
        if(wires.size()!=6 || propagated!=approved || leaked!=0 || events.size()!=6 || clients!=6)
            throw new AssertionError("Destination propagation and per-attempt completion contract not satisfied");
        System.out.println("SPRING CONTRACT PASS");
    }
}
