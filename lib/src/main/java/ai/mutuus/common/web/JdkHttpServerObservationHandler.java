package ai.mutuus.common.web;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.util.*;
import ai.mutuus.common.logging.AccessLogger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.*;
import io.opentelemetry.context.propagation.TextMapGetter;

/** Wrap the entire authentication/router handler. Manual mode is an explicit agent exception. */
public final class JdkHttpServerObservationHandler implements HttpHandler {
    public enum SpanOwner { AGENT, MANUAL }
    private final HttpHandler delegate;
    private final SpanOwner owner;
    private final OpenTelemetry telemetry;
    private final AccessLogger logger;
    private final String route;
    public JdkHttpServerObservationHandler(HttpHandler delegate, SpanOwner owner, OpenTelemetry telemetry, AccessLogger logger, String route) {
        this.delegate = Objects.requireNonNull(delegate); this.owner = Objects.requireNonNull(owner);
        this.telemetry = Objects.requireNonNull(telemetry); this.logger = Objects.requireNonNull(logger);
        if (route != null && (!route.startsWith("/") || route.contains("?"))) throw new IllegalArgumentException("Route template required");
        this.route = route == null ? "/{unmatched}" : route;
    }
    private static final TextMapGetter<HttpExchange> GETTER = new TextMapGetter<>() {
        public Iterable<String> keys(HttpExchange exchange) { return exchange.getRequestHeaders().keySet(); }
        public String get(HttpExchange exchange, String key) { return exchange.getRequestHeaders().getFirst(key); }
    };
    @Override public void handle(HttpExchange exchange) throws IOException {
        try (var observation = open(exchange)) { observation.run(); }
    }
    Observation open(HttpExchange exchange) {
        if (owner == SpanOwner.MANUAL && Span.current().getSpanContext().isValid())
            throw new IllegalStateException("Manual server cannot borrow an active agent span");
        Span span = owner == SpanOwner.MANUAL ? telemetry.getTracer("ai.mutuus.common.http-server")
            .spanBuilder("HTTP server").setSpanKind(SpanKind.SERVER)
            .setParent(telemetry.getPropagators().getTextMapPropagator().extract(Context.root(), exchange, GETTER)).startSpan() : null;
        Context context = span == null ? Context.current() : Context.current().with(span);
        try (var ignored = context.makeCurrent()) { return new Observation(exchange, span, context, new HttpCompletionScope()); }
    }
    final class Observation implements AutoCloseable {
        final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        final HttpExchange exchange; final Span span; final Context context; final HttpCompletionScope completion;
        Observation(HttpExchange exchange, Span span, Context context, HttpCompletionScope completion) {
            this.exchange = exchange; this.span = span; this.context = context; this.completion = completion;
        }
        void run() throws IOException {
            try (var ignored = context.makeCurrent()) { delegate.handle(exchange); }
            catch (IOException e) {
                finish(exchange.getResponseCode() >= 100 ? exchange.getResponseCode() : 500, "HTTP_IO_ERROR"); throw e;
            } catch (RuntimeException | Error e) { finish(500, "HTTP_500"); throw e; }
        }
        void reject() throws IOException {
            try { exchange.sendResponseHeaders(503, -1); }
            finally { finish(503, "HTTP_503"); close(); exchange.close(); }
        }
        void finish(int status, String code) {
            completion.finish(status > 0 ? status : null, code, done ->
                logger.serverComplete(exchange.getRequestMethod(), route, route,
                    done.status() == null ? 500 : done.status(), done.durationNanos(), done.errorCode(), false, done.snapshot()));
        }
        @Override public void close() { if (!closed.compareAndSet(false, true)) return; try { finish(exchange.getResponseCode(), exchange.getResponseCode() < 100 ? "HTTP_RESPONSE_MISSING" : null); }
            finally { if (span != null) span.end(); } }
    }
}
