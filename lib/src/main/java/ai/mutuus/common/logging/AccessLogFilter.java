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
        final ai.mutuus.common.web.HttpCompletionScope completion = new ai.mutuus.common.web.HttpCompletionScope();
        final String id = completion.requestId();
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
            
            captureIdentity();
            int status = response.getStatus();
            if (error == null && failure != null) {
                Throwable cause = failure;
                java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                while (cause.getCause() != null && seen.add(cause.getCause())) cause = cause.getCause();
                boolean disconnect = ai.mutuus.common.web.HttpCompletionScope.confirmedDisconnect(failure);
                if (disconnect) { error = "CLIENT_DISCONNECTED"; forcedStatus = 499; }
                else if (cause instanceof IOException) {
                    forcedStatus = response.isCommitted() || status >= 400 ? status : 500;
                    error = "HTTP_IO_ERROR";
                } else { error = "HTTP_500"; forcedStatus = 500; }
            }
            if (forcedStatus != 0) status = forcedStatus;
            if (error == null && failure == null) error = HandledExceptionMetadata.errorCode(request);
            if (error == null) error = status >= 400 ? "HTTP_" + status : "";
            String exceptionType = failure != null ? failure.getClass().getName() : HandledExceptionMetadata.errorType(request);
            if (exceptionType != null) {
                Map<String, Object> completion = new LinkedHashMap<>(snapshot);
                completion.put("error.type", exceptionType);
                snapshot = Map.copyOf(completion);
            }
            Object template = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = template == null ? "/{unmatched}" : template.toString();
            completion.finish(status, error, snapshot, done -> {
                boolean slow = props.getSlowRequestThresholdMillis() > 0
                    && done.durationNanos() / 1_000_000 > props.getSlowRequestThresholdMillis();
                accessLogger.serverComplete(request.getMethod(), route, route, done.status(),
                    done.durationNanos(), done.errorCode(), slow, done.snapshot());
            });
        }
        @Override public void onComplete(AsyncEvent event) { finish(null, 0); }
        @Override public void onTimeout(AsyncEvent event) { finish("HTTP_504", 504); }
        @Override public void onError(AsyncEvent event) { failure = event.getThrowable(); finish("HTTP_500", 500); }
        @Override public void onStartAsync(AsyncEvent event) { event.getAsyncContext().addListener(this); }
    }
}
