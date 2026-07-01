# 컨트롤러 입출력 로깅 (payload-logging)

외부에서 데이터가 유입되면 **컨트롤러 바깥에서 모든 요청을 일괄로 감싸**,
**진입 시 입력(요청 본문)** 과 **종료 시 리턴(응답 본문)** 을 각각 출력하는 선택적(opt-in) 기능이다.
"입력 출력 함수"와 "리턴 출력 함수"를 **각각 별도 클래스로 분리**해 독립적으로 갱신/교체할 수 있게 했다.

> 패키지: `ai.mutuus.common.payload` · 로거: `ai.mutuus.common.payload` · 기본값: **OFF**(본문엔 PII·비용 우려)

---

## 1. 한눈에 — 필터 체인 어디에 있나

요청은 컨트롤러에 닿기 전에 필터들을 **양파 껍질처럼** 통과한다(바깥→안). 응답은 역순으로 빠져나간다.

```
        ┌─────────────────────────────────────────────────────────────┐
 인입 →  │ TraceFilter            (order = HIGHEST_PRECEDENCE)          │  추적ID/appCode 적재
        │ ┌─────────────────────────────────────────────────────────┐ │
        │ │ AccessLogFilter        (+10)                              │ │  request.received / completed
        │ │ ┌─────────────────────────────────────────────────────┐ │ │
        │ │ │ PayloadLoggingFilter (+20)  ◀── 이 기능              │ │ │  request.payload / response.payload
        │ │ │ ┌─────────────────────────────────────────────────┐ │ │ │
        │ │ │ │  Security 필터들 → DispatcherServlet → Controller │ │ │ │  @RestController.audit(...) 등
        │ │ │ └─────────────────────────────────────────────────┘ │ │ │
        │ │ └─────────────────────────────────────────────────────┘ │ │
        │ └─────────────────────────────────────────────────────────┘ │
        └─────────────────────────────────────────────────────────────┘ → 응답
```

- 숫자가 **클수록 안쪽**(컨트롤러에 가깝다). PayloadLoggingFilter(+20)는 컨트롤러 바로 바깥이라
  **컨트롤러가 받는 본문 그대로**를 로깅한다.
- `/actuator` 등 제외 경로와 본문 없는 요청은 건너뛴다.

---

## 2. 클래스 구성 (역할 분리)

```text
[자동구성]  CommonPayloadLoggingAutoConfiguration        (기본 OFF)
     │  등록 / 기본빈 제공(@ConditionalOnMissingBean) / 설정 바인딩
     ▼
[필터]     PayloadLoggingFilter
           오케스트레이터: 진입 → 체인 → 종료(try/finally)
     │
     ├── 진입 시 호출 ──▶ RequestPayloadLogger    «입력 출력 함수 (interface)»
     │                       └ DefaultRequestPayloadLogger    event=request.payload
     │
     ├── 종료 시 호출 ──▶ ResponsePayloadLogger   «리턴 출력 함수 (interface)»
     │                       └ DefaultResponsePayloadLogger   event=response.payload
     │
     └── 요청 래핑  ────▶ CachedBodyHttpServletRequest   (요청 본문 반복읽기)

[설정] PayloadLoggingProperties : enabled / maxBodyBytes / includeContentTypes / excludePathPrefixes
[확장] 소비자가 Request/ResponsePayloadLogger 빈을 정의하면 자동 대체
```

| 클래스 | 역할 | 지속 업데이트 포인트 |
|--------|------|----------------------|
| `PayloadLoggingFilter` | 진입→체인→종료를 `try/finally`로 감쌈 | 순서/래핑만 — 거의 불변 |
| **`RequestPayloadLogger`** (인터페이스) | **입력 파라미터 출력 함수** | 빈 재정의로 형식 교체 |
| `DefaultRequestPayloadLogger` | 그 기본 구현(JSON) | 필드/형식 직접 수정 |
| **`ResponsePayloadLogger`** (인터페이스) | **리턴값 출력 함수** | 빈 재정의로 형식 교체 |
| `DefaultResponsePayloadLogger` | 그 기본 구현(JSON) | 필드/형식 직접 수정 |
| `CachedBodyHttpServletRequest` | 요청 본문을 캐싱해 진입 로깅+컨트롤러가 둘 다 읽게 | — |
| `PayloadLoggingProperties` | 토글·크기·타입·제외경로 | 운영 튜닝 |
| `CommonPayloadLoggingAutoConfiguration` | 위를 자동 등록(웹 한정, `enabled=true`만) | — |

> 핵심: **입력(Request) / 리턴(Response) 출력이 서로 다른 클래스**라, 한쪽 형식을 바꿔도 다른 쪽에 영향이 없다.

---

## 3. 요청 한 건의 처리 흐름

```text
클라이언트
   │  HTTP 요청(본문)
   ▼
PayloadLoggingFilter   ── 요청 본문 캐싱(CachedBody) · 응답 래핑(ContentCaching)
   │
   │  ① 진입   : RequestPayloadLogger.onRequest(입력본문)  ─▶ 로그 event=request.payload
   │  ② 체인   : filterChain.doFilter → Security → Controller
   │  ③ 종료   : (finally)
   │              ResponsePayloadLogger.onResponse(리턴본문) ─▶ 로그 event=response.payload
   │              copyBodyToResponse()  (캐싱 본문을 실제 응답으로 복원)
   ▼
클라이언트  ◀── 응답
```

1. **진입**: 요청 본문을 캐싱(컨트롤러도 다시 읽을 수 있게) → `RequestPayloadLogger`가 입력 출력
2. **체인 실행**: 컨트롤러까지 진행, 응답 본문은 래퍼에 캐싱
3. **종료(finally)**: `ResponsePayloadLogger`가 리턴 출력 → 캐싱 본문을 실제 응답으로 복원

---

## 4. 켜기 / 확장

**켜기** (`application.yml`, 기본 OFF):
```yaml
mutuus:
  common:
    payload-logging:
      enabled: true
      max-body-bytes: 2048          # 초과분 절단
      include-content-types:        # 이 타입만 본문 로깅(바이너리 회피)
        - application/json
        - text/
      exclude-path-prefixes:
        - /actuator
```

**형식 갱신(지속 업데이트) — 두 가지 방법**
1. 기본 구현(`DefaultRequest/ResponsePayloadLogger`)을 직접 수정
2. 소비 서비스가 자체 빈을 정의 → `@ConditionalOnMissingBean`으로 자동 대체
   ```java
   @Bean
   ResponsePayloadLogger maskedResponseLogger() {
       return (req, status, body) -> { /* 마스킹/요약 후 출력 */ };
   }
   ```

---

## 5. 로그 예시 (JSON 한 줄, 로그 뷰어에서 바로 보임)
```json
{"event":"request.payload","httpMethod":"POST","httpPath":"/demo/audit",
 "contentType":"application/json","requestBody":"{\"text\":\"hi\"}",
 "X-Trace-Id":"...","appCode":"SMPL","instanceCode":"4V0F2G","service":"sample-api"}

{"event":"response.payload","httpMethod":"POST","httpPath":"/demo/audit",
 "httpStatus":200,"responseBody":"{\"code\":\"OK\",...}",
 "X-Trace-Id":"...","appCode":"SMPL","instanceCode":"4V0F2G"}
```
같은 `X-Trace-Id`로 입력↔리턴이 한 트랜잭션으로 묶인다.

---

## 6. 한계 / 다음 단계
- 필터는 **HTTP 본문(직렬화된 바이트)** 까지만 본다. **컨트롤러 메서드의 실제 인자/리턴 객체**
  (역직렬화된 DTO, `@PathVariable`, `@RequestParam` 분해값)는 못 본다.
- 그것까지 필요하면 **`HandlerInterceptor` 또는 AOP `@Around`** 계층이 별도로 필요하다(예정).
