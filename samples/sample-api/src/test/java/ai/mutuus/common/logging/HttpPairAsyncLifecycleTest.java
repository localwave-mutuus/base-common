package ai.mutuus.common.logging;

import java.io.IOException;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.AsyncEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.assertThat;

class HttpPairAsyncLifecycleTest {
    @Test void 비동기_error_complete와_반복_dispatch는_중복_짝을_만들지_않는다() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); logger.addAppender(capture);
        try {
            var filter = new AccessLogFilter(new AccessLogger(), new CommonLoggingProperties());
            var req = new MockHttpServletRequest("GET", "/async-error"); req.setAsyncSupported(true);
            var res = new MockHttpServletResponse();
            filter.doFilter(req, res, (request, response) -> request.startAsync());
            assertThat(capture.list).hasSize(1);
            var async = (MockAsyncContext) req.getAsyncContext();
            var listener = async.getListeners().getFirst();
            listener.onStartAsync(new AsyncEvent(async, req, res));
            assertThat(async.getListeners()).hasSize(2);
            listener.onError(new AsyncEvent(async, req, res, new IOException("PRIVATE_DO_NOT_LOG")));
            listener.onComplete(new AsyncEvent(async, req, res));
            listener.onTimeout(new AsyncEvent(async, req, res));
            assertThat(capture.list).hasSize(2);
            assertThat(capture.list.get(1).getKeyValuePairs()).anyMatch(p -> p.key.equals("errorCode") && p.value.equals("CLIENT_DISCONNECT"));
            filter.doFilter(req, res, (request, response) -> {});
            assertThat(capture.list).hasSize(2);
            // /actuatorx와 임의 추가 exclude도 일반 HTTP이므로 기록된다.
            var props = new CommonLoggingProperties(); props.setExcludePathPrefixes(java.util.List.of("/async-error"));
            new AccessLogFilter(new AccessLogger(), props).doFilter(new MockHttpServletRequest("GET", "/actuatorx"), new MockHttpServletResponse(), (r,s) -> {});
            assertThat(capture.list).hasSize(4);
        } finally { logger.detachAppender(capture); }
    }
}
