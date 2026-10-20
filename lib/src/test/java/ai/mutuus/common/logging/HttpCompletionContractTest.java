package ai.mutuus.common.logging;
import ai.mutuus.common.web.HttpCompletionScope;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.servlet.HandlerMapping;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
class HttpCompletionContractTest {
    static final class Capture extends AccessLogger {
        final List<Map<String,Object>> events = new ArrayList<>();
        @Override public void serverComplete(String method,String path,String route,int status,long nanos,String error,boolean slow,Map<String,Object> snapshot) {
            var event = new HashMap<String,Object>(snapshot); event.put("status",status); event.put("path",path); event.put("nanos",nanos);
            if (error != null) event.put("error",error); events.add(event);
        }
    }
    @Test void responseStatusesValidationAndControllerRoutesCompleteOnce() throws Exception {
        for (int status : new int[]{200,302,400,401,403,404,405,409,413,422,500,503}) {
            var logger = new Capture(); var request = new MockHttpServletRequest("GET", "/private?secret=hidden");
            var response = new MockHttpServletResponse();
            new AccessLogFilter(logger,new CommonLoggingProperties()).doFilter(request,response,(req,res)-> {
                ((jakarta.servlet.http.HttpServletResponse)res).setStatus(status);
                if (status != 404) req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"/items/{id}");
            });
            assertThat(logger.events).hasSize(1); var event=logger.events.getFirst();
            assertThat(event).containsEntry("status",status).containsEntry("path",status == 404 ? "/{unmatched}" : "/items/{id}");
            if (status>=400) assertThat(event).containsEntry("error","HTTP_"+status);
            assertThat(event).doesNotContainEntry("golmok.auth.state","failed");
            assertThat(UUID.fromString((String)event.get("golmok.request.id"))).isNotNull();
            assertThat((long)event.get("nanos")).isGreaterThanOrEqualTo(0);
            assertThat(event.toString()).doesNotContain("hidden");
        }
    }
    @Test void ioIsNotAutomaticallyDisconnectAndUnhandledExceptionIs500() throws Exception {
        for (boolean io : new boolean[]{true,false}) {
            var logger=new Capture(); var request=new MockHttpServletRequest("GET","/x");
            assertThatThrownBy(()->new AccessLogFilter(logger,new CommonLoggingProperties()).doFilter(request,new MockHttpServletResponse(),(req,res)-> {
                if(io) throw new java.io.IOException("SECRET"); throw new IllegalStateException("SECRET");
            })).isInstanceOf(Exception.class);
            assertThat(logger.events).hasSize(1); assertThat(logger.events.getFirst()).containsEntry("status",500)
                .containsEntry("error",io?"HTTP_IO_ERROR":"HTTP_500");
            assertThat(logger.events.toString()).doesNotContain("SECRET");
        }
    }
    @Test void asyncTimeoutErrorAndCompleteShareOneScope() throws Exception {
        for(boolean timeout:new boolean[]{true,false}) {
            var logger=new Capture();var request=new MockHttpServletRequest("GET","/async");request.setAsyncSupported(true);
            var response=new MockHttpServletResponse();
            new AccessLogFilter(logger,new CommonLoggingProperties()).doFilter(request,response,(req,res)->req.startAsync());
            var async=(MockAsyncContext)request.getAsyncContext();
            var event=new jakarta.servlet.AsyncEvent(async,request,response,new IllegalStateException("SECRET"));
            for(var listener:List.copyOf(async.getListeners())) { if(timeout)listener.onTimeout(event);else listener.onError(event); listener.onComplete(event); }
            assertThat(logger.events).hasSize(1);assertThat(logger.events.getFirst()).containsEntry("status",timeout?504:500);
        }
    }
    @Test void concurrentCancellationCompletesOnceWithNoStatus() throws Exception {
        var scope=new HttpCompletionScope();var count=new AtomicInteger();var tasks=new ArrayList<CompletableFuture<Void>>();
        for(int i=0;i<20;i++)tasks.add(CompletableFuture.runAsync(()->scope.finish(null,"HTTP_CANCELLED",done->{
            assertThat(done.status()).isNull();assertThat(done.outcome()).isEqualTo("failure");count.incrementAndGet();
        })));
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).get();assertThat(count.get()).isEqualTo(1);
    }
    @Test void restartedAsyncRegistersAgainAndCommittedIoKeepsStatus() throws Exception {
        var logger=new Capture();var request=new MockHttpServletRequest("GET","/async");request.setAsyncSupported(true);
        var response=new MockHttpServletResponse();
        new AccessLogFilter(logger,new CommonLoggingProperties()).doFilter(request,response,(req,res)->req.startAsync());
        var original=(MockAsyncContext)request.getAsyncContext();var restarted=new MockAsyncContext(request,response);
        for(var listener:List.copyOf(original.getListeners()))listener.onStartAsync(new jakarta.servlet.AsyncEvent(restarted,request,response));
        assertThat(restarted.getListeners()).hasSize(1);response.setStatus(409);
        for(var listener:restarted.getListeners())listener.onComplete(new jakarta.servlet.AsyncEvent(restarted,request,response));
        for(var listener:original.getListeners())listener.onComplete(new jakarta.servlet.AsyncEvent(original,request,response));
        assertThat(logger.events).hasSize(1);assertThat(logger.events.getFirst()).containsEntry("status",409).containsEntry("error","HTTP_409");
        var committed=new Capture();var committedResponse=new MockHttpServletResponse();committedResponse.setStatus(413);committedResponse.flushBuffer();
        assertThatThrownBy(()->new AccessLogFilter(committed,new CommonLoggingProperties()).doFilter(new MockHttpServletRequest("GET","/x"),committedResponse,
            (req,res)->{throw new java.io.IOException("PRIVATE");})).isInstanceOf(java.io.IOException.class);
        assertThat(committed.events).hasSize(1);assertThat(committed.events.getFirst()).containsEntry("status",413).containsEntry("error","HTTP_IO_ERROR");
    }
    @Test void completionMetadataCannotReplaceActualLineageOrRequestId() {
        var active=io.opentelemetry.api.trace.Span.wrap(io.opentelemetry.api.trace.SpanContext.create(
            "1234567890abcdef1234567890abcdef","1234567890abcdef",io.opentelemetry.api.trace.TraceFlags.getSampled(),io.opentelemetry.api.trace.TraceState.getDefault()));
        HttpCompletionScope scope;
        try(var ignored=active.makeCurrent()) {
            var captured=ai.mutuus.common.observability.TelemetryContextSnapshot.capture((trace,span)->Optional.of("abcdef1234567890"));
            try(var restored=captured.makeCurrent()) {scope=new HttpCompletionScope();}
        }
        scope.finish(200,null,Map.of("parent.id","bbbbbbbbbbbbbbbb","span.id","cccccccccccccccc","golmok.request.id","forged"),done->{
            assertThat(done.snapshot()).containsEntry("parent.id","abcdef1234567890").containsEntry("span.id","1234567890abcdef")
                .containsEntry("golmok.request.id",scope.requestId());
        });
    }
}
