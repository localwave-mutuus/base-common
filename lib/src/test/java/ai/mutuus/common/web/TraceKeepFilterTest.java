package ai.mutuus.common.web;

import java.util.Set;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class TraceKeepFilterTest {
    @Test
    void selectedUserMarksServerAndLoginFallback() throws Exception {
        verify("selected", true, false);
        verify("selected", true, true);
    }

    @Test
    void unselectedUserAddsNoAttributes() throws Exception {
        verify("other", false, false);
        verify("other", false, true);
    }

    private void verify(String login, boolean selected, boolean fallback) throws Exception {
        var exporter = InMemorySpanExporter.create();
        try (var provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()) {
            var tracer = provider.get("test");
            var server = tracer.spanBuilder("request").setSpanKind(SpanKind.SERVER).startSpan();
            var request = new MockHttpServletRequest();
            if (!fallback) request.setUserPrincipal(() -> login);
            var filter = new TraceKeepFilter(() -> Set.of("selected"),
                    r -> (String) r.getAttribute("loginId"));
            try (var scope = server.makeCurrent()) {
                filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                    var child = tracer.spanBuilder("controller").startSpan();
                    try (var childScope = child.makeCurrent()) {
                        req.setAttribute("loginId", fallback ? login : "other");
                    } finally { child.end(); }
                });
            } finally { server.end(); }
            var spans = exporter.getFinishedSpanItems();
            var data = spans.stream().filter(s -> s.getName().equals("request")).findFirst().orElseThrow();
            assertEquals(selected ? Boolean.TRUE : null, data.getAttributes().get(
                    io.opentelemetry.api.common.AttributeKey.booleanKey("golmok.trace.keep")));
            assertEquals(selected ? login : null, data.getAttributes().get(
                    io.opentelemetry.api.common.AttributeKey.stringKey("enduser.id")));
            assertTrue(spans.stream().filter(s -> s.getName().equals("controller"))
                    .allMatch(s -> s.getAttributes().isEmpty()));
        }
    }
}
