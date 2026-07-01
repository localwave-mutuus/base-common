package ai.mutuus.sample.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.api.PageResponse;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.common.exception.CommonErrorCode;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.sample.persistence.SampleNote;
import ai.mutuus.sample.persistence.SampleNoteRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 공통기능 케이스 데모 — sample-api(소비 검증용 샘플)에 <b>항상 포함</b>되어 API 와 함께 구동된다.
 * <p>각 엔드포인트가 라이브러리의 한 기능을 단독으로 트리거하므로, 정적 UI(/demo/index.html)에서
 * 케이스를 골라 호출하고 응답·헤더·로그를 관찰하며 라이브러리 코드에 브레이크포인트를 걸어
 * 단계별로 디버깅할 수 있다. 케이스 목록은 {@link #cases()}({@code GET /demo/cases})가 제공한다.
 * <p>{@code /demo/**} 는 application.yml 의 permit-all 기본값에 포함돼 토큰 없이 호출된다.
 */
@RestController
@RequestMapping("/demo")
public class DemoCaseController {

    private final RestClient.Builder restClientBuilder;
    private final MessageResolver messages;
    private final SampleNoteRepository notes;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final String sessionNamespace;
    private final String serviceName;
    private final String appCode;
    private final String instanceCode;

    public DemoCaseController(RestClient.Builder restClientBuilder, MessageResolver messages,
                              SampleNoteRepository notes, ObjectProvider<StringRedisTemplate> redisProvider,
                              @Value("${mutuus.common.session.namespace:demo:session}") String sessionNamespace,
                              @Value("${mutuus.common.service-name:sample-api}") String serviceName,
                              @Value("${mutuus.common.app-code:}") String appCode,
                              @Value("${mutuus.common.instance-code:}") String instanceCode) {
        this.restClientBuilder = restClientBuilder;
        this.messages = messages;
        this.notes = notes;
        this.redisProvider = redisProvider;
        this.sessionNamespace = sessionNamespace;
        this.serviceName = serviceName;
        this.appCode = appCode;
        this.instanceCode = instanceCode;
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
                        "ai.mutuus.common.web.TraceFilter#doFilterInternal", 200, null),
                new DemoCase("outbound", "아웃바운드 헤더 전파", "GET", "/demo/outbound", null,
                        "RestClient 로 자기 자신(/api/public/echo-headers)을 호출. 다운스트림이 본 traceId·appCode·instanceCode 는 동일, spanId 는 새로 발급됨.",
                        "ai.mutuus.common.web.HeaderPropagationInterceptor#intercept", 200, null),
                new DemoCase("error-business", "표준 예외 - 비즈니스(404)", "GET", "/demo/error/business", null,
                        "BusinessException → RFC7807/ApiResponse 오류 봉투. status·messageKey·traceId 부가속성 확인. (기대 404)",
                        "ai.mutuus.common.exception.GlobalExceptionHandler", 404, null),
                new DemoCase("error-server", "표준 예외 - 서버(500)", "GET", "/demo/error/server", null,
                        "처리되지 않은 RuntimeException → 500 INTERNAL_ERROR + error.server 로그(스택 포함). (기대 500)",
                        "ai.mutuus.common.exception.GlobalExceptionHandler", 500, null),
                new DemoCase("error-validation", "표준 예외 - 검증(400)", "POST", "/demo/error/validation",
                        "{\"name\":\"\"}",
                        "@Valid 실패 → VALIDATION_ERROR + fieldErrors. 본문 name 을 비워 호출. (기대 400)",
                        "ai.mutuus.common.exception.GlobalExceptionHandler", 400, null),
                new DemoCase("i18n", "i18n 메시지", "GET", "/demo/i18n", null,
                        "MessageResolver 로 현재 로케일/ko/en 메시지를 함께 반환. 요청 헤더 X-Locale: en 으로 바꿔 비교.",
                        "ai.mutuus.common.i18n.MessageResolver#get", 200, null),
                new DemoCase("logging-slow", "API 생명주기 로깅(느린 요청 WARN)", "GET", "/demo/logging/slow", null,
                        "임계치 이상 지연시켜 request.completed 가 WARN 으로 남는지 확인(AccessLogFilter).",
                        "ai.mutuus.common.logging.AccessLogFilter", 200, null),
                new DemoCase("persistence-audit", "영속성 감사(BaseEntity)", "POST", "/demo/audit",
                        "{\"text\":\"hello\"}",
                        "SampleNote 저장 → created_at/by·updated_at/by 자동 기록. created_by 는 TraceContext 사용자(상단 X-User-Id)에서.",
                        "ai.mutuus.common.persistence.TraceContextAuditorAware#getCurrentAuditor", 200, null),
                new DemoCase("security-401", "보안 - 미인증 401(auth.failure)", "GET", "/api/secure/me", null,
                        "이 케이스는 인증을 보내지 않고 호출 → 401 + auth.failure 로그 + WWW-Authenticate(permit-all 아님). (기대 401)",
                        "ai.mutuus.common.security.LoggingAuthenticationEntryPoint", 401, "none"),
                new DemoCase("security-200", "보안 - 인증 성공(sub=토큰)", "GET", "/api/secure/me", null,
                        "데모 토큰으로 호출(상단 Bearer 입력 시 그 값, 없으면 demo-user) → 200, sub=토큰, 이후 로그 X-User-Id 반영(데모 JwtDecoder).",
                        "ai.mutuus.common.security.AuthenticatedUserContextFilter", 200, "fixed"),
                new DemoCase("session", "분산 세션 컨벤션(Redis)", "GET", "/demo/session", null,
                        "세션 생성 → Redis 에 <namespace>:* 키로 저장. namespace/timeout 컨벤션 확인(로컬 Redis 16010 필요, 키는 두 번째 호출에서 보임).",
                        "ai.mutuus.common.session.CommonSessionAutoConfiguration", 200, null),
                new DemoCase("observe", "관측 - 서비스 태그/추적", "GET", "/demo/observe", null,
                        "service.name 태그 + 현재 추적 식별자. 전체 OTel export 는 collector 필요(여기선 컨벤션만 확인).",
                        "ai.mutuus.common.observability.CommonObservabilityAutoConfiguration", 200, null),
                new DemoCase("codes", "식별 코드/헤더(App·Instance)", "GET", "/demo/codes", null,
                        "어플리케이션코드(4)·인스턴스구분코드(6) 상수. 응답 헤더 X-App-Code/X-Instance-Id 로도 회신(상단에 표시됨).",
                        "ai.mutuus.common.config.CommonEnvironmentPostProcessor#resolveCodes", 200, null),
                new DemoCase("error-malformed", "표준 예외 - 잘못된 포맷(400)", "POST", "/demo/error/validation",
                        "{bad json",
                        "파싱 불가 본문 → HttpMessageNotReadable → 400 MALFORMED_REQUEST. (기대 400)",
                        "ai.mutuus.common.exception.GlobalExceptionHandler#handleMalformed", 400, null),
                new DemoCase("error-network", "표준 예외 - 네트워크(502)", "GET", "/demo/error/network", null,
                        "해석 불가 호스트로 아웃바운드 호출 → ResourceAccessException → 502 EXTERNAL_API_ERROR(타임아웃은 504). (기대 502)",
                        "ai.mutuus.common.exception.GlobalExceptionHandler#handleNetwork", 502, null),
                new DemoCase("error-timeout", "표준 예외 - 타임아웃(504)", "GET", "/demo/error/timeout", null,
                        "짧은 read timeout(300ms)으로 느린 다운스트림(/demo/logging/slow, 1.2s)을 호출 → 타임아웃 → 504 GATEWAY_TIMEOUT. 라이브러리 기본 타임아웃은 spring.http.client.*(EPP 컨벤션 2s/10s). (기대 504)",
                        "ai.mutuus.common.exception.GlobalExceptionHandler#handleNetwork", 504, null),
                new DemoCase("error-custom", "표준 예외 - 소비자 커스텀 코드(409)", "GET", "/demo/error/custom", null,
                        "소비자가 ErrorCode 인터페이스를 구현한 SampleErrorCode.ORDER_ALREADY_SHIPPED 를 던짐 → 라이브러리가 CommonErrorCode 와 동일 봉투로 409 처리(code=ORDER_ALREADY_SHIPPED, i18n 메시지는 소비자 번들). X-Locale: en 으로 바꿔 비교. (기대 409)",
                        "ai.mutuus.sample.demo.SampleErrorCode", 409, null),
                new DemoCase("wrap", "성공 응답 자동 래핑(ResponseBodyAdvice)", "GET", "/demo/wrap", null,
                        "컨트롤러가 ApiResponse 가 아닌 평범한 Map 을 반환해도 라이브러리가 {code:OK,data:{...}} 표준 봉투로 자동 래핑(mutuus.common.response-wrapper). 이미 ApiResponse 인 다른 케이스는 이중 래핑 안 됨. (기대 200)",
                        "ai.mutuus.common.response.ApiResponseWrapperAdvice", 200, null),
                new DemoCase("openapi", "OpenAPI 공통 설정(info + Bearer JWT)", "GET", "/v3/api-docs", null,
                        "라이브러리 CommonOpenApiAutoConfiguration 이 공통 info + Bearer JWT 보안스킴을 주입 → 응답의 info.title·components.securitySchemes.bearerAuth 확인. Swagger UI 우상단 Authorize 로도 확인(상단 🧭 링크). (기대 200)",
                        "ai.mutuus.common.openapi.CommonOpenApiAutoConfiguration", 200, null),
                new DemoCase("page", "표준 페이징 응답(PageResponse)", "GET", "/demo/page?page=0&size=2", null,
                        "목록 API 가 PageResponse(content/page/size/totalElements/totalPages/first/last)로 페이지 메타를 일관 반환. response-wrapper 와 결합돼 {code:OK, data:PageResponse{...}}. page/size 바꿔 확인. (기대 200)",
                        "ai.mutuus.common.api.PageResponse", 200, null),
                new DemoCase("mask", "로그 민감정보 마스킹(카드/주민번호)", "POST", "/demo/mask",
                        "{\"card\":\"9876543210123456\",\"rrn\":\"990101-1234567\",\"name\":\"홍길동\"}",
                        "본문의 카드/주민번호가 payload·method 로그(requestBody/responseBody/args/return)에서 마스킹됨(응답 자체는 원문 — 마스킹은 로깅 한정). 📜 로그뷰어에서 확인. (기대 200)",
                        "ai.mutuus.common.core.SensitiveDataMasker", 200, null),
                new DemoCase("idem", "멱등성(Idempotency-Key) - 중복 POST 재방", "GET", "/demo/idem", null,
                        "같은 Idempotency-Key 로 대상 POST(/demo/idem/echo, 호출마다 새 UUID)를 두 번 호출 → 2차는 재처리 없이 1차 응답 재방(value 동일, Idempotent-Replayed=true). sameResponse=true 기대(mutuus.common.idempotency.enabled). (기대 200)",
                        "ai.mutuus.common.idempotency.IdempotencyFilter", 200, null),
                new DemoCase("params-query", "입력 파라미터 - 쿼리(단일 q + 배열 ids)", "GET",
                        "/demo/params/query?q=hello&ids=a&ids=b", null,
                        "단일 q + 배열 ids 를 쿼리로 전달. 로그의 request.received httpQuery, method.enter args=[hello, [a, b]] 확인. OpenAPI: query parameter(배열=style form).",
                        "ai.mutuus.sample.demo.ParamsDemoController#query", 200, null),
                new DemoCase("params-body", "입력 파라미터 - 본문(단일 tag + 배열 tags)", "POST",
                        "/demo/params/body", "{\"tag\":\"primary\",\"tags\":[\"a\",\"b\",\"c\"]}",
                        "단일 tag + 배열 tags 를 JSON 본문으로 전달. request.payload body, method.enter args=[ParamsRequest[...]] 확인. OpenAPI: requestBody object(array property).",
                        "ai.mutuus.sample.demo.ParamsDemoController#body", 200, null),
                new DemoCase("logfile", "파일 로그 보기 (<앱>-<인스턴스>.log)", "GET", "/demo/logfile", null,
                        "현재 로그 파일 경로와 마지막 줄(JSON: appCode/instanceCode/traceId 포함). 다른 케이스 몇 번 호출 후 확인.",
                        "lib/src/main/resources/logback-common.xml", 200, null));
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
        throw new BusinessException(CommonErrorCode.NOT_FOUND);
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
        String code = CommonErrorCode.NOT_FOUND.messageKey();
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
    // 케이스 6: 영속성 감사 — BaseEntity 상속 엔티티 저장 시 감사 컬럼 자동 기록
    // ---------------------------------------------------------------------

    @PostMapping("/audit")
    public ApiResponse<Map<String, Object>> audit(@RequestBody(required = false) Map<String, Object> body) {
        String text = (body != null && body.get("text") != null) ? body.get("text").toString() : "demo-note";
        SampleNote saved = notes.save(new SampleNote(text));
        return ApiResponse.ok(Map.of(
                "id", saved.getId(),
                "text", saved.getText(),
                "createdAt", String.valueOf(saved.getCreatedAt()),
                "createdBy", String.valueOf(saved.getCreatedBy()),
                "updatedAt", String.valueOf(saved.getUpdatedAt()),
                "updatedBy", String.valueOf(saved.getUpdatedBy())));
    }

    // ---------------------------------------------------------------------
    // 케이스 8: 분산 세션 컨벤션 — 세션 생성 → Redis 에 <namespace>:* 키로 저장
    // ---------------------------------------------------------------------

    @GetMapping("/session")
    public ApiResponse<Map<String, Object>> session(HttpServletRequest request) {
        HttpSession s = request.getSession(true);
        s.setAttribute("demoTouchedAt", Instant.now().toString());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", s.getId());
        result.put("namespace", sessionNamespace);
        result.put("note", "세션은 응답 커밋 시 Redis 에 저장된다 — redisKeys 는 이전 호출분이 보일 수 있음(두 번 호출 권장).");
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                Set<String> keys = redis.keys(sessionNamespace + ":*");
                result.put("redisKeys", keys);
            } catch (RuntimeException e) {
                result.put("redisError", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } else {
            result.put("redisKeys", "(StringRedisTemplate 없음 — redis 미구성)");
        }
        return ApiResponse.ok(result);
    }

    // ---------------------------------------------------------------------
    // 케이스 9: 관측 — service.name 태그 + 현재 추적 식별자
    // ---------------------------------------------------------------------

    @GetMapping("/observe")
    public ApiResponse<Map<String, Object>> observe() {
        return ApiResponse.ok(Map.of(
                "serviceName", serviceName,
                "traceId", TraceContext.traceId(),
                "context", TraceContext.snapshot(),
                "note", "모든 추적/로그에 service.name 태그가 붙는다. 전체 OTel export(span 전송)는 collector 가 필요하다."));
    }

    // ---------------------------------------------------------------------
    // 케이스 10: 식별 코드/헤더 — 어플리케이션코드(4)/인스턴스구분코드(6)
    // ---------------------------------------------------------------------

    @GetMapping("/codes")
    public ApiResponse<Map<String, Object>> codes() {
        return ApiResponse.ok(Map.of(
                "appCode", appCode,
                "instanceCode", instanceCode,
                "traceId", TraceContext.traceId(),
                "note", "응답 헤더 X-App-Code/X-Instance-Id 로도 회신(상단 표시). 아웃바운드 호출·로그 파일명/필드에도 쓰인다."));
    }

    // ---------------------------------------------------------------------
    // 케이스 11: 네트워크 에러 — 해석 불가 호스트로 아웃바운드 → ResourceAccessException
    // ---------------------------------------------------------------------

    @GetMapping("/error/network")
    public ApiResponse<Void> errorNetwork() {
        // 존재하지 않는 호스트(.invalid TLD)로 호출 → UnknownHost → ResourceAccessException
        // → GlobalExceptionHandler#handleNetwork 가 502/504 로 변환(여기로 반환되지 않음).
        restClientBuilder.build().get()
                .uri("http://demo-nonexistent.invalid/api")
                .retrieve()
                .body(String.class);
        return ApiResponse.ok(null);
    }

    // ---------------------------------------------------------------------
    // 케이스: 아웃바운드 타임아웃 → 504. 짧은 read timeout 으로 느린 다운스트림을 호출해
    // 타임아웃을 유도한다(SocketTimeout → ResourceAccessException → GlobalExceptionHandler 504).
    // ---------------------------------------------------------------------

    @GetMapping("/error/timeout")
    public ApiResponse<Void> errorTimeout(HttpServletRequest request) {
        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(300)); // 느린 다운스트림(1.2s)보다 짧게 → 타임아웃 유도
        // 느린 엔드포인트(/demo/logging/slow, 1.2s sleep)를 호출 → read timeout → 여기로 반환되지 않고 504 변환.
        RestClient.builder().requestFactory(factory).build().get()
                .uri(base + "/demo/logging/slow")
                .retrieve()
                .body(String.class);
        return ApiResponse.ok(null);
    }

    // ---------------------------------------------------------------------
    // 케이스: 로그 민감정보 마스킹 — 본문의 카드/주민번호가 payload·method 로그에서 가려진다.
    // (응답 자체는 원문 — 마스킹은 "로깅" 한정. mutuus.common.logging.masking.enabled=true)
    // ---------------------------------------------------------------------

    @PostMapping("/mask")
    public ApiResponse<Map<String, Object>> mask(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(body);
    }

    // ---------------------------------------------------------------------
    // 케이스: 소비자 커스텀 에러 코드 — SampleErrorCode(ErrorCode 인터페이스 구현)를
    // BusinessException 에 실어 던지면, 라이브러리 GlobalExceptionHandler 가 공통 코드와 동일하게 처리.
    // ---------------------------------------------------------------------

    @GetMapping("/error/custom")
    public ApiResponse<Void> errorCustom() {
        // 라이브러리 CommonErrorCode 가 아니라 소비자가 정의한 도메인 코드를 던진다(409 CONFLICT).
        throw new BusinessException(SampleErrorCode.ORDER_ALREADY_SHIPPED);
    }

    // ---------------------------------------------------------------------
    // 케이스: 성공 응답 자동 래핑 — 컨트롤러가 "평범한 객체"를 반환해도
    // ApiResponseWrapperAdvice(ResponseBodyAdvice) 가 표준 ApiResponse 봉투로 감싼다.
    // ---------------------------------------------------------------------

    @GetMapping("/wrap")
    public Map<String, Object> wrap() {
        // 반환 타입이 ApiResponse 가 아니다(평범한 Map). response-wrapper 가 켜져 있으면
        // 라이브러리가 {"code":"OK","data":{...}} 표준 봉투로 자동 래핑한다.
        return Map.of(
                "wrapped", true,
                "value", 42,
                "note", "컨트롤러는 평범한 객체를 반환 — ApiResponseWrapperAdvice 가 표준 봉투로 감쌌다");
    }

    // ---------------------------------------------------------------------
    // 케이스: 표준 페이징 응답(PageResponse) — 목록 API 가 페이지 메타를 일관 형태로 반환.
    // 여기선 데모용 in-memory 목록을 페이징한다(수동 페이징 → PageResponse.of).
    // response-wrapper 와 결합되어 {"code":"OK","data": PageResponse{...}} 로 나간다.
    // ---------------------------------------------------------------------

    @GetMapping("/page")
    public PageResponse<Map<String, Object>> page(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "2") int size) {
        List<Map<String, Object>> all = List.of(
                Map.of("id", 1, "name", "apple"),
                Map.of("id", 2, "name", "banana"),
                Map.of("id", 3, "name", "cherry"),
                Map.of("id", 4, "name", "date"),
                Map.of("id", 5, "name", "elderberry"));
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return PageResponse.of(all.subList(from, to), safePage, safeSize, all.size());
    }

    // ---------------------------------------------------------------------
    // 케이스: 멱등성(Idempotency-Key) — 같은 키의 중복 POST 는 재처리 없이 첫 응답을 재방한다.
    // 데모는 자기 자신(/demo/idem/echo, 호출마다 새 UUID 반환)을 "같은 키"로 두 번 호출한다.
    // 멱등 필터가 켜져 있으면 두 응답의 value 가 동일하고, 2번째 응답에 Idempotent-Replayed 헤더가 붙는다.
    // ---------------------------------------------------------------------

    /** 호출마다 새 값을 반환하는 대상 엔드포인트(멱등 필터가 첫 응답을 캡처·재방하는지 관찰용). */
    @PostMapping("/idem/echo")
    public ApiResponse<Map<String, Object>> idemEcho() {
        return ApiResponse.ok(Map.of(
                "value", java.util.UUID.randomUUID().toString(),
                "nano", System.nanoTime()));
    }

    @GetMapping("/idem")
    public ApiResponse<Map<String, Object>> idem(HttpServletRequest request) {
        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String key = java.util.UUID.randomUUID().toString(); // 두 호출이 공유하는 멱등 키
        RestClient client = restClientBuilder.build();

        // 1차: 실제 처리 → 새 value. 2차: 같은 키 → 재처리 없이 1차 응답 재방(Idempotent-Replayed).
        var first = client.post().uri(base + "/demo/idem/echo")
                .header("Idempotency-Key", key)
                .retrieve().toEntity(Map.class);
        var second = client.post().uri(base + "/demo/idem/echo")
                .header("Idempotency-Key", key)
                .retrieve().toEntity(Map.class);

        Object firstBody = first.getBody();
        Object secondBody = second.getBody();
        String replayed = second.getHeaders().getFirst("Idempotent-Replayed");
        return ApiResponse.ok(Map.of(
                "idempotencyKey", key,
                "first", firstBody,
                "second", secondBody,
                "replayedHeader", String.valueOf(replayed),
                "sameResponse", java.util.Objects.equals(firstBody, secondBody),
                "note", "같은 Idempotency-Key 의 2차 POST 는 재처리 없이 1차 응답을 재방 → value 동일 + Idempotent-Replayed=true"));
    }

    // ---------------------------------------------------------------------
    // 케이스 12: 파일 로그 보기 — <앱코드>-<인스턴스코드>.log 의 마지막 줄
    // ---------------------------------------------------------------------

    @GetMapping("/logfile")
    public ApiResponse<Map<String, Object>> logfile() throws IOException {
        Path path = Path.of("target", "logs", appCode + "-" + instanceCode + ".log");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appCode", appCode);
        result.put("instanceCode", instanceCode);
        result.put("file", path.toAbsolutePath().toString());
        if (Files.exists(path)) {
            List<String> all = Files.readAllLines(path);
            int from = Math.max(0, all.size() - 12);
            result.put("lineCount", all.size());
            result.put("tail", all.subList(from, all.size()));
        } else {
            result.put("exists", false);
            result.put("hint", "아직 파일이 없음 — 다른 케이스를 몇 번 호출한 뒤 다시 확인(LOG_DIR=target/logs).");
        }
        return ApiResponse.ok(result);
    }

    // ---------------------------------------------------------------------
    // 로그 뷰어 tail(/demo/logs/tail)은 1.5초 폴링이라 데모 케이스와 분리 →
    // ai.mutuus.sample.logs.LogTailController (로깅 타게팅에서 제외되게 별도 패키지).
    // ---------------------------------------------------------------------

    /**
     * @param expectStatus 이 케이스가 정상 동작 시 반환하는 HTTP 상태(에러 케이스 포함). UI 가 실제==기대면 PASS 로 표시.
     * @param authMode      인증 처리: {@code "none"}=인증 미전송, {@code "fixed"}=데모 토큰(상단 Bearer 우선), {@code null}=상단 Bearer 입력만.
     */
    public record DemoCase(String id, String title, String method, String path,
                           String sampleBody, String observe, String breakpoint,
                           int expectStatus, String authMode) {
    }

    public record ValidationRequest(@NotBlank String name) {
    }
}
