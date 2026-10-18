package ai.mutuus.common.logging;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.VerifiedIdentityContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** 보안/추적 필터 밖에서 수신하되, agent span과 검증된 신원 snapshot으로 정확히 한 번 완료 기록한다. */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {
    public static final String TRACE_ATTRIBUTE = HeaderNames.HTTP_PAIR_TRACE_ATTR;
    public static final String USER_ATTRIBUTE = HeaderNames.HTTP_PAIR_USER_ATTR;
    private static final String STATE_ATTRIBUTE = AccessLogFilter.class.getName() + ".state";
    private final AccessLogger accessLogger;
    private final CommonLoggingProperties props;
    public AccessLogFilter(AccessLogger accessLogger, CommonLoggingProperties props) {
        this(accessLogger, props, true);
    }
    /** w3c는 호환 인자다. 추적 문맥의 유일한 원천은 이미 열린 agent span이다. */
    public AccessLogFilter(AccessLogger accessLogger, CommonLoggingProperties props, boolean w3c) {
        this.accessLogger = accessLogger;
        this.props = props;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.equals("/actuator") || path.startsWith("/actuator/") || request.getAttribute(STATE_ATTRIBUTE) != null) {
            chain.doFilter(request, response); return;
        }
        State state = new State(request, response);
        request.setAttribute(STATE_ATTRIBUTE, state);
        request.setAttribute(RequestIds.HTTP_ATTRIBUTE, state.id);
        if (state.snapshot.get("trace.id") instanceof String trace) {
            request.setAttribute(TRACE_ATTRIBUTE, trace);
            response.setHeader(HeaderNames.TRACE_ID, trace);
        }
        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override public AsyncContext startAsync() { return attach(super.startAsync(this, response)); }
            @Override public AsyncContext startAsync(ServletRequest req, ServletResponse res) { return attach(super.startAsync(req, res)); }
            private AsyncContext attach(AsyncContext context) {
                state.async = true;
                context.addListener(state);
                return context;
            }
        };
        String previousRequestId = RequestIds.current();
        TraceContext.put(RequestIds.CONTEXT_KEY, state.id);
        try { chain.doFilter(wrapped, response); }
        catch (IOException | ServletException | RuntimeException | Error ex) { state.failure = ex; throw ex; }
        finally {
            state.captureIdentity();
            if (!state.async) state.finish(null, 0);
            TraceContext.remove(RequestIds.CONTEXT_KEY);
            TraceContext.put(RequestIds.CONTEXT_KEY, previousRequestId);
        }
    }
    private final class State implements AsyncListener {
        final HttpServletRequest request;
        final HttpServletResponse response;
        final String id = UUID.randomUUID().toString();
        final long started = System.nanoTime();
        final AtomicBoolean finished = new AtomicBoolean();
        volatile Map<String, Object> snapshot;
        volatile boolean async;
        volatile Throwable failure;
        State(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
            Map<String, Object> initial = new LinkedHashMap<>(CanonicalLogSnapshot.context());
            initial.put("golmok.scope", "request");
            initial.put("golmok.request.id", id);
            initial.putAll(CanonicalLogSnapshot.requestMetadata(request::getHeader));
            this.snapshot = Map.copyOf(initial);
        }
        void captureIdentity() {
            if (request.getAttribute(VerifiedIdentityContext.REQUEST_ATTRIBUTE) instanceof VerifiedIdentityContext.Identity identity) {
                Map<String, Object> completed = new LinkedHashMap<>(snapshot);
                completed.remove("user.id");
                completed.remove("golmok.identity.realm");
                completed.remove("golmok.identity.source");
                completed.putAll(identity.fields());
                snapshot = Map.copyOf(completed);
            }
        }
        void finish(String error, int forcedStatus) {
            if (!finished.compareAndSet(false, true)) return;
            captureIdentity();
            int status = response.getStatus();
            if (error == null && failure != null) {
                Throwable cause = failure;
                java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                while (cause.getCause() != null && seen.add(cause.getCause())) cause = cause.getCause();
                boolean disconnect = cause instanceof IOException;
                error = disconnect ? "CLIENT_DISCONNECT" : "UNHANDLED_EXCEPTION";
                forcedStatus = disconnect ? 499 : 500;
            }
            if (forcedStatus != 0) status = forcedStatus;
            if (error == null) error = status >= 400 ? "HTTP_" + status : "";
            if (status == 401 && !snapshot.containsKey("user.id")) {
                Map<String, Object> failed = new LinkedHashMap<>(snapshot);
                failed.put("golmok.auth.state", "failed");
                snapshot = Map.copyOf(failed);
            }
            String exceptionType = failure != null ? failure.getClass().getName() : HandledExceptionMetadata.errorType(request);
            if (exceptionType != null) {
                Map<String, Object> completion = new LinkedHashMap<>(snapshot);
                completion.put("error.type", exceptionType);
                snapshot = Map.copyOf(completion);
            }
            long nanos = System.nanoTime() - started;
            boolean slow = props.getSlowRequestThresholdMillis() > 0 && nanos / 1_000_000 > props.getSlowRequestThresholdMillis();
            Object template = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = template == null ? null : template.toString();
            // 실제 URL 식별자·개인정보는 남기지 않고 매칭된 템플릿 경로만 출력한다.
            accessLogger.serverComplete(request.getMethod(), route == null ? "/{unmatched}" : route,
                    route, status, nanos, error, slow, snapshot);
        }
        @Override public void onComplete(AsyncEvent event) { finish(null, 0); }
        @Override public void onTimeout(AsyncEvent event) { finish("ASYNC_TIMEOUT", 504); }
        @Override public void onError(AsyncEvent event) { failure = event.getThrowable(); finish("ASYNC_ERROR", 500); }
        @Override public void onStartAsync(AsyncEvent event) { event.getAsyncContext().addListener(this); }
    }
}
