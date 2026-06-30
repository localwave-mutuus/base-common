package ai.mutuus.common.web;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFilterTest {

    private final TraceFilter filter = new TraceFilter("TEST", "INST01");

    @AfterEach
    void tearDown() {
        TraceContext.clear();
        MDC.clear();
    }

    @Test
    void 인입_헤더가_없으면_traceId를_생성해_컨텍스트_MDC_응답헤더에_싣는다() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                // 체인 진행 시점에 컨텍스트가 적재돼 있어야 한다
                assertThat(TraceContext.traceId()).isNotBlank();
                assertThat(MDC.get(HeaderNames.TRACE_ID)).isNotBlank();
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(HeaderNames.TRACE_ID)).isNotBlank();
    }

    @Test
    void 인입_헤더에_traceId가_있으면_보존하고_그대로_회신한다() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(HeaderNames.TRACE_ID, "fixed-trace-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HeaderNames.TRACE_ID)).isEqualTo("fixed-trace-123");
    }

    @Test
    void 빈_선택_헤더는_컨텍스트에_적재하지_않는다() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(HeaderNames.SCREEN_ID, "   ");   // 공백
        var captured = new String[1];
        var chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                captured[0] = TraceContext.get(HeaderNames.SCREEN_ID).orElse(null);
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void 요청_종료_후_ThreadLocal과_MDC가_정리된다() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(HeaderNames.USER_ID, "u-1");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(TraceContext.traceId()).isNull();
        assertThat(TraceContext.get(HeaderNames.USER_ID)).isEmpty();
        assertThat(MDC.get(HeaderNames.USER_ID)).isNull();
    }
}
