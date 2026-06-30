# 공통기능 케이스 데모 (테스트 & 디버깅)

`common-platform` 라이브러리가 제공하는 공통기능을 **케이스 단위로 선택해 실행·관찰·디버깅**하기 위한 데모다.
라이브러리(`lib`)는 건드리지 않고 **소비자 샘플 `sample-api` 에만** 데모를 둔다(독립 소비자 충실도·게시 산출물 순수성 유지).

## 실행

- IDE: 실행/디버그 구성 **`sample-api [demo] (공통기능 케이스, H2)`** 선택 → **F5**(디버그) 또는 ▶.
  - 프로파일 `demo,stage`(임베디드 H2, 무인프라), 포트 `8081`, `/demo/**` permit-all.
- UI(케이스 선택·실행): <http://localhost:8081/demo/index.html>
- 케이스 목록(JSON): `GET http://localhost:8081/demo/cases`

UI에서 케이스의 **[실행]** 을 누르면 응답 상태·`X-Trace-Id`·본문을 보여준다. 동시에 **서버 콘솔 로그**(구조화 JSON: `request.received`/`request.completed`/`error.*`)를 함께 본다.
디버깅은 아래 "브레이크포인트" 위치에 중단점을 걸고 [실행]하면 라이브러리 내부에서 멈춘다.

## 케이스 카탈로그

| # | 케이스 | 호출 | 관찰 포인트 | 브레이크포인트 | 자동검증 테스트 |
|---|--------|------|------------|---------------|----------------|
| 1 | e2e 추적/MDC | `GET /demo/trace` | 응답 `X-Trace-Id`, 본문 TraceContext 스냅샷, 로그 traceId | `web.TraceFilter#doFilterInternal` | `CommonPlatformIntegrationTest` |
| 2 | 아웃바운드 헤더 전파 | `GET /demo/outbound` | 다운스트림이 본 traceId(동일)·spanId(신규) | `web.HeaderPropagationInterceptor#intercept` | `OutboundPropagationIntegrationTest` |
| 3a | 표준 예외 - 비즈니스(404) | `GET /demo/error/business` | RFC7807/ApiResponse 오류 봉투, messageKey, traceId | `exception.GlobalExceptionHandler` | `CommonPlatformIntegrationTest` |
| 3b | 표준 예외 - 서버(500) | `GET /demo/error/server` | 500 INTERNAL_ERROR + `error.server` 로그(스택) | `exception.GlobalExceptionHandler` | — |
| 3c | 표준 예외 - 검증(400) | `POST /demo/error/validation` `{"name":""}` | VALIDATION_ERROR + fieldErrors | `exception.GlobalExceptionHandler` | — |
| 4 | i18n 메시지 | `GET /demo/i18n` (+`X-Locale: en`) | 현재/ko/en 메시지 비교 | `i18n.MessageResolver#get` | `MessageResolverTest` |
| 5 | API 생명주기 로깅 | `GET /demo/logging/slow` | `request.completed` 가 WARN(느린 요청) | `logging.AccessLogFilter` | `AccessLoggingIntegrationTest` |
| 6 | 영속성 감사(BaseEntity) | `POST /demo/audit` `{"text":"hello"}` | `created_at/by`·`updated_at/by` 자동 기록 | `persistence.TraceContextAuditorAware#getCurrentAuditor` | `PersistenceAuditingIntegrationTest` |
| 7 | 보안 - 미인증 401 | `GET /api/secure/me` (Bearer 없음) | 401 + `auth.failure` 로그 + `WWW-Authenticate` | `security.LoggingAuthenticationEntryPoint` | `CommonPlatformIntegrationTest` |
| 8 | 보안 - 인증 성공 | `GET /api/secure/me` (Bearer `alice`) | 200, `sub=alice`, 이후 로그 `X-User-Id=alice` | `security.AuthenticatedUserContextFilter` | `CommonPlatformIntegrationTest` |
| 9 | 분산 세션 컨벤션 | `GET /demo/session` (2회) | `demo:session:*` Redis 키, namespace/timeout 컨벤션 | `session.CommonSessionAutoConfiguration` | `RedisLiveSessionIntegrationTest` |
| 10 | 관측 - 서비스 태그/추적 | `GET /demo/observe` | `service.name` 태그 + 추적 식별자 | `observability.CommonObservabilityAutoConfiguration` | `CommonObservabilityAutoConfigurationTest` |

> UI 상단의 `X-Locale`/`X-Screen-Id`/`X-User-Id`/`Bearer` 입력란으로 요청 헤더를 바꿔 케이스 동작 차이를 관찰한다.
> - 감사(6)의 `created_by` 는 `X-User-Id`(TraceContext 사용자)에서 채워진다.
> - 보안 인증성공(8)은 데모 전용 `JwtDecoder`(토큰 문자열 = subject, `@Profile("demo")`)로 실 IdP 없이 인증 경로를 재현한다.
> - 세션(9)은 로컬 실 Redis(`16010`, `default/eva`)가 떠 있어야 한다. 키는 세션이 응답 커밋 시 저장되므로 **두 번째 호출**에서 보인다. Redis 없는 프로파일은 `application.yml` 에서 redis health 를 꺼 영향이 없다.
> - 관측(10)은 `service.name` 태그·추적 식별자(컨벤션)만 확인한다. 전체 OTel span export 는 collector 가 필요하다.

> springdoc(Swagger UI)은 Spring Boot 4(Spring Framework 7) 호환이 확인되면 도입을 검토한다. 현재는 의존성 없는 정적 UI(`/demo/index.html`) + `GET /demo/cases` 카탈로그로 동일 UX를 제공한다.
