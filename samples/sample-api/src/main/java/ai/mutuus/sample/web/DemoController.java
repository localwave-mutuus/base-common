package ai.mutuus.sample.web;

import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 라이브러리 동작 검증용 엔드포인트. 모든 응답은 표준 {@link ApiResponse} 봉투를 사용한다.
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    /** 성공 응답(ApiResponse) + 추적 컨텍스트 확인. */
    @GetMapping("/public/hello")
    public ApiResponse<Map<String, Object>> hello() {
        return ApiResponse.ok(Map.of("greeting", "hello"));
    }

    /** 전역 예외 처리(ApiResponse 오류 봉투) + i18n 메시지 확인. */
    @GetMapping("/public/boom")
    public ApiResponse<Void> boom() {
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    /** @Valid 검증 실패 → VALIDATION_ERROR + fieldErrors 확인. */
    @PostMapping("/public/echo")
    public ApiResponse<EchoResponse> echo(@Valid @RequestBody EchoRequest request) {
        return ApiResponse.ok(new EchoResponse(request.message()));
    }

    /** 인증된 사용자만 접근 — JWT 자원 서버 자동구성 확인. */
    @GetMapping("/secure/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(Map.of("sub", jwt.getSubject()));
    }

    public record EchoRequest(@NotBlank String message) {
    }

    public record EchoResponse(String echoed) {
    }
}
