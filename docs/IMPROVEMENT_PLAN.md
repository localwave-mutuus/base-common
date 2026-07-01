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
| #18 자동 로그 마스킹 | `StringUtils.mask` + 필드/정규식 마스커를 payload/method 로깅에 조건부 연동 | M |
| #17 멱등성 | Idempotency-Key 필터 + Redis 저장 | M~L |
| #13 Rate Limiting | Bucket4j 공통 필터 | M |
| #14 캐시 추상화 | `@EnableCaching` + Redis CacheManager 기본값 | S~M |
| #15 이벤트/메시징 | Kafka/Rabbit 봉투·공통 설정(인프라 선택 선행) | L |

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
- [ ] Phase 2 이후 순차.

## 테스트 검증 시나리오 (1-B)
| # | 시나리오 | 기대 |
|---|---|---|
| S1 | 라이브러리 기본 코드 회귀 | `CommonErrorCode.NOT_FOUND` → 404, code=NOT_FOUND, i18n(ko/en) 정상 |
| S2 | 소비자 커스텀 코드 | `SampleErrorCode`(ErrorCode 구현) → 동일 봉투로 커스텀 status/code 처리 |
| S3 | 커스텀 코드 i18n | 소비자 messages 번들의 커스텀 키가 ko/en 로 해석 |
| S4 | 봉투 일관성 | 커스텀 코드도 `success/code/message/data/error/traceId/timestamp` 동일 |
| S5 | 화면 단위 | `/demo/index.html` 의 신규 케이스(소비자 커스텀 코드)가 ▶전체실행에서 기대 상태로 PASS |
