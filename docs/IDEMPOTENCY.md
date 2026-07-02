# 멱등성 (Idempotency-Key)

`common-platform` 의 멱등성 기능 — **동작 메커니즘 + 키 생성 책임 + 오해와 올바른 사용 + 서버 발급 토큰 강화 패턴**을 한 곳에 정리한다.

패키지: `ai.mutuus.common.idempotency` · 기본 **OFF(opt-in)** · 관련 아키텍처 요약은 [CLAUDE.md](../CLAUDE.md) "§10 멱등성".

---

## 1. 목적

같은 요청이 네트워크 재시도 등으로 **중복 도착**해도 서버 상태를 한 번만 바꾸고, 이후 중복은 **첫 응답을 그대로 재방(replay)** 하게 만드는 컨벤션이다. 클라이언트가 요청마다 `Idempotency-Key` 헤더를 붙이는 것이 전제다.

## 2. 구성 요소

| 요소 | 역할 |
|---|---|
| `IdempotencyFilter` | 요청을 가로채 재방/신규 처리를 판단하는 `OncePerRequestFilter` |
| `IdempotencyStore` | 키 저장소 추상화 (`reserve`/`find`/`complete`) |
| `InMemoryIdempotencyStore` | 기본 인메모리 구현(TTL 관리, 단일 인스턴스용) |
| `IdempotencyRecord` | 저장 레코드 — 처리 중 마커(`inProgress`) 또는 완료 응답 스냅샷(`completed`) |
| `CommonIdempotencyAutoConfiguration` | opt-in 자동구성 + 필터 등록 |

## 3. 필터 체인 내 위치

`HIGHEST_PRECEDENCE + 15` 에 등록된다:

```
TraceFilter(HP) → AccessLogFilter(HP+10) → IdempotencyFilter(HP+15) → PayloadLoggingFilter(HP+20) → … → 컨트롤러
```

액세스 로그 뒤(요청은 로깅됨), 본문 로깅 앞이라 **재방 시 컨트롤러까지 가지 않고 필터에서 즉시 응답**을 돌려준다.

## 4. 동작 흐름 (`IdempotencyFilter#doFilterInternal`)

**0) 개입 조건** — 요청 메서드가 대상 목록(기본 `POST/PUT/PATCH`)에 있고 **`Idempotency-Key` 헤더가 있을 때만** 개입. 아니면 통과(GET·헤더 없는 요청은 무영향).

**1) 기존 레코드 조회** — `store.find(key)`:
- **완료된 레코드** → `replay()`: 저장한 상태코드·Content-Type·본문 그대로 + `Idempotent-Replayed: true` 헤더 (재처리 없음).
- **처리 중(in-progress) 마커** → `conflict()`: **409** + `Idempotent-Replayed: in-progress` (동시 중복).

**2) 신규 요청이면 예약** — `store.reserve(key, ttl)` 로 in-progress 마커를 **원자적으로** 등록. 그 찰나 다른 요청이 먼저 예약했으면(false) 다시 `find` 해 재방/409 로 분기(경쟁 안전).

**3) 실제 처리 + 응답 캡처** — 응답을 `ContentCachingResponseWrapper` 로 감싸 컨트롤러까지 진행 후 `finally`:
```java
store.complete(key, IdempotencyRecord.completed(status, contentType, body), ttl); // 첫 응답 저장
cached.copyBodyToResponse();                                                       // 실제 응답으로 흘려보냄
```
→ 이후 같은 키 요청은 1) 단계에서 이 저장분을 재방한다.

### 상태 전이
```
(없음) --reserve--> in-progress --complete--> completed
   ▲                    │ 같은 키 동시 요청        │ 같은 키 이후 요청
   │                    ▼                          ▼
 TTL 만료             409 반환                   첫 응답 재방
```

## 5. InMemoryIdempotencyStore 구성

```java
private final ConcurrentHashMap<String, Entry> map;      // 키 → (record, expiresAtMillis)
```
- `reserve` : `putIfAbsent` 로 마커 등록. 기존이 **유효**하면 false(중복), **만료**면 `replace` 로 원자 교체 후 true.
- `find` : `get` 후 만료면 `remove(key, e)` 로 지연 청소하고 null.
- `complete` : 완료 응답으로 `put`(마커 덮어쓰기) + 새 만료시각.

**특성·한계**: TTL은 **접근 시점에만 정리(lazy)** · **단일 인스턴스 전용**(JVM 힙, 인스턴스 간 공유 안 됨) · 최대 엔트리 수 제한 없음(TTL이 유일 방어선).

## 6. 저장소 추상화 — 분산(다중 인스턴스) 대응

`InMemoryIdempotencyStore` 는 인스턴스 간 공유되지 않는다. 다중 인스턴스에서는 소비 서비스가 **Redis 등 공유 저장소로 `IdempotencyStore` 를 구현한 빈**을 등록하면 `@ConditionalOnMissingBean` 으로 기본 구현을 대체한다(필터/흐름은 그대로). 원자 예약은 Redis `SET key val NX PX ttl` 로 구현하면 된다.

## 7. 설정 (기본 OFF)

```yaml
mutuus.common.idempotency:
  enabled: true            # 기본 false — 켜야 필터 등록
  header-name: Idempotency-Key
  ttl: 24h                 # 이 기간 내 같은 키는 첫 응답 재방
  methods: [POST, PUT, PATCH]
```

---

## 8. 키는 누가 만드나 — **클라이언트가 생성한다 (발급 API 아님)**

> 흔한 오해: "멱등성 키를 API로 발급받나?" → **아니오.** 별도 "키 발급 API"는 없다.

- 키는 **호출하는 클라이언트**(웹/앱/다른 MSA)가 요청 직전에 **스스로 생성**해 헤더에 싣는다.
- 서버(이 라이브러리)는 **받은 키로 중복을 걸러 재방**해 줄 뿐, 키를 만들어 주지 않는다.

```
POST /orders
Idempotency-Key: 3f9a1c2e-…   ← 클라이언트가 만든 UUID
```

### 왜 클라이언트인가 — "응답 유실 후 재시도"가 본질
```
클라이언트 ──POST──▶ 서버   … 서버는 처리 완료
클라이언트 ◀──✕(응답 유실/타임아웃)── 서버   … 클라이언트는 성공 여부 모름 → 재시도
```
재시도 시 **처음 만든 키를 그대로** 다시 보낸다 → 서버가 "이미 처리함"으로 인식 → 1회만 반영.

### 클라이언트별 생성 방법
| 호출 주체 | 키 생성 |
|---|---|
| 웹 프론트(JS) | `crypto.randomUUID()` |
| Android/iOS | `UUID.randomUUID()` / `UUID().uuidString` |
| 서버→서버(MSA) | `java.util.UUID.randomUUID()` |
| API 테스트 | 직접 값 / Postman `{{$guid}}` |

---

## 9. "멱등성이 더블클릭을 못 막는 것 아니냐?" — 키의 **스코프**가 관건

핵심: **키를 "물리적 클릭"이 아니라 "논리적 작업(의도)"에 묶어야** 한다.

### ❌ 함정 — 클릭마다 새 키 (더블클릭 못 막음)
```javascript
button.onclick = () => {
  const key = crypto.randomUUID();   // 클릭마다 새 키 → 더블클릭 = 키 2개 = 결제 2건
  fetch('/api/pay', { headers: { 'Idempotency-Key': key }, ... });
};
```

### ✅ 올바름 — 작업 단위로 만들고 재사용 (더블클릭·재시도 모두 차단)
```javascript
const paymentKey = crypto.randomUUID();      // 결제 "1건"이 시작될 때 한 번만
button.onclick = () => {
  fetch('/api/pay', { headers: { 'Idempotency-Key': paymentKey }, ... }); // 항상 같은 키
};
```

### 멱등성의 가치는 더블클릭"만"이 아님 — 클라이언트가 못 막는 것까지
| 중복 원인 | 프론트 버튼 disable | 멱등성 |
|---|:---:|:---:|
| 유저 더블클릭 | ⭕ | ⭕ |
| 응답 유실 후 자동 재시도(타임아웃) | ❌ | ⭕ |
| 게이트웨이/프록시 자동 재전송 | ❌ | ⭕ |
| 두 탭에서 같은 제출 | ❌ | ⭕(같은 키 공유 시) |
| 모바일 네트워크 끊김 재전송 | ❌ | ⭕ |

→ 버튼 disable 은 더블클릭만 막는 1차 방어, 멱등성은 **네트워크/인프라 레벨 중복까지 막는 서버 측 안전망**. 대체가 아니라 **계층 방어**.

---

## 10. 강화 패턴 — 서버 발급 토큰 (Synchronizer Token)

키 생성 책임을 **서버로 옮겨** "클릭마다 새 키" 실수와 임의 키 남용을 원천 차단한다. 결제 화면 진입 시 서버가 "이번 작업 전용 토큰"을 발급해 화면에 심고, 제출 시 그 토큰을 그대로 되돌려 받는다(클라이언트는 **받아서 돌려주기만**).

```
GET /checkout            → 서버: opToken=UUID 발급 → 화면에 심음(hidden/JSON)
POST /orders             → Idempotency-Key: opToken
   ├─ 1차: 처리 + 토큰 소진(completed)
   └─ 더블클릭/재제출: 같은 opToken → 재방 또는 409
```

### 강도 2단계
**① mint-only (간단)** — 서버는 UUID를 발급만, 중복 차단은 제출 시 멱등 필터가 담당.
→ **현재 라이브러리로 발급 엔드포인트만 추가하면 즉시 동작**.
```java
@GetMapping("/checkout/token")
public Map<String,String> issue() {
    return Map.of("idempotencyKey", UUID.randomUUID().toString()); // 발급만
}
```

**② whitelist (강함) — 발급분만 인정 + 1회용** — 발급 시 토큰을 `issued` 상태로 등록, 제출 시 "발급된 적 없는 토큰이면 400". 임의 키 주입·리플레이 남용 차단.
```
발급:  store.issue(token, userId, ttl)   // 'issued'
제출:  없음        → 400 (서버가 안 준 토큰)
       'issued'    → 처리 → 'completed'
       in-progress → 409
       completed   → 첫 응답 재방
```

### 클라이언트 생성 vs 서버 발급
| | 클라이언트 UUID | 서버 발급 토큰 |
|---|---|---|
| "클릭마다 새 키" 실수 | 발생 가능 → 뚫림 | 구조적으로 불가(받아서 돌려줌) |
| 임의 키 남용 | 그대로 수용 | 발급분만 인정 가능 |
| 단일 사용/만료/사용자 바인딩 | 어려움 | 발급 시 서버가 통제 |

### 이 라이브러리에 얹을 때(설계 주의)
현재 `IdempotencyStore` 상태는 **2개**(`in-progress`/`completed`)다.
- **① mint-only** → 지금 코드로 **추가 없이** 가능(발급 엔드포인트만).
- **② whitelist** → "발급됨(issued)" **3번째 상태**가 필요. 지금 필터는 "find로 뭔가 있으면 in-progress → 409"라, 발급 시점에 미리 reserve하면 **정상 첫 제출이 409로 오판**된다. 따라서 `IdempotencyStore` 에 `issue()`/`issued` 상태를 추가하거나 **별도 토큰 레지스트리**가 필요하다.

---

## 11. 계층 방어 (함께 쓰는 다른 수단)

| 계층 | 역할 |
|---|---|
| 프론트 **버튼 disable** | 즉각적 UX, 더블클릭 1차 차단 |
| **PRG**(Post-Redirect-Get) | 새로고침 재제출 방지(브라우저 폼) |
| **멱등성 키**(이 기능) | 네트워크/인프라 레벨 중복 차단(서버 안전망) |
| **서버 발급 토큰** | 키 자체의 신뢰(오·남용 원천 차단) |
| **DB 유니크 제약** | 최후 방어 — 앱이 뚫려도 DB에서 중복 거부 |

대체가 아니라 겹겹으로 쌓는다.

## 12. 회귀 가드 / 데모

- 단위: `InMemoryIdempotencyStoreTest`(reserve/find/complete·만료 재예약)
- 통합: `samples/sample-api/IdempotencyIntegrationTest`(RANDOM_PORT — 같은 키→재방 / 다른 키→새 응답 / 키 없음→미적용)
- 데모: `GET /demo/idem` — 같은 키로 `/demo/idem/echo`(호출마다 새 UUID)를 두 번 호출 → 2차는 재처리 없이 1차 값 + `Idempotent-Replayed: true`. 데모 카탈로그 [DEMO_CASES.md](DEMO_CASES.md) `idem`.

> 요약: `Idempotency-Key`(대상 메서드) 요청에 한해 필터가 "예약→처리→응답 캡처/저장"하고, 같은 키 재요청은 저장된 첫 응답을 컨트롤러 없이 재방(처리 중이면 409)한다. **키는 클라이언트가 "작업 1건" 단위로 생성·재사용**해야 하며, 더 강하게 묶으려면 **서버 발급 토큰** 패턴을 얹는다.
