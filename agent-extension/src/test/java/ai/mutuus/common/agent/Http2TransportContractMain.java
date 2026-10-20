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
public final class Http2TransportContractMain {
    record Wire(boolean approved, boolean trace, boolean state, boolean baggage) { }
    static Wire wire(boolean approved, com.sun.net.httpserver.Headers h) {
        return new Wire(approved,h.containsKey("traceparent"),h.containsKey("tracestate"),h.containsKey("baggage"));
    }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("target/harness/h2-wire.json"));
        var server=new ProcessBuilder("python3","src/test/python/h2_wire.py")
            .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        int port=Integer.parseInt(new BufferedReader(new InputStreamReader(server.getInputStream())).readLine());
        var log=(Logger)LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
        var events=new LinkedBlockingQueue<ILoggingEvent>();
        var appender=new ListAppender<ILoggingEvent>() { @Override protected void append(ILoggingEvent e){events.add(e);} };
        appender.start();log.addAppender(appender);
        var trust=new javax.net.ssl.X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers(){return new java.security.cert.X509Certificate[0];}
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain,String auth){}
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain,String auth){}
        };
        var tls=javax.net.ssl.SSLContext.getInstance("TLS");
        tls.init(null,new javax.net.ssl.TrustManager[]{trust},null);
        var root=GlobalOpenTelemetry.getTracer("transport-contract").spanBuilder("caller").startSpan();
        try(var scope=root.makeCurrent();var client=new ObservedJdkHttpClient(HttpClient.newBuilder().sslContext(tls).version(HttpClient.Version.HTTP_2).followRedirects(HttpClient.Redirect.NORMAL).build())) {
            for(String path:List.of("/forward","/reverse")) {
                String host=path.equals("/forward")?"localhost":"127.0.0.1";
                var response=client.send(HttpRequest.newBuilder(URI.create("https://"+host+":"+port+path))
                    .header("traceparent","00-1234567890abcdef1234567890abcdef-abcdef1234567890-01")
                    .header("tracestate","fixture=value").header("baggage","fixture=value").build(),HttpResponse.BodyHandlers.discarding());
                if(response.statusCode()!=204 || response.version()!=HttpClient.Version.HTTP_2)throw new AssertionError("HTTP/2 redirect response");
            }
            if(client.send(HttpRequest.newBuilder(URI.create("https://localhost:"+port+"/retry")).build(),HttpResponse.BodyHandlers.discarding()).statusCode()!=204)throw new AssertionError("retry status");

        } finally {
            root.end();server.destroyForcibly();log.detachAppender(appender);appender.stop();
        }
        var exported=(List<Map<String,String>>)ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("ai.mutuus.test:type=Exported"),"Spans");
        long clients=exported.stream().filter(s->s.get("kind").equals("CLIENT")).count();
        String evidence=java.nio.file.Files.readString(java.nio.file.Path.of("target/harness/h2-wire.json"));
        // Python emits booleans only; no trace/header values are persisted.
        var entries=evidence.substring(1,evidence.length()-1).split("},");
        long approved=0,propagated=0,leaked=0;
        for(String entry:entries) {
            boolean allowed=entry.contains("\"approved\": true");
            boolean trace=entry.contains("\"trace\": true");
            boolean state=entry.contains("\"state\": true"), baggage=entry.contains("\"baggage\": true");
            if(allowed) {approved++;if(trace)propagated++;}
            if((!allowed && (trace||state||baggage)) || baggage)leaked++;
        }
        System.out.printf("HTTP2 EVIDENCE wire=%d approved=%d approvedTrace=%d deniedLeak=%d completions=%d clientSpans=%d%n",entries.length,approved,propagated,leaked,events.size(),clients);
        if(entries.length!=6 || propagated!=approved || leaked!=0 || events.size()!=6 || clients!=6)
            throw new AssertionError("Destination propagation and per-attempt completion contract not satisfied");
        System.out.println("HTTP2 CONTRACT PASS");
    }
}
