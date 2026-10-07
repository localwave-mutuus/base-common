package ai.mutuus.common.logging;

import java.util.concurrent.Executors;
import ai.mutuus.common.async.TraceContextPropagation;
import ai.mutuus.common.core.RequestIds;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.web.TraceFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActionRequestIdTest {
    @Test void shared_accessor_uses_pair_uuid_in_sync_async_and_cleans_request_context() throws Exception {
        var access = mock(AccessLogger.class);
        var filter = new AccessLogFilter(access, new CommonLoggingProperties());
        var request = new MockHttpServletRequest("POST", "/signup");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            new TraceFilter("GMEM", "000001").doFilter(req, res, (innerReq, innerRes) -> {
                var id = RequestIds.current();
                assertThat(java.util.UUID.fromString(id).toString()).isEqualTo(id);
                assertThat(id).isEqualTo(request.getAttribute(RequestIds.HTTP_ATTRIBUTE));
                try (var executor = Executors.newSingleThreadExecutor()) {
                    assertThat(executor.submit(TraceContextPropagation.wrap((java.util.concurrent.Callable<String>) RequestIds::current)).get()).isEqualTo(id);
                } catch (Exception e) { throw new AssertionError(e); }
            });
        });
        assertThat(RequestIds.current()).isNull(); assertThat(TraceContext.traceId()).isNull();
        var id = (String) request.getAttribute(RequestIds.HTTP_ATTRIBUTE);
        verify(access).httpPair(eq("http.request.in"), eq(id), anyString(), eq("POST"), eq("/signup"),
                eq(0), eq(0L), eq(""), isNull(), eq(false), isNull(), anyMap());
        verify(access).httpPair(eq("http.response.out"), eq(id), anyString(), eq("POST"), eq("/signup"),
                eq(200), anyLong(), eq(""), isNull(), eq(false), isNull(), anyMap());
    }
}
