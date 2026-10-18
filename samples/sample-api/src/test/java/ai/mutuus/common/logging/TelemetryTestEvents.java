package ai.mutuus.common.logging;

import java.util.Map;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import ai.mutuus.common.core.VerifiedIdentityContext;

/** 시험에서도 생산 경로와 같은 생성 시점 snapshot을 사용한다. Global SDK를 바꾸지 않는다. */
public final class TelemetryTestEvents {
    private TelemetryTestEvents() { }
    public static Map<String, Object> fields(ILoggingEvent event) { return CanonicalLogSnapshot.snapshot(event); }
    public static Capture attach(Logger logger, Appender<ILoggingEvent> downstream) {
        SnapshotAppender snapshot = new SnapshotAppender(); snapshot.setContext(logger.getLoggerContext());
        if (!downstream.isStarted()) downstream.start();
        snapshot.addAppender(downstream); snapshot.start(); logger.addAppender(snapshot);
        return new Capture(logger, snapshot);
    }
    public record Capture(Logger logger, SnapshotAppender appender) implements AutoCloseable {
        @Override public void close() { logger.detachAppender(appender); appender.stop(); }
    }
    public static SpanScope span(String name) { return span(name, Context.root()); }
    public static SpanScope span(String name, Context parent) {
        SdkTracerProvider provider = SdkTracerProvider.builder().build();
        Span span = provider.get("consumer-fixture").spanBuilder(name).setParent(parent).startSpan();
        return new SpanScope(provider, span, span.makeCurrent());
    }
    public record SpanScope(SdkTracerProvider provider, Span span, Scope scope) implements AutoCloseable {
        public String traceId() { return span.getSpanContext().getTraceId(); }
        public String spanId() { return span.getSpanContext().getSpanId(); }
        @Override public void close() { try { scope.close(); } finally { span.end(); provider.close(); } }
    }
    public static Scope identity(String id, String realm) {
        return VerifiedIdentityContext.makeCurrent(new VerifiedIdentityContext.Identity(id, realm, "authenticated"));
    }
}