package ai.mutuus.common.idempotency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * 멱등성 필터. 지정 메서드(POST/PUT/PATCH 등) 요청에 {@code Idempotency-Key} 헤더가 있으면,
 * <b>같은 키의 중복 요청을 재처리하지 않고 첫 응답을 그대로 재방(replay)</b>한다.
 * <p>헤더가 없거나 대상 메서드가 아니면 평소대로 통과한다. 처리 중인 같은 키의 동시 중복은 409 로 응답한다.
 * 응답 본문 캡처는 {@link ContentCachingResponseWrapper} 로 하고, 재방 응답에는 {@code Idempotent-Replayed}
 * 헤더를 붙인다. 등록: {@code CommonIdempotencyAutoConfiguration}({@code HIGHEST_PRECEDENCE+15}).
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyStore store;
    private final IdempotencyProperties props;
    private final Set<String> methods;

    public IdempotencyFilter(IdempotencyStore store, IdempotencyProperties props) {
        this.store = store;
        this.props = props;
        this.methods = new HashSet<>();
        for (String m : props.getMethods()) {
            this.methods.add(m.toUpperCase(Locale.ROOT));
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getHeader(props.getHeaderName());
        // 대상 메서드가 아니거나 키가 없으면 멱등 처리 없이 통과.
        if (!methods.contains(request.getMethod().toUpperCase(Locale.ROOT)) || !StringUtils.hasText(key)) {
            filterChain.doFilter(request, response);
            return;
        }

        String fingerprint = fingerprint(request);
        boolean reserved = false;
        // legacy transient 응답의 CAS 삭제 및 예약 경쟁을 유한 횟수만 재확인한다.
        for (int attempt = 0; attempt < 3; attempt++) {
            IdempotencyRecord existing = store.find(key);
            if (existing != null) {
                if (fingerprintMismatch(existing, fingerprint)) {
                    conflict(response, "fingerprint-mismatch");
                    return;
                }
                if (!existing.completed()) {
                    conflict(response, "in-progress");
                    return;
                }
                if (!transientFailure(existing.status())) {
                    replay(response, existing);
                    return;
                }
                // 무조건 remove는 다른 retry의 in-progress 예약을 지울 수 있으므로 금지한다.
                if (!store.removeIfCompleted(key, existing)) {
                    conflict(response, "transient-release-unconfirmed");
                    return;
                }
                continue;
            }
            if (store.reserve(key, props.getTtl(), fingerprint)) {
                reserved = true;
                break;
            }
        }
        if (!reserved) {
            conflict(response, "in-progress");
            return;
        }

        // 첫 요청 — 처리하고 응답을 캡처해 저장.
        ContentCachingResponseWrapper cached = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, cached);
            if (transientFailure(cached.getStatus())) {
                // 확인된 HTTP 일시 실패를 완료 응답으로 고착시키지 않는다. 같은 키 재시도 허용.
                store.remove(key);
            } else {
                store.complete(key, IdempotencyRecord.completed(
                        fingerprint, cached.getStatus(), cached.getContentType(), cached.getContentAsByteArray()), props.getTtl());
            }
            cached.copyBodyToResponse();
        } catch (ServletException | IOException | RuntimeException | Error ex) {
            store.remove(key);
            cached.copyBodyToResponse();
            throw ex;
        }
    }

    /** 저장된 첫 응답을 그대로 회신(재처리 없음). */
    private void replay(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.status());
        if (record.contentType() != null) {
            response.setContentType(record.contentType());
        }
        response.setHeader("Idempotent-Replayed", "true");
        if (record.body() != null && record.body().length > 0) {
            response.getOutputStream().write(record.body());
        }
    }

    /** 같은 키가 아직 처리 중일 때(동시 중복). */
    private void conflict(HttpServletResponse response, String reason) {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setHeader("Idempotent-Replayed", reason);
    }

    private boolean fingerprintMismatch(IdempotencyRecord record, String fingerprint) {
        return record.fingerprint() != null && !record.fingerprint().equals(fingerprint);
    }

    private static boolean transientFailure(int status) {
        return status >= 500 && status <= 599 || status == 408 || status == 429;
    }

    private String fingerprint(HttpServletRequest request) {
        String raw = request.getMethod() + " " + request.getRequestURI() + "?" + nullToEmpty(request.getQueryString());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return raw;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
