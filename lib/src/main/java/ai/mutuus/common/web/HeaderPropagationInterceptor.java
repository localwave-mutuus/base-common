package ai.mutuus.common.web;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.IdGenerator;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 아웃바운드 호출(RestClient/RestTemplate)에 e2e 헤더를 자동 부착한다.
 * <p>현재 {@link TraceContext}의 추적/화면/이벤트/단말/사용자 헤더를 하위 API로 전파하며, span ID는 호출 구간마다
 * 새로 발급한다. <b>보안</b>: 대상 호스트가 신뢰 allowlist({@code mutuus.common.propagation.allowed-hosts}) 밖이면
 * 식별/민감 헤더({@link HeaderNames#IDENTITY})는 <b>부착하지 않고</b>({@code security.propagation.blocked} 기록)
 * 상관용(trace/span/locale)만 전파해 외부로의 사용자·내부 토폴로지 유출을 막는다. allowlist 가 비어 있으면 전부 전파(하위호환).
 */
public class HeaderPropagationInterceptor implements ClientHttpRequestInterceptor {

    private final List<String> allowedHosts;
    private final SecurityAuditLogger securityAuditLogger;

    /** 하위호환 기본 — allowlist 없음(전부 전파), 감사 로깅 무동작. */
    public HeaderPropagationInterceptor() {
        this(List.of(), new SecurityAuditLogger());
    }

    public HeaderPropagationInterceptor(List<String> allowedHosts, SecurityAuditLogger securityAuditLogger) {
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
        for (String name : HeaderNames.PROPAGATED) {
            if (HeaderNames.SPAN_ID.equals(name)) {
                continue; // span 은 구간마다 새로 발급
            }
            if (!trusted && HeaderNames.IDENTITY.contains(name)) {
                if (TraceContext.get(name).isPresent()) {
                    dropped.add(name); // 신뢰 밖 호스트엔 식별 헤더 미부착
                }
                continue;
            }
            TraceContext.get(name).ifPresent(v -> headers.set(name, v));
        }
        if (dropped.length() > 0) {
            securityAuditLogger.propagationBlocked(host, dropped.toString());
        }
        // 하위 호출 구간 식별을 위한 신규 span
        headers.set(HeaderNames.SPAN_ID, IdGenerator.newSpanId());
        return execution.execute(request, body);
    }

    /** allowlist 가 비면 모두 신뢰(하위호환). 지정 시 정확 일치 또는 {@code .suffix} 접미 일치(대소문자 무시). */
    private boolean isTrusted(String host) {
        if (allowedHosts.isEmpty()) {
            return true;
        }
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
