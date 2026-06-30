package ai.mutuus.sample.demo;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.common.exception.ErrorCode;
import ai.mutuus.common.i18n.MessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * 공통기능 케이스 데모 — {@code demo} 프로파일에서만 활성화된다(@Profile("demo")).
 * <p>각 엔드포인트가 라이브러리의 한 기능을 단독으로 트리거하므로, 정적 UI(/demo/index.html)에서
 * 케이스를 골라 호출하고 응답·헤더·로그를 관찰하며 라이브러리 코드에 브레이크포인트를 걸어
 * 단계별로 디버깅할 수 있다. 케이스 목록은 {@link #cases()}({@code GET /demo/cases})가 제공한다.
 * <p>운영 소비 서비스에는 영향이 없다(프로파일 미활성 시 빈으로 등록되지 않음).
 */
@RestController
@RequestMapping("/demo")
@Profile("demo")
public class DemoCaseController {

    private final RestClient.Builder restClientBuilder;
    private final MessageResolver messages;

    public DemoCaseController(RestClient.Builder restClientBuilder, MessageResolver messages) {
        this.restClientBuilder = restClientBuilder;
        this.messages = messages;
    }

    // ---------------------------------------------------------------------
    // 케이스 카탈로그(목록) — UI 가 이 목록으로 선택지를 렌더한다.
    // ---------------------------------------------------------------------

    /** 공통기능 케이스 목록. UI(/demo/index.html)가 이걸 불러 선택 메뉴를 만든다. */
    @GetMapping("/cases")
    public List<DemoCase> cases() {
        return List.of(
                new DemoCase("trace", "e2e 추적/MDC", "GET", "/demo/trace", null,
                        "TraceFilter 가 적재한 TraceContext 스냅샷. 응답 헤더 X-Trace-Id 와 서버 로그의 traceId 를 함께 확인.",
                        "ai.mutuus.common.web.TraceFilter#doFilterInternal"),
                new DemoCase("outbound", "아웃바운드 헤더 전파", "GET", "/demo/outbound", null,
                        "RestClient 로 자기 자신(/api/public/echo-headers)을 호출. 다운스트림이 본 traceId 는 동일, spanId 는 새로 발급됨.",
                        "ai.mutuus.common.web.HeaderPropagationInterceptor#intercept"),
                new DemoCase("error-business", "표준 예외 - 비즈니스(404)", "GET", "/demo/error/business", null,
                        "BusinessException → RFC7807/ApiResponse 오류 봉투. status·messageKey·traceId 부가속성 확인.",
                        "ai.mutuus.common.exception.GlobalExceptionHandler"),
                new DemoCase("error-server", "표준 예외 - 서버(500)", "GET", "/demo/error/server", null,
                        "처리되지 않은 RuntimeException → 500 INTERNAL_ERROR + error.server 로그(스택 포함).",
                        "ai.mutuus.common.exception.GlobalExceptionHandler"),
                new DemoCase("error-validation", "표준 예외 - 검증(400)", "POST", "/demo/error/validation",
                        "{\"name\":\"\"}",
                        "@Valid 실패 → VALIDATION_ERROR + fieldErrors. 본문 name 을 비워 호출.",
                        "ai.mutuus.common.exception.GlobalExceptionHandler"),
                new DemoCase("i18n", "i18n 메시지", "GET", "/demo/i18n", null,
                        "MessageResolver 로 현재 로케일/ko/en 메시지를 함께 반환. 요청 헤더 X-Locale: en 으로 바꿔 비교.",
                        "ai.mutuus.common.i18n.MessageResolver#get"),
                new DemoCase("logging-slow", "API 생명주기 로깅(느린 요청 WARN)", "GET", "/demo/logging/slow", null,
                        "임계치 이상 지연시켜 request.completed 가 WARN 으로 남는지 확인(AccessLogFilter).",
                        "ai.mutuus.common.logging.AccessLogFilter"));
    }

    // ---------------------------------------------------------------------
    // 케이스 1: e2e 추적/MDC
    // ---------------------------------------------------------------------

    @GetMapping("/trace")
    public ApiResponse<Map<String, Object>> trace() {
        return ApiResponse.ok(Map.of(
                "traceId", TraceContext.traceId(),
                "userId", String.valueOf(TraceContext.userId()),
                "context", TraceContext.snapshot()));
    }

    // ---------------------------------------------------------------------
    // 케이스 2: 아웃바운드 헤더 전파 (RestClient → 자기 자신)
    // ---------------------------------------------------------------------

    @GetMapping("/outbound")
    public ApiResponse<Map<String, Object>> outbound(HttpServletRequest request) {
        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        // Boot 가 자동구성한 RestClient.Builder → 라이브러리의 HeaderPropagationInterceptor 가 적용됨.
        RestClient client = restClientBuilder.build();
        Object downstream = client.get()
                .uri(base + "/api/public/echo-headers")
                .retrieve()
                .body(Object.class);
        return ApiResponse.ok(Map.of(
                "callerTraceId", TraceContext.traceId(),
                "downstreamSaw", downstream));
    }

    // ---------------------------------------------------------------------
    // 케이스 3: 표준 예외 (business / server / validation)
    // ---------------------------------------------------------------------

    @GetMapping("/error/business")
    public ApiResponse<Void> errorBusiness() {
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    @GetMapping("/error/server")
    public ApiResponse<Void> errorServer() {
        throw new IllegalStateException("demo: 의도된 서버 오류");
    }

    @PostMapping("/error/validation")
    public ApiResponse<Void> errorValidation(@Valid @RequestBody ValidationRequest body) {
        return ApiResponse.ok(null); // @Valid 실패 시 도달하지 않음(GlobalExceptionHandler 가 처리)
    }

    // ---------------------------------------------------------------------
    // 케이스 4: i18n 메시지
    // ---------------------------------------------------------------------

    @GetMapping("/i18n")
    public ApiResponse<Map<String, Object>> i18n() {
        String code = ErrorCode.NOT_FOUND.messageKey();
        return ApiResponse.ok(Map.of(
                "code", code,
                "current", messages.get(code),
                "currentLocale", LocaleContextHolder.getLocale().toString(),
                "ko", messages.get(Locale.KOREAN, code),
                "en", messages.get(Locale.ENGLISH, code)));
    }

    // ---------------------------------------------------------------------
    // 케이스 5: API 생명주기 로깅 — 느린 요청 → request.completed WARN
    // ---------------------------------------------------------------------

    @GetMapping("/logging/slow")
    public ApiResponse<Map<String, Object>> slow() throws InterruptedException {
        long millis = 1200L; // 기본 slow-request 임계치(1000ms) 초과 유도
        Thread.sleep(millis);
        return ApiResponse.ok(Map.of("sleptMillis", millis,
                "hint", "서버 로그의 request.completed 가 WARN 인지 확인"));
    }

    // ---------------------------------------------------------------------

    public record DemoCase(String id, String title, String method, String path,
                           String sampleBody, String observe, String breakpoint) {
    }

    public record ValidationRequest(@NotBlank String name) {
    }
}
