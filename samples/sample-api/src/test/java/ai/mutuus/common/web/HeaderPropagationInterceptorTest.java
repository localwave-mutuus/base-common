package ai.mutuus.common.web;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.TraceHeaders;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** agent 없는 단위 시험: CLIENT span/W3C 주입 성공을 주장하지 않고 interceptor의 보안 경계를 검증한다. */
class HeaderPropagationInterceptorTest {
    @AfterEach
    void tearDown() { TraceContext.clear(); }

    @Test
    void 기존_커스텀_추적과_사용자_헤더는_신뢰호스트에도_전파하지_않는다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "forged-trace");
        TraceContext.put(HeaderNames.USER_ID, "forged-user");
        var request = request("trusted.test");
        request.getHeaders().set(HeaderNames.TRACE_ID, "old-trace");
        request.getHeaders().set(HeaderNames.SPAN_ID, "old-span");
        request.getHeaders().set(HeaderNames.USER_ID, "old-user");
        var calls = new AtomicInteger();
        new HeaderPropagationInterceptor(List.of("trusted.test"), new SecurityAuditLogger())
                .intercept(request, new byte[]{1, 2}, (outgoing, body) -> {
                    calls.incrementAndGet();
                    for (String name : List.of(HeaderNames.TRACE_ID, HeaderNames.SPAN_ID, HeaderNames.USER_ID)) {
                        assertThat(outgoing.getHeaders().getFirst(name)).isNull();
                    }
                    assertThat(body).containsExactly((byte) 1, (byte) 2);
                    return new MockClientHttpResponse(new byte[0], 200);
                });
        assertThat(calls).hasValue(1);
    }

    @Test
    void 현재_server_span을_수동으로_W3C_주입하지_않는다() throws Exception {
        var server = Span.wrap(SpanContext.create("1234567890abcdef1234567890abcdef", "1234567890abcdef",
                TraceFlags.getSampled(), TraceState.getDefault()));
        var request = request("trusted.test");
        try (Scope ignored = server.makeCurrent()) {
            new HeaderPropagationInterceptor(List.of("trusted.test"), new SecurityAuditLogger())
                    .intercept(request, new byte[0], echoExecution());
            for (String name : List.of(TraceHeaders.TRACEPARENT, HeaderNames.TRACE_ID,
                    HeaderNames.SPAN_ID, HeaderNames.USER_ID)) {
                assertThat(request.getHeaders().getFirst(name)).isNull();
            }
            assertThat(Span.current().getSpanContext()).isEqualTo(server.getSpanContext());
        }
    }

    @Test
    void 기존_W3C_헤더는_인터셉터가_변경하거나_지우지_않는다() throws Exception {
        String traceparent = "00-1234567890abcdef1234567890abcdef-abcdef1234567890-01";
        var request = request("external.test");
        request.getHeaders().set(TraceHeaders.TRACEPARENT, traceparent);
        request.getHeaders().set("tracestate", "synthetic=value");
        // false는 과거 수동 주입 토글. 0.4에서는 agent 소유 헤더를 건드리지 않는다.
        new HeaderPropagationInterceptor(List.of(), new SecurityAuditLogger(), false)
                .intercept(request, new byte[0], echoExecution());
        assertThat(request.getHeaders().getFirst(TraceHeaders.TRACEPARENT)).isEqualTo(traceparent);
        assertThat(request.getHeaders().getFirst("tracestate")).isEqualTo("synthetic=value");
    }

    @Test
    void 미신뢰와_빈_allowlist는_식별헤더를_제거한다() throws Exception {
        for (List<String> allowed : List.of(List.<String>of(), List.of("trusted.test"))) {
            var request = request("external.test");
            HeaderNames.IDENTITY.forEach(name -> request.getHeaders().set(name, "synthetic"));
            new HeaderPropagationInterceptor(allowed, new SecurityAuditLogger())
                    .intercept(request, new byte[0], echoExecution());
            HeaderNames.IDENTITY.forEach(name -> assertThat(request.getHeaders().getFirst(name)).isNull());
        }
    }

    @Test
    void 신뢰호스트와_하위호스트에는_허용_화면과_로케일만_문맥에서_추가한다() throws Exception {
        TraceContext.put(HeaderNames.SCREEN_ID, "synthetic-screen");
        TraceContext.put(HeaderNames.LOCALE, "ko-KR");
        TraceContext.put(HeaderNames.DEVICE_ID, "sensitive-device");
        for (String host : List.of("trusted.test", "api.trusted.test")) {
            var request = request(host);
            new HeaderPropagationInterceptor(List.of("trusted.test"), new SecurityAuditLogger())
                    .intercept(request, new byte[0], echoExecution());
            assertThat(request.getHeaders().getFirst(HeaderNames.SCREEN_ID)).isEqualTo("synthetic-screen");
            assertThat(request.getHeaders().getFirst(HeaderNames.LOCALE)).isEqualTo("ko-KR");
            assertThat(request.getHeaders().getFirst(HeaderNames.DEVICE_ID)).isNull();
        }
        var lookalike = request("nottrusted.test");
        new HeaderPropagationInterceptor(List.of("trusted.test"), new SecurityAuditLogger())
                .intercept(lookalike, new byte[0], echoExecution());
        assertThat(lookalike.getHeaders().getFirst(HeaderNames.SCREEN_ID)).isNull();
    }

    private static MockClientHttpRequest request(String host) {
        return new MockClientHttpRequest(HttpMethod.GET, URI.create("https://" + host + "/resource"));
    }
    private static ClientHttpRequestExecution echoExecution() {
        return (req, body) -> new MockClientHttpResponse(new byte[0], 200);
    }
}