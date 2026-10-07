package ai.mutuus.common.logging;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.IdGenerator;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
/** 보안/추적보다 먼저 수신, 비동기는 종료 콜백에서 정확히 한 번 기록한다. */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {
    public static final String TRACE_ATTRIBUTE = HeaderNames.HTTP_PAIR_TRACE_ATTR;
    public static final String USER_ATTRIBUTE = HeaderNames.HTTP_PAIR_USER_ATTR;
    private static final String STATE_ATTRIBUTE = AccessLogFilter.class.getName() + ".state";
    private final AccessLogger accessLogger;
    private final CommonLoggingProperties props;
    public AccessLogFilter(AccessLogger accessLogger, CommonLoggingProperties props) {
        this.accessLogger = accessLogger; this.props = props;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.equals("/actuator") || path.startsWith("/actuator/") || request.getAttribute(STATE_ATTRIBUTE) != null) {
            chain.doFilter(request, response); return;
        }
        String trace = request.getHeader(HeaderNames.TRACE_ID);
        if (trace == null || trace.isBlank()) trace = IdGenerator.newTraceId();
        request.setAttribute(TRACE_ATTRIBUTE, trace);
        State state = new State(request, response, UUID.randomUUID().toString(), trace);
        request.setAttribute(STATE_ATTRIBUTE, state);
        request.setAttribute(RequestIds.HTTP_ATTRIBUTE, state.id);
        response.setHeader(HeaderNames.TRACE_ID, trace);
        accessLogger.httpPair("http.request.in", state.id, trace, request.getMethod(), path,
                0, 0, "", null, false, props.isIncludeQueryString() ? request.getQueryString() : null, state.screen);
        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override public AsyncContext startAsync() { return attach(super.startAsync(this, response)); }
            @Override public AsyncContext startAsync(ServletRequest req, ServletResponse res) { return attach(super.startAsync(req, res)); }
            private AsyncContext attach(AsyncContext context) {
                state.async = true; context.addListener(state); return context;
            }
        };
        String previousRequestId = RequestIds.current();
        TraceContext.put(RequestIds.CONTEXT_KEY, state.id);
        try { chain.doFilter(wrapped, response); }
        catch (IOException | ServletException | RuntimeException | Error ex) { state.failure = ex; throw ex; }
        finally {
            if (!state.async) state.finish(null, 0);
            TraceContext.remove(RequestIds.CONTEXT_KEY);
            TraceContext.put(RequestIds.CONTEXT_KEY, previousRequestId);
        }
    }
    private final class State implements AsyncListener {
        final HttpServletRequest request;
        final HttpServletResponse response;
        final String id, trace;
        final java.util.Map<String, String> screen;
        final long started = System.nanoTime();
        final AtomicBoolean finished = new AtomicBoolean();
        volatile boolean async;
        volatile Throwable failure;
        State(HttpServletRequest request, HttpServletResponse response, String id, String trace) {
            this.request = request; this.response = response; this.id = id; this.trace = trace;
            this.screen = ai.mutuus.common.core.ScreenMetadata.fromHeaders(request::getHeader);
        }
        void finish(String error, int forcedStatus) {
            if (!finished.compareAndSet(false, true)) return;
            int status = response.getStatus();
            if (error == null && failure != null) {
                Throwable cause = failure;
                while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
                boolean disconnect = cause instanceof IOException;
                error = disconnect ? "CLIENT_DISCONNECT" : "UNHANDLED_EXCEPTION";
                forcedStatus = disconnect ? 499 : 500;
            }
            if (forcedStatus != 0) status = forcedStatus;
            if (error == null) error = status >= 400 ? "HTTP_" + status : "";
            long nanos = System.nanoTime() - started;
            boolean slow = props.getSlowRequestThresholdMillis() > 0 && nanos / 1_000_000 > props.getSlowRequestThresholdMillis();
            accessLogger.httpPair("http.response.out", id, trace, request.getMethod(), request.getRequestURI(),
                    status, nanos, error, (String) request.getAttribute(USER_ATTRIBUTE), slow, null, screen);
        }
        @Override public void onComplete(AsyncEvent event) { finish(null, 0); }
        @Override public void onTimeout(AsyncEvent event) { finish("ASYNC_TIMEOUT", 504); }
        @Override public void onError(AsyncEvent event) { failure = event.getThrowable(); finish(null, 0); }
        @Override public void onStartAsync(AsyncEvent event) { event.getAsyncContext().addListener(this); }
    }
}
