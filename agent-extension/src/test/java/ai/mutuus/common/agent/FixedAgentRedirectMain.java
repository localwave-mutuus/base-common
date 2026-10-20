package ai.mutuus.common.agent;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.*;
import io.opentelemetry.api.GlobalOpenTelemetry;

/** A different input: approved host redirecting to an unapproved local destination. */
public final class FixedAgentRedirectMain {
    public static void main(String[] args) throws Exception {
        var server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        var target=new LinkedBlockingQueue<String>();
        int port=server.getAddress().getPort();
        server.createContext("/redirect",ex->{ex.getResponseHeaders().set("Location","http://127.0.0.1:"+port+"/target");ex.sendResponseHeaders(302,-1);ex.close();});
        server.createContext("/target",ex->{target.add(java.util.Objects.toString(ex.getRequestHeaders().getFirst("traceparent"),""));ex.sendResponseHeaders(204,-1);ex.close();});
        server.start();
        var root=GlobalOpenTelemetry.getTracer("harness").spanBuilder("redirect-caller").startSpan();
        try(var ignored=root.makeCurrent();var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/redirect")).build(),HttpResponse.BodyHandlers.discarding());
            String header=target.poll(5,TimeUnit.SECONDS);
            if(header==null || !header.isEmpty()) throw new AssertionError("Unapproved redirect destination received trace context; original CLIENT URL cannot authorize final destination");
            System.out.println("REDIRECT PASS");
        } finally { root.end();server.stop(0); }
    }
}
