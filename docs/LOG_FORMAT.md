# 공통 로그 포맷 (육하원칙 / 5W1H)

common-platform 라이브러리는 API 생명주기 전반을 **구조화 JSON 로그**로 자동 기록한다.
모든 로그는 logstash 인코더(`logback-common.xml`)로 JSON 렌더되며, 아래 **육하원칙(5W1H)**
필드 스키마를 따른다. 익셉션은 전용 필드와 스택트레이스로 수용된다.

## 1. 육하원칙(5W1H) 필드 매핑

| 원칙 | 필드 | 출처 | 비고 |
|------|------|------|------|
| **When (언제)** | `@timestamp` | logback | 모든 이벤트 자동 |
| **Who (누가)** | `userId` | 인증 주체(JWT subject) 우선, 없으면 `X-User-Id` 헤더 | `AuthenticatedUserContextFilter`가 검증값으로 갱신 |
| **Where (어디서)** | `service` | `${SERVICE_NAME}` custom field | 서비스(앱) 식별 |
| | `clientIp` | `X-Forwarded-For` 첫 홉 / remote addr | 호출 출처 IP |
| | `deviceLevel`, `deviceId` | `X-Device-Level`, `X-Device-Id` | **어느 시스템/단말**에서 접근했는지 |
| **What (무엇을)** | `event` | 라이브러리 | 이벤트 종류(아래 표) |
| | `httpMethod`, `httpPath` | 요청 | 대상 리소스 |
| | `errorCode` | `ErrorCode` | 오류 분류 코드 |
| **Why (왜)** | `httpStatus` | 응답 | 결과 상태 |
| | `reason` / `detail` | 예외/보안 | 실패 사유 메시지 |
| **How (어떻게)** | `httpMethod`, `httpQuery` | 요청 | 호출 방식 |
| | `durationMs`, `slow` | 필터 | 처리 시간/지연 여부 |
| | `exception`, `stack_trace` | 예외 | 예외 클래스 + 스택(서버 오류) |

> 추적 상관관계 필드 `X-Trace-Id`/`X-Span-Id`/`X-Screen-Id`/`X-Event-Id`는 MDC를 통해
> **모든 로그 라인에 자동 포함**된다(`TraceFilter`가 적재). 별도 이벤트 필드가 아니다.

## 2. 이벤트(event) 종류

| event | 시점 | 레벨 | 핵심 필드 |
|-------|------|------|----------|
| `request.received` | 요청 수신(핸들러 진입 전) | INFO | httpMethod, httpPath, httpQuery, clientIp, deviceLevel, deviceId, userId |
| `request.completed` | 응답 완료 | INFO (5xx·느림은 WARN) | httpStatus, durationMs, slow, userId |
| `auth.failure` | 인증 실패(401) | WARN | httpPath, reason |
| `auth.denied` | 인가 거부(403) | WARN | httpPath, reason, userId |
| `error.business` | 비즈니스/검증/요청 오류(4xx) | WARN | errorCode, httpPath, detail |
| `error.server` | 처리되지 않은 서버 오류(5xx) | ERROR | httpPath, exception, stack_trace |

모든 지점은 라이브러리가 지정한 컴포넌트를 거치며 자동 기록된다 — 소비 서비스 코드 변경 불필요.
(`AccessLogFilter`, `LoggingAuthenticationEntryPoint`/`LoggingAccessDeniedHandler`, `GlobalExceptionHandler`)

## 3. 익셉션 수용 규칙 (item 7)

- **서버 오류(`error.server`)**: `exception`(예외 클래스 FQN) + `stack_trace`(throwable, logstash가 직렬화)를 포함한다.
  API 응답 본문(`ApiResponse.error.exception`)에도 예외 클래스명을 담아 진단을 돕는다(노출 정책에 따라 조정).
- **비즈니스/검증 오류(`error.business`)**: 스택트레이스 없이 `errorCode` + `detail`(+ 응답의 `fieldErrors`)만 기록한다(정상 흐름의 예상된 예외).

## 4. 예시

요청 수신:
```json
{ "@timestamp":"2026-06-29T16:00:00.123+09:00", "level":"INFO", "service":"order-api",
  "X-Trace-Id":"ab12cd34", "event":"request.received", "httpMethod":"POST",
  "httpPath":"/api/orders", "clientIp":"10.0.0.7", "deviceLevel":"WEB", "userId":"u-1001" }
```
서버 오류:
```json
{ "@timestamp":"...", "level":"ERROR", "service":"order-api", "X-Trace-Id":"ab12cd34",
  "event":"error.server", "httpPath":"/api/orders", "exception":"java.lang.IllegalStateException",
  "stack_trace":"java.lang.IllegalStateException: ...\n\tat ..." }
```

## 5. 활성화

소비 서비스의 `logback-spring.xml`에서 공통 설정을 include 하면 위 포맷이 적용된다.
```xml
<configuration>
  <include resource="logback-common.xml"/>
</configuration>
```
로깅 토글/튜닝은 `mutuus.common.logging.*` (`enabled`, `exclude-path-prefixes`, `slow-request-threshold-millis` 등).
