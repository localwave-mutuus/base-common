# 공통모듈 갭 보완 계획 (IMPROVEMENT PLAN)

체크리스트(필수/권장/선택) 점검에서 도출한 갭을 보완하는 작업 계획. **모듈 분리(common-web/security/jpa/client)는 제외/보류** — 현행 **단일 jar + optional 의존성 + 조건부 자동구성 + 큐레이션 스타터/BOM** 구조를 유지한다.

## 유지 원칙
- 도메인 로직/엔티티 금지(횡단 관심사만).
- 모든 신규 기능은 조건부(`@ConditionalOnClass`/`@ConditionalOnProperty`/`@ConditionalOnMissingBean`).
- 테스트는 `samples/*`.

## 공통 DoD (모든 항목)
1. 기능 코드 `lib/src/main/java/ai/mutuus/common/<패키지>`
2. 통합 의존성 `lib/pom.xml`에 `<optional>true</optional>`
3. 자동구성 `@ConditionalOnClass`(+토글/대체) → `AutoConfiguration.imports` 등록
4. 프로퍼티 `@ConfigurationProperties(mutuus.common.*)`
5. 회귀 테스트 `samples/sample-api`(+비전이 시 `sample-batch`)
6. 새 optional 3rd-party면 `common-platform-bom` 버전 등록 + 필요시 스타터 반영
7. 문서(docs/·README·CLAUDE.md)

---

## Phase 1 — 필수 갭 (최우선)

### 1-A. 성공 응답 자동 래핑 (ResponseBodyAdvice) — 갭 #1
- 목적: 컨트롤러 `ApiResponse.ok()` 수동 래핑 자동화.
- 접근: `web`/`api`에 `ResponseBodyAdvice<Object>` + autoconfig, **기본 OFF(opt-in)**.
- 필수 예외: 이미 `ApiResponse`/`ProblemDetail`/`ResponseEntity` 이면 이중래핑 금지, `String`/`byte[]`/스트리밍 제외, **springdoc(`/v3/api-docs`)·actuator·swagger 경로 제외**.
- 리스크 高 · 규모 M.

### 1-B. ErrorCode 인터페이스화 — 갭 #3  ★ 먼저 착수
- 목적: 소비 서비스가 자기 에러코드를 같은 타입으로 확장.
- 접근: `interface ErrorCode { code(); status(); messageKey(); }` 신설 + 기존 enum → `CommonErrorCode implements ErrorCode` 리네임. `BusinessException`/`GlobalExceptionHandler`가 인터페이스 사용.
- 리스크 中(파괴적 리네임 — **0.1.0-SNAPSHOT 미출시라 지금이 적기**) · 규모 M.

---

## Phase 2 — 권장 갭
- **2-A. HTTP 클라 타임아웃/재시도/에러디코딩(#9)**: 기존 RestClient/RestTemplate 커스터마이저 확장(`mutuus.common.http.*`, GET만 재시도 옵션). 리스크 中 · M.
- **2-B. OpenAPI 공통 설정 라이브러리 승격(#11)**: optional `openapi` autoconfig(`@ConditionalOnClass` springdoc) — 공통 `OpenAPI` 빈(info/servers/Bearer JWT 스킴/공통 스키마). 리스크 低~中 · M.
- **2-C. 페이징 응답 객체(+ObjectMapper 공통)(#8)**: `api/PageResponse<T>`(순수 DTO). ObjectMapper 공통설정은 오지랖이라 toggle로 신중히. 리스크 低/中 · S+M.
- **2-D. 커스텀 검증 어노테이션(#7, 경량/선택)**: `validation`에 범용 `@Constraint` 소수만. 도메인성 지양. 리스크 低 · S.

## Phase 3 — 선택 (조직 요구 기반)
| 항목 | 접근 | 규모 |
|---|---|---|
| #18 자동 로그 마스킹 | ✅ **완료** — `SensitiveDataMasker`(정규식, 마지막 4자 유지) opt-in, payload/method 로그의 본문·인자·리턴에서 카드/주민번호 마스킹(응답은 원문). `SensitiveDataMaskerTest`·`MaskingIntegrationTest` + 데모 화면 24/24 PASS(`/demo/mask`). | M |
| #17 멱등성 | ✅ **완료** — `IdempotencyFilter`(opt-in `mutuus.common.idempotency.enabled`, 대상 메서드+`Idempotency-Key` 헤더 있을 때만). 같은 키의 중복 POST/PUT/PATCH 는 재처리 없이 첫 응답 재방(`Idempotent-Replayed: true`), 처리 중 동시 중복은 409. 저장소는 `IdempotencyStore` 인터페이스 + `InMemoryIdempotencyStore` 기본(분산은 소비자가 Redis 구현으로 `@ConditionalOnMissingBean` 대체). `InMemoryIdempotencyStoreTest`·`IdempotencyIntegrationTest`(RANDOM_PORT) + 데모 화면 25/25 PASS(`/demo/idem`). | M~L |
| #13 Rate Limiting | ⛔ **작업 대상 제외**(사용자 결정) — 필요 시 Bucket4j 공통 필터로 별도 진행 | M |
| #14 캐시 추상화 | ✅ **완료** — `CommonCacheAutoConfiguration`(opt-in `mutuus.common.cache.enabled`, `@ConditionalOnClass` Redis 캐시+Boot 캐시 자동구성). `@EnableCaching` + 사용자 `RedisCacheConfiguration` 빈으로 컨벤션(TTL·키 프리픽스 `<service>:cache:`·JSON 직렬화·null 캐싱 비활성) 주입 → Boot 가 `cacheDefaults` 로 채택. 소비자 자체 config/CacheManager 우선(`@ConditionalOnMissingBean`). `CommonCacheConfigurationTest`(단위)·`RedisCacheIntegrationTest`(Testcontainers)·`RedisCacheLiveIntegrationTest`(실 Redis) + 데모 화면 26/26 PASS(`/demo/cache`, 실 Redis 캐싱 실증). | S~M |
| #15 이벤트/메시징 | 🟡 **코어 완료** — broker-agnostic 이벤트 봉투(`DomainEvent<T>`, `of()`가 TraceContext traceId/userId 자동주입) + `EventPublisher` 컨벤션 + `ApplicationEventPublisherAdapter`(in-process 기본). 브로커로 내보내려면 소비자가 `EventPublisher` 대체(`@ConditionalOnMissingBean`, 봉투 규약 유지). `DomainEventTest`·`EventPublishingIntegrationTest` + 데모 화면 27/27 PASS(`/demo/event`). **브로커별 어댑터(Kafka/Rabbit)는 최종 작업 대상에서 제외**(사용자 결정, 인프라/브로커 선택 선행 필요). | L |

---

## 우선순위·규모 요약
| 순위 | 항목 | 규모 | 리스크 |
|---|---|:---:|:---:|
| 1 | 1-B ErrorCode 인터페이스화 | M | 中 |
| 2 | 1-A 응답 자동 래핑 | M | 高 |
| 3 | 2-B OpenAPI 공통 | M | 低 |
| 4 | 2-A HTTP 타임아웃 | M | 中 |
| 5 | 2-C PageResponse | S | 低 |
| 6 | 3-#18 자동 마스킹 | M | 中 |

## 추천 순서
1. Phase 1(1-B → 1-A) — 필수 완성. **1-B는 미출시인 지금** 하는 게 비용 최소.
2. Phase 2(2-B → 2-C → 2-A).
3. Phase 3는 조직 요구 확인된 것만(우선 #18).

각 Phase = 독립 커밋 + 리액터 테스트 그린 + (신규 optional dep 시) BOM/스타터 반영.

---

## 진행 현황
- [x] **1-B. ErrorCode 인터페이스화** — **완료**: `interface ErrorCode` + `CommonErrorCode` enum, 소비자 `SampleErrorCode` 실증. 검증 `CustomErrorCodeIntegrationTest`(S1-S4) + 데모 화면 ▶전체실행 **19/19 PASS**.
- [x] **1-A. 성공 응답 자동 래핑** — **완료**: `ApiResponseWrapperAdvice`(`ResponseBodyAdvice`, 기본 OFF). 이중래핑/`String`·`byte[]`·`Resource`/비JSON/제외경로(springdoc `/v3/api-docs`·actuator·swagger + 소비자 지정 `/demo/cases`)는 통과. 검증 `ResponseWrapperIntegrationTest`(W1~W5) + 데모 화면 ▶전체실행 **20/20 PASS**(신규 `/demo/wrap`).
- [x] **2-B. OpenAPI 공통 설정 승격** — **완료**: `CommonOpenApiAutoConfiguration`(`@ConditionalOnClass(OpenAPI)`, springdoc lib optional). 공통 info + Bearer JWT 보안스킴 주입 → springdoc 이 베이스로 스펙 생성. 검증 `OpenApiCommonConfigIntegrationTest` + 데모 화면 ▶전체실행 **21/21 PASS**(신규 `openapi`), /v3/api-docs 에 info.title·bearerAuth 실앱 확인.
- [x] **2-C. 표준 페이징 응답(PageResponse)** — **완료**: `api/PageResponse<T>`(순수 DTO) + `persistence/PageResponses.from(Page)`(optional 어댑터). 검증 `PageResponseTest`·`PageResponsesTest`·`PageResponseIntegrationTest` + 데모 화면 ▶전체실행 **22/22 PASS**(신규 `/demo/page`, 래퍼 결합). (ObjectMapper 공통설정은 오지랖이라 제외.)
- [x] **2-A. HTTP 클라 타임아웃/재시도/에러디코딩** — **완료**: 타임아웃 컨벤션 기본값(EPP `spring.http.client.connect-timeout`2s/`read-timeout`10s), `HttpRetryInterceptor`(멱등·IOException, opt-in `mutuus.common.http.retry.*`), 타임아웃→504 에러디코딩(SocketTimeout+HttpTimeout). 검증 `HttpRetryInterceptorTest`·`HttpTimeoutIntegrationTest`(RANDOM_PORT) + 데모 화면 ▶전체실행 **23/23 PASS**(신규 `/demo/error/timeout`→504).

- [x] **3-#18. 자동 로그 마스킹** — **완료**: `SensitiveDataMasker`(정규식, 마지막 4자 유지, opt-in `mutuus.common.logging.masking.enabled`). payload/method 로그의 본문·인자·리턴에서 카드/주민번호 마스킹(응답 본문은 원문). 검증 `SensitiveDataMaskerTest`·`MaskingIntegrationTest` + 데모 화면 ▶전체실행 **24/24 PASS**(신규 `/demo/mask`).
- [x] **3-#17. 멱등성(Idempotency-Key)** — **완료**: `IdempotencyFilter`(order `HIGHEST_PRECEDENCE+15`, opt-in `mutuus.common.idempotency.enabled`). 대상 메서드(기본 POST/PUT/PATCH)+`Idempotency-Key` 헤더 있을 때만 동작 — 같은 키 중복은 첫 응답 재방(`Idempotent-Replayed: true`), 처리 중 동시 중복은 409. `IdempotencyStore` 인터페이스 + `InMemoryIdempotencyStore` 기본(분산은 소비자 Redis 구현으로 `@ConditionalOnMissingBean` 대체). 검증 `InMemoryIdempotencyStoreTest`(단위)·`IdempotencyIntegrationTest`(RANDOM_PORT, 같은키→재방/다른키→새응답/키없음→미적용) + 데모 화면 ▶전체실행 **25/25 PASS**(신규 `/demo/idem`).

- [x] **3-#14. 캐시 추상화** — **완료**: `CommonCacheAutoConfiguration`(opt-in `mutuus.common.cache.enabled`, `@ConditionalOnClass` Redis 캐시+Boot 캐시 자동구성). `@EnableCaching`(Boot 4는 캐싱 기본 OFF → 라이브러리가 켬) + 사용자 `RedisCacheConfiguration` 빈으로 컨벤션(TTL·키 프리픽스 `<service>:cache:`·JSON 직렬화·null 캐싱 비활성) 주입. 소비자 자체 config/CacheManager 우선(`@ConditionalOnMissingBean`). 검증 `CommonCacheConfigurationTest`(Redis 없이 단위)·`RedisCacheIntegrationTest`(Testcontainers)·`RedisCacheLiveIntegrationTest`(실 Redis, 로컬 가동 시 PASS) + 데모 화면 ▶전체실행 **26/26 PASS**(신규 `/demo/cache`, 실 Redis 캐싱 실증 sameValue=true).

- [x] **3-#15. 이벤트/메시징 코어(broker-agnostic)** — **완료**: `event` 패키지 — 봉투 `DomainEvent<T>`(eventId·type·occurredAt·traceId·userId·payload, `of()`가 TraceContext 자동주입) + `EventPublisher` 인터페이스(+편의 `publish(type,payload)`) + `ApplicationEventPublisherAdapter`(in-process 기본, Spring `ApplicationEventPublisher` 위임) + `CommonEventAutoConfiguration`(기본 ON, `@ConditionalOnMissingBean` → 소비자 브로커 발행자로 대체 가능). 검증 `DomainEventTest`(단위)·`EventPublishingIntegrationTest`(발행→in-process 수신) + 데모 화면 ▶전체실행 **27/27 PASS**(신규 `/demo/event`, 인입 X-Trace-Id 가 봉투 traceId 로 자동주입 실증). **브로커별 어댑터(Kafka/Rabbit)는 인프라 선택 선행이라 보류.**

**→ Phase 1·2 전부 완료, Phase 3 중 #18·#17·#14·#15(코어) 완료.**

**개선 계획 종료.** 남은 후보(#13 Rate Limiting, #15 브로커별 어댑터)는 **사용자 결정으로 최종 작업 대상에서 제외**한다 — 추후 조직 요구/브로커 선택이 확정되면 별도 건으로 재개. 현 시점 라이브러리는 이 계획서 범위를 완료한 상태다.

## 테스트 검증 시나리오 (1-B)
| # | 시나리오 | 기대 |
|---|---|---|
| S1 | 라이브러리 기본 코드 회귀 | `CommonErrorCode.NOT_FOUND` → 404, code=NOT_FOUND, i18n(ko/en) 정상 |
| S2 | 소비자 커스텀 코드 | `SampleErrorCode`(ErrorCode 구현) → 동일 봉투로 커스텀 status/code 처리 |
| S3 | 커스텀 코드 i18n | 소비자 messages 번들의 커스텀 키가 ko/en 로 해석 |
| S4 | 봉투 일관성 | 커스텀 코드도 `success/code/message/data/error/traceId/timestamp` 동일 |
| S5 | 화면 단위 | `/demo/index.html` 의 신규 케이스(소비자 커스텀 코드)가 ▶전체실행에서 기대 상태로 PASS |
