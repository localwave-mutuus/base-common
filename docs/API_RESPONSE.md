# 표준 API 응답 봉투 (ApiResponse)

common-platform 은 모든 API 통신을 **성공/오류 통합 봉투** `ApiResponse<T>` 로 감싼다.
성공이든 오류든 본문 구조가 **항상 동일**하며, 클라이언트는 `code` 로 분기한다(성공은 `"OK"`).
HTTP 상태코드는 별도로 정확히 설정되고(예: 404), 본문 형태는 바뀌지 않는다.

## 1. 구조

```jsonc
{
  "code":      "OK",            // 안정적 코드. 성공="OK", 오류=ErrorCode 이름(예: "NOT_FOUND")
  "message":   "리소스를 찾을 수 없습니다.",  // 사람이 읽는 메시지(i18n 적용 결과)
  "data":      { /* ... */ },   // 성공 페이로드(오류 시 null)
  "error":     null,            // 오류 상세(성공 시 null) — 아래 ApiError
  "traceId":   "ab12cd34",      // 응답 자체의 추적 ID(TraceContext)
  "timestamp": "2026-06-29T16:00:00.123+09:00"
}
```

`error`(ApiError):

```jsonc
{
  "code":        "VALIDATION_ERROR",   // 클라이언트 분기용 안정적 코드
  "detail":      "요청 값 검증에 실패했습니다.",
  "fieldErrors": [                       // 검증 실패 시 필드별 사유(없으면 빈 배열)
    { "field": "message", "reason": "must not be blank" }
  ],
  "exception":   "java.lang.IllegalStateException"  // 서버 오류 진단용(노출 정책에 따라 null)
}
```

## 2. 성공 응답

컨트롤러는 `ApiResponse.ok(data)` 로 감싼다.

```java
@GetMapping("/api/orders/{id}")
public ApiResponse<OrderDto> get(@PathVariable Long id) {
    return ApiResponse.ok(orderService.find(id));
}
```

```jsonc
// 200 OK
{ "code":"OK", "message":"OK", "data":{ "id":9, "status":"PAID" },
  "error":null, "traceId":"ab12cd34", "timestamp":"..." }
```

## 3. 오류 응답

오류는 컨트롤러가 직접 만들지 않는다. `BusinessException(ErrorCode)` 를 던지면
`GlobalExceptionHandler` 가 표준 봉투로 변환하고, 상태코드·메시지(i18n)·`fieldErrors` 를 채운다.

```java
throw new BusinessException(CommonErrorCode.NOT_FOUND);   // 라이브러리 기본 코드
// 소비 서비스는 ErrorCode 인터페이스를 구현한 자체 코드도 던질 수 있다:
// throw new BusinessException(OrderErrorCode.ALREADY_SHIPPED);
```

```jsonc
// 404 Not Found
{ "code":"NOT_FOUND", "message":"리소스를 찾을 수 없습니다.",
  "data":null,
  "error":{ "code":"NOT_FOUND", "detail":"리소스를 찾을 수 없습니다.", "fieldErrors":[], "exception":null },
  "traceId":"ab12cd34", "timestamp":"..." }
```

프레임워크 예외도 자동 매핑된다(별도 코드 불필요):

| 예외 | code | HTTP |
|------|------|------|
| `@Valid` 검증 실패(`MethodArgumentNotValidException`) | `VALIDATION_ERROR` | 400 (+ `fieldErrors`) |
| `HttpRequestMethodNotSupportedException` | `METHOD_NOT_ALLOWED` | 405 |
| `HttpMediaTypeNotSupportedException` | `UNSUPPORTED_MEDIA_TYPE` | 415 |
| `NoResourceFoundException` | `NOT_FOUND` | 404 |
| 그 외 처리되지 않은 모든 예외 | `INTERNAL_ERROR` | 500 (스택 ERROR 로깅 + `error.exception`) |

## 4. ErrorCode 카탈로그

라이브러리 기본 코드는 **`CommonErrorCode` enum**(= `ErrorCode` 인터페이스 구현)이며, `code` 는 그 이름과 동일하다(`ErrorCode.code()`). 메시지는 `messages/messages*.properties` 의 `messageKey` 로 로케일별 변환된다(`X-Locale` 헤더).

| code | HTTP | messageKey |
|------|------|-----------|
| `INVALID_REQUEST` | 400 | `error.invalid.request` |
| `VALIDATION_ERROR` | 400 | `error.validation` |
| `MALFORMED_REQUEST` | 400 | `error.malformed` |
| `UNAUTHORIZED` | 401 | `error.unauthorized` |
| `FORBIDDEN` | 403 | `error.forbidden` |
| `NOT_FOUND` | 404 | `error.not.found` |
| `METHOD_NOT_ALLOWED` | 405 | `error.method.not.allowed` |
| `CONFLICT` | 409 | `error.conflict` |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | `error.unsupported.media.type` |
| `TOO_MANY_REQUESTS` | 429 | `error.too.many.requests` |
| `INTERNAL_ERROR` | 500 | `error.internal` |
| `EXTERNAL_API_ERROR` | 502 | `error.external.api` |
| `SERVICE_UNAVAILABLE` | 503 | `error.service.unavailable` |
| `GATEWAY_TIMEOUT` | 504 | `error.gateway.timeout` |

> **새 에러 코드 추가**:
> - 라이브러리 공통 코드 → `CommonErrorCode` enum 항목 + `messages*.properties`(기본/`_ko`/`_en`) `messageKey`.
> - **소비 서비스 도메인 코드** → `ErrorCode` 인터페이스를 구현한 자체 enum(예: `SampleErrorCode`) + 자체 messages 번들(`spring.messages.basename` 에 추가). **라이브러리 수정 불필요.**

## 5. 추적/로깅 연계

- `traceId` 는 `TraceContext` 의 `X-Trace-Id` 로, 응답 본문과 로그가 동일 ID 로 묶인다.
- 오류는 동시에 구조화 로그로 기록된다: 4xx → `error.business`(WARN), 5xx → `error.server`(ERROR, 스택 포함).
  자세한 로그 스키마는 [LOG_FORMAT.md](LOG_FORMAT.md) 참고.
- `message`/`detail` 은 절대 하드코딩하지 않고 i18n 메시지 번들을 거친다.

## 6. (옵션) 성공 응답 자동 래핑

`mutuus.common.response-wrapper.enabled=true`(기본 OFF)면 컨트롤러가 **평범한 객체**를 반환해도
라이브러리가 `ApiResponse.ok(...)` 봉투로 자동 래핑한다 — 수동 `ApiResponse.ok()` 없이도 표준 봉투를 얻는다.

```java
@GetMapping("/wrap")
public Map<String,Object> wrap() { return Map.of("value", 42); }   // → {"code":"OK","data":{"value":42},...}
```

**손대지 않는 것**(이중 래핑·직렬화 파손 방지): 이미 `ApiResponse`/`ProblemDetail`, `String`/`byte[]`/`Resource`,
비(非)JSON 응답, 제외 경로(`exclude-path-prefixes` 기본 `/actuator`·`/v3/api-docs`·`/swagger-ui`).
자동 래핑을 켜면 raw 배열을 반환하는 자체 인프라 엔드포인트가 있을 수 있으니(예: 목록 API)
필요 시 그 경로를 `exclude-path-prefixes` 에 추가한다. 회귀 가드 `ResponseWrapperIntegrationTest`.

---

회귀 가드: `samples/sample-api/CommonPlatformIntegrationTest`(봉투/상태코드/i18n/`fieldErrors`).
