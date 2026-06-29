package ai.mutuus.sample.web;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.common.exception.ErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 라이브러리 동작 검증용 엔드포인트.
 * <ul>
 *   <li>{@code /api/public/**} — permit-all (라이브러리 보안 자동구성 설정)</li>
 *   <li>{@code /api/secure/**} — 인증 필요</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    /** 추적 컨텍스트가 자동으로 채워지는지 확인 (TraceFilter 경유). */
    @GetMapping("/public/hello")
    public Map<String, Object> hello() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "hello");
        body.put("traceId", TraceContext.traceId());   // 라이브러리 ThreadLocal 컨텍스트
        return body;
    }

    /** 전역 예외 처리(RFC7807 ProblemDetail) + i18n 메시지 확인. */
    @GetMapping("/public/boom")
    public String boom() {
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    /** 인증된 사용자만 접근 — JWT 자원 서버 자동구성 확인. */
    @GetMapping("/secure/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", jwt.getSubject());
        return body;
    }
}
