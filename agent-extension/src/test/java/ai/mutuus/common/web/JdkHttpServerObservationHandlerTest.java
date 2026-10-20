package ai.mutuus.common.web;
import ai.mutuus.common.logging.AccessLogger;
import ai.mutuus.common.observability.SpanLineageSpanProcessor;
import com.sun.net.httpserver.*;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class JdkHttpServerObservationHandlerTest {
    HttpExchange exchange(String traceparent) throws Exception {
        var exchange=mock(HttpExchange.class);var status=new AtomicInteger(-1);var headers=new Headers();
        if(traceparent!=null) headers.add("traceparent",traceparent);
        when(exchange.getRequestHeaders()).thenReturn(headers);when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getResponseCode()).thenAnswer(call->status.get());
        doAnswer(call->{status.set(call.getArgument(0));return null;}).when(exchange).sendResponseHeaders(anyInt(),anyLong());
        return exchange;
    }
    @Test void manualW3cRootInvalidParentRejectionAndErrorsCompleteOnce() throws Exception {
        var processor=new SpanLineageSpanProcessor(10);processor.register();
        try(var provider=SdkTracerProvider.builder().addSpanProcessor(processor).build()) {
            var telemetry=OpenTelemetrySdk.builder().setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();
            var events=new ArrayList<Map<String,Object>>();
            var logger=new AccessLogger() {
                @Override public void serverComplete(String method,String path,String route,int status,long nanos,String error,boolean slow,Map<String,Object> fields) {
                    var value=new HashMap<String,Object>(fields);value.put("status",status);if(error!=null)value.put("error",error);events.add(value);
                }
            };
            var handler=new JdkHttpServerObservationHandler(ex->ex.sendResponseHeaders(403,-1),JdkHttpServerObservationHandler.SpanOwner.MANUAL,telemetry,logger,"/admin");
            handler.handle(exchange("00-1234567890abcdef1234567890abcdef-abcdef1234567890-01"));
            assertThat(events).hasSize(1);assertThat(events.getFirst()).containsEntry("parent.id","abcdef1234567890").containsEntry("error","HTTP_403");
            handler.handle(exchange("invalid"));assertThat(events).hasSize(2);assertThat(events.getLast()).doesNotContainKey("parent.id");
            var admission=new JdkHttpServerAdmissionAdapter(handler,task->{throw new RejectedExecutionException();});
            admission.handle(exchange(null));assertThat(events).hasSize(3);assertThat(events.getLast()).containsEntry("status",503).containsEntry("error","HTTP_503");
            var broken=new JdkHttpServerObservationHandler(ex->{throw new IllegalStateException("SECRET");},JdkHttpServerObservationHandler.SpanOwner.MANUAL,telemetry,logger,null);
            assertThatThrownBy(()->broken.handle(exchange(null))).isInstanceOf(IllegalStateException.class);
            assertThat(events).hasSize(4);assertThat(events.getLast()).containsEntry("status",500).containsEntry("error","HTTP_500");assertThat(events.toString()).doesNotContain("SECRET");
            var span=provider.get("fixture").spanBuilder("already-agent-owned").startSpan();
            try(var ignored=span.makeCurrent()) { assertThatThrownBy(()->handler.handle(exchange(null))).isInstanceOf(IllegalStateException.class); }
            finally {span.end();}
        }
    }
    @Test void realManualServerAdmissionAndAgentBorrowingHaveOneCompletionAndNoExtraSpan() throws Exception {
        var processor=new SpanLineageSpanProcessor(10);processor.register();
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(),0),0);
        try(var provider=SdkTracerProvider.builder().addSpanProcessor(processor).build()) {
            var telemetry=OpenTelemetrySdk.builder().setTracerProvider(provider).setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();
            var events=new java.util.concurrent.LinkedBlockingQueue<Map<String,Object>>();
            var logger=new AccessLogger() {
                @Override public void serverComplete(String method,String path,String route,int status,long nanos,String error,boolean slow,Map<String,Object> fields) {
                    var result=new HashMap<String,Object>(fields);result.put("status",status);events.add(result);
                }
            };
            var handler=new JdkHttpServerObservationHandler(ex->{ex.sendResponseHeaders(204,-1);ex.close();},JdkHttpServerObservationHandler.SpanOwner.MANUAL,telemetry,logger,"/health");
            server.createContext("/",new JdkHttpServerAdmissionAdapter(handler,Runnable::run));server.start();
            try(var client=java.net.http.HttpClient.newHttpClient()) {
                var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:"+server.getAddress().getPort()+"/health"))
                    .header("traceparent","00-1234567890abcdef1234567890abcdef-abcdef1234567890-01").build();
                assertThat(client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(204);
                var event=events.poll(5,TimeUnit.SECONDS);assertThat(event).isNotNull().containsEntry("parent.id","abcdef1234567890").containsEntry("status",204);
            }
            var borrowed=provider.get("fixture").spanBuilder("agent-server").setSpanKind(io.opentelemetry.api.trace.SpanKind.SERVER).startSpan();
            try(var ignored=borrowed.makeCurrent()) {
                new JdkHttpServerObservationHandler(ex->ex.sendResponseHeaders(200,-1),JdkHttpServerObservationHandler.SpanOwner.AGENT,telemetry,logger,null).handle(exchange(null));
                var event=events.poll(5,TimeUnit.SECONDS);assertThat(event).containsEntry("span.id",borrowed.getSpanContext().getSpanId());
                assertThat(((io.opentelemetry.sdk.trace.ReadableSpan)borrowed).hasEnded()).isFalse();
            } finally {borrowed.end();}
            assertThat(events).isEmpty();
        } finally {server.stop(0);}
    }
}
