package ai.mutuus.sample.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.common.exception.ErrorCode;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.sample.persistence.SampleNote;
import ai.mutuus.sample.persistence.SampleNoteRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
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
                        "ai.mutuus.common.logging.AccessLogFilter"),
                new DemoCase("persistence-audit", "영속성 감사(BaseEntity)", "POST", "/demo/audit",
                        "{\"text\":\"hello\"}",
                        "SampleNote 저장 → created_at/by·updated_at/by 자동 기록. created_by 는 TraceContext 사용자(상단 X-User-Id)에서.",
                        "ai.mutuus.common.persistence.TraceContextAuditorAware#getCurrentAuditor"),
                new DemoCase("security-401", "보안 - 미인증 401(auth.failure)", "GET", "/api/secure/me", null,
                        "Bearer 를 비우고 호출 → 401 + auth.failure 로그 + WWW-Authenticate(permit-all 아님).",
                        "ai.mutuus.common.security.LoggingAuthenticationEntryPoint"),
                new DemoCase("security-200", "보안 - 인증 성공(sub=토큰)", "GET", "/api/secure/me", null,
                        "상단 Bearer 에 이름(예: alice) 입력 후 호출 → 200, sub=그 이름, 이후 로그의 X-User-Id 반영(데모 JwtDecoder).",
                        "ai.mutuus.common.security.AuthenticatedUserContextFilter"),
                new DemoCase("session", "분산 세션 컨벤션(Redis)", "GET", "/demo/session", null,
                        "세션 생성 → Redis 에 <namespace>:* 키로 저장. namespace/timeout 컨벤션 확인(로컬 Redis 16010 필요, 키는 두 번째 호출에서 보임).",
                        "ai.mutuus.common.session.CommonSessionAutoConfiguration"),
                new DemoCase("observe", "관측 - 서비스 태그/추적", "GET", "/demo/observe", null,
                        "service.name 태그 + 현재 추적 식별자. 전체 OTel export 는 collector 필요(여기선 컨벤션만 확인).",
                        "ai.mutuus.common.observability.CommonObservabilityAutoConfiguration"),
                new DemoCase("codes", "식별 코드/헤더(App·Instance)", "GET", "/demo/codes", null,
                        "어플리케이션코드(4)·인스턴스구분코드(6) 상수. 응답 헤더 X-App-Code/X-Instance-Id 로도 회신(상단에 표시됨).",
                        "ai.mutuus.common.config.CommonEnvironmentPostProcessor#resolveCodes"),
                new DemoCase("error-malformed", "표준 예외 - 잘못된 포맷(400)", "POST", "/demo/error/validation",
                        "{bad json",
                        "파싱 불가 본문 → HttpMessageNotReadable → 400 MALFORMED_REQUEST.",
                        "ai.mutuus.common.exception.GlobalExceptionHandler#handleMalformed"),
                new DemoCase("error-network", "표준 예외 - 네트워크(502/504)", "GET", "/demo/error/network", null,
                        "해석 불가 호스트로 아웃바운드 호출 → ResourceAccessException → 502 EXTERNAL_API_ERROR(타임아웃은 504).",
                        "ai.mutuus.common.exception.GlobalExceptionHandler#handleNetwork"),
                new DemoCase("logfile", "파일 로그 보기 (<앱>-<인스턴스>.log)", "GET", "/demo/logfile", null,
                        "현재 로그 파일 경로와 마지막 줄(JSON: appCode/instanceCode/traceId 포함). 다른 케이스 몇 번 호출 후 확인.",
                        "lib/src/main/resources/logback-common.xml"));
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

    public record DemoCase(String id, String title, String method, String path,
                           String sampleBody, String observe, String breakpoint) {
    }

    public record ValidationRequest(@NotBlank String name) {
    }
}
