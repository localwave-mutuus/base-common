package ai.mutuus.common.web;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
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
    void spanId는_전파하지_않고_구간마다_새로_발급한다() throws Exception {
        TraceContext.put(HeaderNames.SPAN_ID, "old-span");
        var request = new MockClientHttpRequest();

        interceptor.intercept(request, new byte[0], echoExecution());

        assertThat(request.getHeaders().getFirst(HeaderNames.SPAN_ID))
                .isNotNull()
                .isNotEqualTo("old-span")
                .hasSize(16);
    }

    private ClientHttpRequestExecution echoExecution() {
        return (req, body) -> new MockClientHttpResponse(new byte[0], 200);
    }
}
