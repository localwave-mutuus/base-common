package ai.mutuus.common.logging;

import ch.qos.logback.core.rolling.RollingFileAppender;

/** AsyncAppender가 큐를 전부 비운 뒤 마지막 active 파일도 닫고 압축한다. */
public class ClosingRollingFileAppender<E> extends RollingFileAppender<E> {
    @Override public void stop() {
        if (isStarted()) rollover();
        super.stop();
    }
}
