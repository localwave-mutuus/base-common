package ai.mutuus.common.web;

import java.io.IOException;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.IdGenerator;
import ai.mutuus.common.core.StringUtils;
import ai.mutuus.common.core.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * e2e 정보 계층 진입 필터.
 * <p>인입 요청에서 추적/화면/이벤트/단말/사용자 헤더를 추출하여
 * {@link TraceContext} 및 SLF4J {@link MDC}에 적재한다. 추적ID가 없으면 신규 UUID를 생성한다.
 * 응답 헤더에 추적ID를 회신하고, 요청 종료 시 컨텍스트를 정리한다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {

    private final String appCode;
    private final String instanceCode;

    /** 어플리케이션코드/인스턴스구분코드는 구동 시 확정된 상수다(CommonEnvironmentPostProcessor 해석). */
    public TraceFilter(String appCode, String instanceCode) {
        this.appCode = appCode;
        this.instanceCode = instanceCode;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = StringUtils.defaultIfBlank(
                    request.getHeader(HeaderNames.TRACE_ID), IdGenerator.newTraceId());
            String spanId = IdGenerator.newSpanId();

            populate(HeaderNames.TRACE_ID, traceId);
            populate(HeaderNames.SPAN_ID, spanId);
            populate(HeaderNames.SCREEN_ID, request.getHeader(HeaderNames.SCREEN_ID));
            populate(HeaderNames.EVENT_ID, request.getHeader(HeaderNames.EVENT_ID));
            populate(HeaderNames.DEVICE_LEVEL, request.getHeader(HeaderNames.DEVICE_LEVEL));
            populate(HeaderNames.DEVICE_ID, request.getHeader(HeaderNames.DEVICE_ID));
            populate(HeaderNames.USER_ID, request.getHeader(HeaderNames.USER_ID));
            populate(HeaderNames.LOCALE, request.getHeader(HeaderNames.LOCALE));
            // 애플리케이션/인스턴스 식별 코드(상수) — MDC/TraceContext 적재 → 로그 포함 + 아웃바운드 전파
            populate(HeaderNames.APP_CODE, appCode);
            populate(HeaderNames.INSTANCE_ID, instanceCode);

            response.setHeader(HeaderNames.TRACE_ID, traceId);
            response.setHeader(HeaderNames.APP_CODE, appCode);
            response.setHeader(HeaderNames.INSTANCE_ID, instanceCode);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
            TraceContext.clear();
        }
    }

    private void populate(String header, String value) {
        if (StringUtils.hasText(value)) {
            TraceContext.put(header, value);
            MDC.put(header, value);
        }
    }
}
