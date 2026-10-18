package ai.mutuus.common.web;

import java.io.IOException;
import java.util.Map;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.StringUtils;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.TraceHeaders;
import ai.mutuus.common.core.RequestIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/** agent의 SERVER span을 사용하는 호환 헤더/업무 메타데이터 필터. 자체 span은 만들지 않는다. */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceFilter extends OncePerRequestFilter {
    private final String appCode;
    private final String instanceCode;
    private final boolean w3c;
    public TraceFilter(String appCode, String instanceCode) { this(appCode, instanceCode, false); }
    public TraceFilter(String appCode, String instanceCode, boolean trustForwardedUser) {
        this(appCode, instanceCode, trustForwardedUser, true);
    }
    /** trustForwardedUser는 호환 인자이며 검증되지 않은 사용자 헤더는 사용하지 않는다. */
    public TraceFilter(String appCode, String instanceCode, boolean trustForwardedUser, boolean w3c) {
        this.appCode = appCode;
        this.instanceCode = instanceCode;
        this.w3c = w3c;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Map<String, String> previous = TraceContext.snapshot();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        try {
            TraceContext.clear();
            TraceHeaders.Inbound inbound = TraceHeaders.inbound(request::getHeader, w3c);
            request.setAttribute(TraceHeaders.REQUEST_ATTRIBUTE, inbound);
            populate(RequestIds.CONTEXT_KEY, (String) request.getAttribute(RequestIds.HTTP_ATTRIBUTE));
            TraceContext.put(TraceHeaders.W3C_ENABLED, Boolean.toString(w3c));
            populate(HeaderNames.SCREEN_ID, request.getHeader(HeaderNames.SCREEN_ID));
            ai.mutuus.common.core.ScreenMetadata.fromHeaders(request::getHeader).forEach(this::populate);
            populate(HeaderNames.EVENT_ID, request.getHeader(HeaderNames.EVENT_ID));
            populate(HeaderNames.DEVICE_LEVEL, request.getHeader(HeaderNames.DEVICE_LEVEL));
            // 사용자·단말 원문은 인증 또는 별도 개인정보 계약 없이 문맥/로그에 넣지 않는다.
            String claimedUser = request.getHeader(HeaderNames.USER_ID);
            if (StringUtils.hasText(claimedUser)) request.setAttribute(HeaderNames.CLAIMED_USER_ATTR, claimedUser);
            populate(HeaderNames.LOCALE, request.getHeader(HeaderNames.LOCALE));
            populate(HeaderNames.APP_CODE, appCode);
            populate(HeaderNames.INSTANCE_ID, instanceCode);
            if (inbound.traceId() != null) response.setHeader(HeaderNames.TRACE_ID, inbound.traceId());
            response.setHeader(HeaderNames.APP_CODE, appCode);
            response.setHeader(HeaderNames.INSTANCE_ID, instanceCode);
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.replace(previous);
            if (previousMdc == null) MDC.clear(); else MDC.setContextMap(previousMdc);
        }
    }
    private void populate(String header, String value) {
        if (StringUtils.hasText(value) && value.length() <= 64 && value.matches("[A-Za-z0-9_.:-]+"))
            TraceContext.put(header, value);
    }
}
