package ai.mutuus.common.web;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.util.concurrent.*;
/** Exchange-visible admission only. Rejection before HttpHandler entry belongs to container metrics. */
public final class JdkHttpServerAdmissionAdapter implements HttpHandler {
    private final JdkHttpServerObservationHandler handler;
    private final Executor executor;
    public JdkHttpServerAdmissionAdapter(JdkHttpServerObservationHandler handler, Executor executor) {
        this.handler = java.util.Objects.requireNonNull(handler); this.executor = java.util.Objects.requireNonNull(executor);
    }
    @Override public void handle(HttpExchange exchange) throws IOException {
        var observation = handler.open(exchange);
        try { executor.execute(() -> {
            try (observation) { observation.run(); }
            catch (IOException | RuntimeException ignored) { exchange.close(); }
        }); } catch (RejectedExecutionException rejected) { observation.reject(); }
    }
}
