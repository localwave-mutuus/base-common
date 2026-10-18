package ai.mutuus.common.web;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 승인한 내부 호스트에 화면/로케일 힌트를 전파한다. Java agent가 CLIENT span과 W3C 전파를 소유한다.
 * <p>커스텀 trace/span/user 주장은 전달하지 않는다. 식별 헤더 차단은 값 없이 감사 이벤트에 남긴다.
 */
public class HeaderPropagationInterceptor implements ClientHttpRequestInterceptor {

    private final List<String> allowedHosts;
    private final SecurityAuditLogger securityAuditLogger;

    /** allowlist 미지정 시 식별 메타데이터 전파를 허용하지 않는다. */
    public HeaderPropagationInterceptor() {
        this(List.of(), new SecurityAuditLogger());
    }

    public HeaderPropagationInterceptor(List<String> allowedHosts, SecurityAuditLogger securityAuditLogger) {
        this(allowedHosts, securityAuditLogger, true);
    }

    public HeaderPropagationInterceptor(List<String> allowedHosts, SecurityAuditLogger securityAuditLogger, boolean w3c) {
        this.allowedHosts = allowedHosts == null ? List.of() : List.copyOf(allowedHosts);
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        var headers = request.getHeaders();
        String host = request.getURI().getHost();
        boolean trusted = isTrusted(host);

        StringJoiner dropped = new StringJoiner(",");
        if (!trusted) {
            for (String name : HeaderNames.IDENTITY) {
                if (headers.containsHeader(name) || TraceContext.get(name).isPresent()) dropped.add(name);
            }
        }
        // Agent creates the CLIENT span and injects W3C headers. Never inject a SERVER span
        // or turn an unverified user/header assertion into downstream identity.
        headers.remove(HeaderNames.USER_ID);
        headers.remove(HeaderNames.TRACE_ID);
        headers.remove(HeaderNames.SPAN_ID);
        if (trusted) {
            for (String name : List.of(HeaderNames.SCREEN_ID, HeaderNames.LOCALE)) {
                TraceContext.get(name).ifPresent(v -> headers.set(name, v));
            }
        } else {
            for (String name : HeaderNames.IDENTITY) headers.remove(name);
            if (dropped.length() > 0) securityAuditLogger.propagationBlocked(host, dropped.toString());
        }
        return execution.execute(request, body);
    }

    /** 정확 일치 또는 하위 도메인 일치, 빈 allowlist는 fail-closed. */
    private boolean isTrusted(String host) {
        if (allowedHosts.isEmpty()) return false;
        if (host == null) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        for (String allowed : allowedHosts) {
            String a = allowed.toLowerCase(Locale.ROOT);
            if (h.equals(a) || h.endsWith("." + a)) {
                return true;
            }
        }
        return false;
    }
}
