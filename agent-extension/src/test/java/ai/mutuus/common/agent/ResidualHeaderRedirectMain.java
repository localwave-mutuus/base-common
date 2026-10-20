package ai.mutuus.common.agent;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.*;
/** Synthetic already-present headers, not evidence that this fallback injected them. */
public final class ResidualHeaderRedirectMain {
    public static void main(String[] args) throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var result=new LinkedBlockingQueue<Integer>();int port=server.getAddress().getPort();
        server.createContext("/start",ex->{ex.getResponseHeaders().set("Location","http://127.0.0.1:"+port+"/end");ex.sendResponseHeaders(302,-1);ex.close();});
        server.createContext("/end",ex->{var h=ex.getRequestHeaders();int count=0;for(String name:new String[]{"traceparent","tracestate","baggage"})if(h.containsKey(name))count++;result.add(count);ex.sendResponseHeaders(204,-1);ex.close();});
        server.start();
        try(var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/start"))
                .header("traceparent","00-1234567890abcdef1234567890abcdef-abcdef1234567890-01")
                .header("tracestate","fixture=value").header("baggage","fixture=value").build();
            client.send(request,HttpResponse.BodyHandlers.discarding());
            Integer count=result.poll(5,TimeUnit.SECONDS);System.out.println("RESIDUAL EVIDENCE deniedHeaderKinds="+count);
            if(count==null || count!=0)throw new AssertionError("Previously present propagation headers survive unapproved redirect");
            System.out.println("RESIDUAL CONTRACT PASS");
        } finally {server.stop(0);}
    }
}
