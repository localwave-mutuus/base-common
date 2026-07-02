package ai.mutuus.sample.demo.security;

import java.util.List;
import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보안 테스트 화면(/demo/security.html)이 호출하는 <b>보호된</b> 데모 엔드포인트.
 * <p>{@code /demo/**} 는 permit-all 이지만 이 컨트롤러는 <b>{@code /api/secure}·{@code /api/admin}</b> 아래라
 * 기본 체인의 {@code anyRequest().authenticated()} 대상이다 → 인증/인가 위배를 실제로 유발해 보안 감사 로그
 * ({@code security.authn.failed}/{@code security.authz.denied})를 눈으로 검증하게 한다.
 * <ul>
 *   <li>{@code GET /api/secure/whoami} — 토큰 없으면 401, 있으면 주체/권한 반환(200)</li>
 *   <li>{@code GET /api/admin/ping} — {@code ROLE_ADMIN} 필요 → USER 토큰이면 403</li>
 * </ul>
 * (ai 프로파일의 mock JwtDecoder 는 {@code Authorization: Bearer <name>} 의 name 을 주체, 권한을 USER 로 준다.)
 */
@RestController
public class SecurityDemoController {

    @Operation(summary = "인증 필요 데모(whoami)", description = "토큰 없으면 401. 있으면 주체/권한 반환.")
    @GetMapping("/api/secure/whoami")
    public ApiResponse<Map<String, Object>> whoami(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(Object::toString).toList();
        return ApiResponse.ok(Map.of("subject", authentication.getName(), "authorities", authorities));
    }

    @Operation(summary = "관리자 권한 데모(ping)", description = "ROLE_ADMIN 필요 → USER 토큰이면 403(authz.denied).")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/ping")
    public ApiResponse<String> adminPing() {
        return ApiResponse.ok("pong (admin)");
    }

    @Operation(summary = "서버 오류 데모(boom)",
            description = "런타임 예외 → 500. 응답 본문엔 내부 예외 클래스명이 노출되지 않는다(정보 노출 방지).")
    @GetMapping("/api/secure/boom")
    public ApiResponse<String> boom() {
        throw new IllegalStateException("의도된 데모 예외(내부 상세) — 응답엔 노출되지 않아야 함");
    }

    @Operation(summary = "공개 엔드포인트(신뢰경계 데모)",
            description = "permit-all. 인입 X-User-Id 위장 헤더는 신뢰되지 않아 폐기된다(security.identity.header_ignored). "
                    + "미인증이면 traceContextUserId 는 비어 있어야 정상(감사 위조 방지).")
    @GetMapping("/api/public/whoami")
    public ApiResponse<Map<String, Object>> publicWhoami() {
        String userId = ai.mutuus.common.core.TraceContext.userId();
        return ApiResponse.ok(Map.of("traceContextUserId", userId == null ? "" : userId));
    }
}
