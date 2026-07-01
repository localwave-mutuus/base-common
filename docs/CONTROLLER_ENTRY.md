# 컨트롤러 메서드 진입 인터셉트 (controller-entry)

**명칭 / 패키지 / URL** 로 지정한 컨트롤러 메서드의 **진입 시점**에 동작(기본=로깅)을 수행하는 선택적(opt-in) 기능이다.
"대상 판별(매처)"과 "진입 동작(핸들러)"을 **각각 별도 클래스로 분리**해 독립적으로 갱신/교체할 수 있게 했다.

> 패키지: `ai.mutuus.common.intercept` · 로거: `ai.mutuus.common.controller` · 기본값: **OFF**
> Spring MVC `HandlerInterceptor` 기반(추가 의존성 없음).

---

## 0. 온보딩 핵심 한 줄
> "특정 컨트롤러 메서드에 **진입할 때** 뭔가를 하고 싶다 → 이 기능. **무엇을 대상으로(매처)** 와 **무엇을 할지(핸들러)** 를 따로따로 바꾼다."

---

## 1. 어디서 동작하나 — 필터 vs 인터셉터

요청은 **서블릿 필터**들을 통과한 뒤 `DispatcherServlet` 안으로 들어오고, 그 안에서 **HandlerInterceptor** 가
컨트롤러 메서드를 감싼다. 이 기능은 **인터셉터** 계층이다(필터보다 안쪽, 컨트롤러에 가장 가깝다).

```
  서블릿 필터 영역(요청 전체)                          DispatcherServlet 내부(핸들러)
 ┌───────────────────────────────────┐   ┌──────────────────────────────────────────┐
 │ TraceFilter → AccessLogFilter →    │ → │ HandlerMapping 으로 컨트롤러 메서드 결정    │
 │ PayloadLoggingFilter               │   │   │                                       │
 └───────────────────────────────────┘   │   ▼                                       │
                                          │ ControllerEntryInterceptor.preHandle ◀── 이 기능 │
                                          │   │ (매처 통과 시 핸들러 실행)               │
                                          │   ▼                                       │
                                          │ (인자 역직렬화) → @RestController.audit(...) │
                                          │   │                                       │
                                          │   ▼ postHandle → afterCompletion          │
                                          └──────────────────────────────────────────┘
```

| | 필터(PayloadLoggingFilter) | 인터셉터(ControllerEntryInterceptor) |
|---|---|---|
| 계층 | 서블릿(컨테이너) | Spring MVC(DispatcherServlet 내부) |
| 시점 | 요청 전체의 가장 바깥 | 컨트롤러 메서드 직전(preHandle) |
| 볼 수 있는 것 | HTTP 본문(직렬화 바이트) | **컨트롤러 메서드**(클래스/메서드명/패키지) + URL |
| 못 보는 것 | 어떤 메서드인지 | 역직렬화된 메서드 인자(인자 분해 전) |
| 타게팅 | 경로(prefix) | **명칭/패키지/URL** |

---

## 2. 클래스 구성 (역할 분리)

```text
[자동구성]  CommonControllerEntryAutoConfiguration        (기본 OFF)
     │  WebMvcConfigurer 로 인터셉터 등록 / 기본빈 제공 / 설정 바인딩
     ▼
[인터셉터] ControllerEntryInterceptor
           preHandle: 컨트롤러 메서드 실행 직전
     │
     ├── 대상 판별 ──▶ ControllerMethodMatcher   «명칭/패키지/URL 판별 (interface)»
     │                     └ DefaultControllerMethodMatcher
     │
     └── 진입 동작 ──▶ ControllerEntryHandler    «진입 시 수행 (interface)»
                           └ DefaultControllerEntryHandler   event=controller.entry

[설정] ControllerEntryProperties : enabled / packages / methodNames / urlPatterns
[확장] 소비자가 Matcher/Handler 빈을 정의하면 자동 대체
```

| 클래스 | 역할 | 지속 업데이트 포인트 |
|--------|------|----------------------|
| `ControllerEntryInterceptor` | preHandle에서 매처→핸들러 연결 | 거의 불변 |
| **`ControllerMethodMatcher`** (인터페이스) | **대상 판별(명칭/패키지/URL)** | 빈 재정의로 정책 교체 |
| `DefaultControllerMethodMatcher` | 그 기본 구현 | 규칙 평가 로직 수정 |
| **`ControllerEntryHandler`** (인터페이스) | **진입 시 수행 동작** | 빈 재정의로 동작 교체 |
| `DefaultControllerEntryHandler` | 그 기본 구현(로깅) | 출력/동작 수정 |
| `ControllerEntryProperties` | 토글·타게팅 규칙 | 운영 튜닝 |
| `CommonControllerEntryAutoConfiguration` | 위를 자동 등록(웹 한정, `enabled=true`만) | — |

> 핵심: **"무엇을 대상으로(매처)"** 와 **"무엇을 할지(핸들러)"** 가 서로 다른 클래스라 한쪽만 바꿔도 다른 쪽에 영향이 없다.

---

## 3. 요청 한 건의 처리 흐름

```text
DispatcherServlet
   │  preHandle(handler = HandlerMethod)
   ▼
ControllerEntryInterceptor
   │  ControllerMethodMatcher.matches ?
   │     ├─ 대상 (명칭/패키지/URL 일치) ─▶ ControllerEntryHandler.onEntry ─▶ 로그 event=controller.entry
   │     └─ 비대상 ─▶ skip
   │  return true  (절대 막지 않음)
   ▼
Controller 메서드 실행  (인자 역직렬화 후)
```

---

## 4. 타게팅 규칙 (명칭 / 패키지 / URL)

```yaml
mutuus:
  common:
    controller-entry:
      enabled: true
      packages:      [ai.mutuus.sample.web]    # 컨트롤러 클래스 패키지 접두사
      method-names:  ["audit", "get*"]         # 메서드명(와일드카드 * 지원)
      url-patterns:  ["/demo/**", "/api/**"]   # URL Ant 패턴
```

**결합 규칙**: 지정한 축만 제약(빈 축은 무시), **축 간 AND · 축 내 OR**.

| 설정 | 의미 |
|------|------|
| packages만 | 그 패키지의 모든 컨트롤러 메서드 |
| url-patterns만 | 그 URL로 들어온 모든 메서드 |
| packages + method-names | 그 패키지 AND 그 메서드명 |
| (셋 다 빈값) | 모든 컨트롤러 진입 |

---

## 5. 확장 (지속 업데이트)
- **동작 교체**: 소비 서비스가 `ControllerEntryHandler` 빈을 정의 → `@ConditionalOnMissingBean`으로 기본(로깅) 대체.
  ```java
  @Bean
  ControllerEntryHandler auditEntryHandler(AuditService audit) {
      return (method, request) -> audit.record(method.getMethod().getName(), request.getRequestURI());
  }
  ```
- **판별 교체**: `ControllerMethodMatcher` 빈을 정의 → 어노테이션 기반 등 커스텀 타게팅으로 교체.

---

## 6. 로그 예시 (JSON 한 줄)
```json
{"event":"controller.entry","controller":"DemoCaseController","method":"audit",
 "package":"ai.mutuus.sample.demo","httpMethod":"POST","httpPath":"/demo/audit",
 "X-Trace-Id":"...","appCode":"SMPL","instanceCode":"4V0F2G","service":"sample-api"}
```

---

## 7. 한계 / 다음 단계
- `preHandle`은 **컨트롤러 메서드와 URL**까지 본다. **역직렬화된 인자/리턴 객체**(DTO 등)는 못 본다.
- 인자/리턴 객체까지 필요하면 **AOP `@Around`** 가 필요하다(추가 의존성 `spring-boot-starter-aop`). 동일하게
  "매처/동작 분리" 구조로 확장 가능 — 필요 시 별도 제공.
- 본문(payload) 로깅은 [PAYLOAD_LOGGING.md](PAYLOAD_LOGGING.md)(필터 기반) 참고. 둘은 상호 보완이다
  (본문=필터, 메서드 식별/진입=인터셉터).
