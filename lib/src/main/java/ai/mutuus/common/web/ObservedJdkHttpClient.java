package ai.mutuus.common.web;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.logging.CanonicalLogSnapshot;
import org.slf4j.LoggerFactory;

/** Explicit completion decorator. The Java agent owns CLIENT spans and propagation. */
public final class ObservedJdkHttpClient extends HttpClient {
    private final HttpClient delegate;
    private static final boolean TRANSPORT = installTransportListener();
    private static boolean installTransportListener() {
        try {
            var server=java.lang.management.ManagementFactory.getPlatformMBeanServer();
            var name=new javax.management.ObjectName("ai.mutuus.common:type=SpanLineage");
            if(!Boolean.TRUE.equals(server.getAttribute(name,"TransportEnabled")))return false;
            server.addNotificationListener(name,(notification,handback)->{
                if(!notification.getType().equals("ai.mutuus.common.transport.complete"))return;
                var fields=(Map<?,?>)notification.getUserData();
                var snapshot=new HashMap<String,Object>();
                for(String key:List.of("trace.id","span.id","parent.id","trace.flags","golmok.request.id"))
                    if(fields.containsKey(key))snapshot.put(key,fields.get(key));
                var event=LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME).atInfo()
                    .addKeyValue("event.dataset",EcsFields.DATASET_HTTP_CLIENT).addKeyValue("event.action","http.client.completed")
                    .addKeyValue("url.path","/{unmatched}").addKeyValue(CanonicalLogSnapshot.CONTEXT_KEY,Map.copyOf(snapshot));
                for(String key:List.of("event.duration","event.outcome","http.request.method","http.response.status_code","error.code"))
                    if(fields.containsKey(key))event.addKeyValue(key,fields.get(key));
                event.log("http.client.completed");
            },null,null);
            return true;
        } catch(Exception unavailable) { return false; }
    }
    static boolean transportLoggingEnabled() { return TRANSPORT; }
    public ObservedJdkHttpClient(HttpClient delegate) { this.delegate = Objects.requireNonNull(delegate); }
    private void finish(HttpCompletionScope scope, HttpRequest request, HttpResponse<?> response, Throwable error) {
        while (error instanceof CompletionException || error instanceof ExecutionException) error = error.getCause();
        String code = error instanceof CancellationException || error instanceof InterruptedException ? "HTTP_CANCELLED"
            : error instanceof HttpTimeoutException ? "HTTP_TIMEOUT" : error != null ? "HTTP_IO_ERROR" : null;
        scope.finish(response == null ? null : response.statusCode(), code, done -> {
            var ev = LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME).atInfo()
                .addKeyValue("event.dataset", EcsFields.DATASET_HTTP_CLIENT).addKeyValue("event.action", "http.client.completed")
                .addKeyValue("event.outcome", done.outcome()).addKeyValue("event.duration", done.durationNanos())
                .addKeyValue("http.request.method", request.method()).addKeyValue("url.path", "/{unmatched}")
                .addKeyValue(CanonicalLogSnapshot.CONTEXT_KEY, done.snapshot());
            if (done.status() != null) ev.addKeyValue("http.response.status_code", done.status());
            if (done.errorCode() != null) ev.addKeyValue("error.code", done.errorCode());
            ev.log("http.client.completed");
        });
    }
    @Override public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        if (TRANSPORT) return delegate.send(request, handler);
        var scope = new HttpCompletionScope(false);
        try { var response = delegate.send(request, handler); finish(scope, request, response, null); return response; }
        catch (IOException | InterruptedException | RuntimeException | Error e) { finish(scope, request, null, e); throw e; }
        finally { scope.detachClientDispatch(); }
    }
    @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        return sendAsync(request, handler, null);
    }
    @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler,
            HttpResponse.PushPromiseHandler<T> push) {
        if (TRANSPORT) return push == null ? delegate.sendAsync(request, handler) : delegate.sendAsync(request, handler, push);
        var scope = new HttpCompletionScope(false);
        try {
            var future = push == null ? delegate.sendAsync(request, handler) : delegate.sendAsync(request, handler, push);
            // Return the original future so cancellation still reaches the underlying exchange.
            future.whenComplete((response, error) -> finish(scope, request, response, error));
            return future;
        } catch (RuntimeException | Error e) { finish(scope, request, null, e); throw e; }
        finally { scope.detachClientDispatch(); }
    }
    @Override public Optional<CookieHandler> cookieHandler() { return delegate.cookieHandler(); }
    @Override public Optional<Duration> connectTimeout() { return delegate.connectTimeout(); }
    @Override public Redirect followRedirects() { return delegate.followRedirects(); }
    @Override public Optional<ProxySelector> proxy() { return delegate.proxy(); }
    @Override public SSLContext sslContext() { return delegate.sslContext(); }
    @Override public SSLParameters sslParameters() { return delegate.sslParameters(); }
    @Override public Optional<Authenticator> authenticator() { return delegate.authenticator(); }
    @Override public Version version() { return delegate.version(); }
    @Override public Optional<Executor> executor() { return delegate.executor(); }
    @Override public WebSocket.Builder newWebSocketBuilder() { return delegate.newWebSocketBuilder(); }
    @Override public void shutdown() { delegate.shutdown(); }
    @Override public void shutdownNow() { delegate.shutdownNow(); }
    @Override public boolean awaitTermination(Duration duration) throws InterruptedException { return delegate.awaitTermination(duration); }
    @Override public boolean isTerminated() { return delegate.isTerminated(); }
    @Override public void close() { delegate.close(); }
}
