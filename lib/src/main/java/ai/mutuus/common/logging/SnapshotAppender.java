package ai.mutuus.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import org.slf4j.event.KeyValuePair;
import java.util.Iterator;

/** root의 유일한 fanout. 이벤트 발생 스레드에서 문맥을 고정한 뒤 콘솔/비동기 파일로 전달한다. */
public final class SnapshotAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
        implements AppenderAttachable<ILoggingEvent> {
    private final AppenderAttachableImpl<ILoggingEvent> delegates = new AppenderAttachableImpl<>();

    @Override public void start() {
        if (isStarted()) return;
        if (!CommonLogResource.current().containsKey("service.instance.id"))
            addWarn("P0 Resource service.instance.id is missing; deployment owner must supply the shared JVM instance ID");
        super.start();
    }

    @Override protected void append(ILoggingEvent event) {
        synchronized (event) {
            if (!(event instanceof LoggingEvent mutable)) {
                addError("SnapshotAppender requires Logback LoggingEvent");
                return;
            }
            boolean captured = event.getKeyValuePairs() != null && event.getKeyValuePairs().stream()
                    .anyMatch(pair -> CanonicalLogSnapshot.SNAPSHOT_KEY.equals(pair.key));
            if (!captured) {
                var snapshot = CanonicalLogSnapshot.capture(event);
                mutable.addKeyValuePair(new KeyValuePair("event.id", snapshot.get("event.id")));
                mutable.addKeyValuePair(new KeyValuePair(CanonicalLogSnapshot.SNAPSHOT_KEY, snapshot));
                try { OtelLogEmitter.emit(snapshot); }
                catch (RuntimeException ex) { addError("OTel log emission failed; file emission continues", ex); }
            }
            event.prepareForDeferredProcessing();
        }
        delegates.appendLoopOnAppenders(event);
    }
    @Override public void addAppender(Appender<ILoggingEvent> appender) { delegates.addAppender(appender); }
    @Override public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() { return delegates.iteratorForAppenders(); }
    @Override public Appender<ILoggingEvent> getAppender(String name) { return delegates.getAppender(name); }
    @Override public boolean isAttached(Appender<ILoggingEvent> appender) { return delegates.isAttached(appender); }
    @Override public void detachAndStopAllAppenders() { delegates.detachAndStopAllAppenders(); }
    @Override public boolean detachAppender(Appender<ILoggingEvent> appender) { return delegates.detachAppender(appender); }
    @Override public boolean detachAppender(String name) { return delegates.detachAppender(name); }
    @Override public void stop() { super.stop(); delegates.detachAndStopAllAppenders(); }
}
