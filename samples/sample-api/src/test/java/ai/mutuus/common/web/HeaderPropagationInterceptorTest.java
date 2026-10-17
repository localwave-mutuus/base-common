package ai.mutuus.common.web;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.TraceHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderPropagationInterceptorTest {

    private final HeaderPropagationInterceptor interceptor = new HeaderPropagationInterceptor();

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void 컨텍스트의_추적헤더를_하위호출에_전파한다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "t-1");
        TraceContext.put(HeaderNames.USER_ID, "u-9");
        var request = new MockClientHttpRequest();

        interceptor.intercept(request, new byte[0], echoExecution());

        assertThat(request.getHeaders().getFirst(HeaderNames.TRACE_ID)).isEqualTo("t-1");
        assertThat(request.getHeaders().getFirst(HeaderNames.USER_ID)).isEqualTo("u-9");
    }

    @Test
    void 현재_spanId를_하위_요청의_parent로_전파한다() throws Exception {
        TraceContext.put(HeaderNames.SPAN_ID, "old-span");
        var request = new MockClientHttpRequest();

        interceptor.intercept(request, new byte[0], echoExecution());

        assertThat(request.getHeaders().getFirst(HeaderNames.SPAN_ID))
                .isEqualTo("old-span");
    }

    @Test
    void W3C를_동시에_보내고_토글로_끌_수_있다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "1234567890abcdef1234567890abcdef");
        TraceContext.put(HeaderNames.SPAN_ID, "1234567890abcdef");
        var request = new MockClientHttpRequest();
        interceptor.intercept(request, new byte[0], echoExecution());
        assertThat(request.getHeaders().getFirst(TraceHeaders.TRACEPARENT))
                .isEqualTo("00-1234567890abcdef1234567890abcdef-1234567890abcdef-01");
        var disabled = new HeaderPropagationInterceptor(java.util.List.of(),
                new ai.mutuus.common.security.audit.SecurityAuditLogger(), false);
        disabled.intercept(request, new byte[0], echoExecution());
        assertThat(request.getHeaders().getFirst(TraceHeaders.TRACEPARENT)).isNull();
        assertThat(request.getHeaders().getFirst(HeaderNames.SPAN_ID)).isEqualTo("1234567890abcdef");
    }

    @Test
    void 미신뢰_호스트에는_신원헤더없이_상관키만_전파한다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "1234567890abcdef1234567890abcdef");
        TraceContext.put(HeaderNames.SPAN_ID, "1234567890abcdef");
        TraceContext.put(HeaderNames.USER_ID, "internal-user");
        var guarded = new HeaderPropagationInterceptor(java.util.List.of("trusted.test"),
                new ai.mutuus.common.security.audit.SecurityAuditLogger(), true);
        var request = new MockClientHttpRequest(org.springframework.http.HttpMethod.GET,
                java.net.URI.create("https://external.test/resource"));
        guarded.intercept(request, new byte[0], echoExecution());
        assertThat(request.getHeaders().getFirst(HeaderNames.USER_ID)).isNull();
        assertThat(request.getHeaders().getFirst(HeaderNames.SPAN_ID)).isEqualTo("1234567890abcdef");
        assertThat(request.getHeaders().getFirst(TraceHeaders.TRACEPARENT)).isNotNull();
    }

    private ClientHttpRequestExecution echoExecution() {
        return (req, body) -> new MockClientHttpResponse(new byte[0], 200);
    }
}
