# G-COMMON 공통 추적·로그 계약 (C2-2)

상태: C2-2 보강안, A05 계약 검토 전·C2 전체 미완료. 정식 기준은 **0.6.1 / e64c545f4d5b**이며 아래 390c768·0.6.0 기술은 최초 설계 조사 이력이다. C1-1 판단 범위는 [바이트 대조표](../reports/g-common-c1-1-sha.md)를 따른다. 이번 보강은 기존 실측 재사용이며 성능 실행은 하지 않았다.

최초 설계 이력: REQ-261021-02 / A5 문서 범위. 기준 base-common `390c768`(0.5.0). 첨부 0528.a01 초안의 결정을 재사용하고 제공된 상위 필수 9항목에 대한 대응·누락만 보완한다. 아래 **현재**는 첨부 정적 조사 결과이며 **계약/제안**은 후속 구현 요구다. 구현·발행·배포·실측 완료를 뜻하지 않는다. 상위 `261021-0527-gcom-c2-contract`의 필수 9항목 전체는 이번 brief에 제공되었으므로 이전의 ‘상위 원문 미제공으로 번호별 대조 불가’ 선언을 철회한다. 상위 번호는 아래 대응표를 따른다. 이번 보강에서 성능 절차는 §7로 분리하고 기존 비동기 절은 §7A로 표기한다.

보조 조사표의 로컬 위치는 `/mnt/disk/00.workspaces/b04.mutuus-operation/teams/A13.공통모듈/build/c2-survey/{common,app,mem,batch}.md`다. 각 표의 고정 조사 리비전을 유지한다.

근거 표기: [common], [app], [mem], [batch]는 이번 입력 attach-2-common.md, attach-3-app.md, attach-4-mem.md, attach-5-batch.md다(초안의 조사표 첨부 번호를 정정). 인용 파일:줄은 조사표가 확인한 고정 리비전 기준이다. C=base-common `lib/src/main/java/ai/mutuus/common/`, R=base-common `lib/src/main/resources/common-platform/log-fields.v2.json`. BO=`apps/backend/`. APP/MEM/BATCH의 경로 약어는 각 첨부 정의를 따른다. 원본 소스 재조사 없이 제공 조사표를 사용했다. APP 조사 입력은 `84afd39fa8fa`이며 결과 사본 `348108f3`와 구별한다. MEM=`e6bb1017e81897d0658c8bbf45d0cd4a124d8cd3`, BATCH=`fdcbfb0`, BO=제공 식별자 `612202e080ee`다. 미제공 정식 SHA를 보충하지 않는다.

## 상위 필수 9항목 대응표

| 상위 번호 | 필수 요구 | 문서 절·결정 | 문서 대조 결과 |
|---|---|---|---|
| 1 | W3C·apply no-op 해소·실제 parent.id | §1 agent 단일 소유·설정 표준, SDK onStart lineage 수집; §2 자체 서버 예외 | 반영 |
| 2 | HTTP 완료 방식·401/403/404/검증/5xx/async/AdminHttpServer | §2 완료 1건, 오류 표·자체 서버 어댑터 | 반영 |
| 3 | 서비스 입력/출력 쌍·INTERNAL·선정·설정 | §3 annotation AND allowlist, INTERNAL enter/exit, 설정 키 | 반영 |
| 4 | 안전 payload·허용 필드 등록 API·8KiB/32KiB | §4 기본 deny·필드 schema 등록 API·bounded capture | 기존 초안의 projector 등록에 필드 등록 계약 보완 |
| 5 | v5 초안·v3/v4 차이·ui.event.id 의미 변경 | §5 비교표·신규 4필드·UI root span ID | 반영 |
| 6 | OpenSearch 필드·mapping 영향 | §6 매핑·채움·trace·요청짝 관측 및 C7 이관 | 관측 범위만 확정, C2 전체 미완료 |
| 7 | p95 지연 5% 이내 측정 방법 | §7 dev 전후 고정 조건·명령・p95 비교 | 반영; 측정 미실행 |
| 8 | 적용 순서·위험 상위 3개씩·깨지는 API·새 버전 | §8 순서·서비스별 위험표·깨지는 API/소비 계약 목록·0.6.0 | 초안의 위험 목록에 별도 API 목록 보완 |
| 9 | 배치 단건 메일 원래 요청 trace 단절 | §7A afterCommit 캡처→수신 envelope 영속→CONSUMER 부모 복원 | 반영 |

‘반영’은 문서 요구 충족이며 실행 검증이나 배포 승인이 아니다.

## 1. W3C·agent·실제 부모의 단일 소유권

**현재:** HTTP span 생성·전파는 Java agent 소유다. `TraceHeaders.apply`는 no-op이고 `outbound(true)`만 propagator inject 경계다. `CanonicalLogSnapshot.context()`는 trace/span/flags만 채우며 parent.id를 자동 충전하지 않는다. inbound 헤더의 span ID는 후보일 뿐 실제 span의 부모 증명이 아니다([common] `C/core/TraceHeaders.java:21-54`, `C/logging/CanonicalLogSnapshot.java:26-46`, `C/web/TraceFilter.java:19-66`).

**계약:** Java HTTP SERVER/CLIENT span은 원칙적으로 agent만 만든다. §2의 미계측 JDK 자체 서버 manual 모드만 명시적 예외다. 명시 주입을 공통 인터셉터에 추가하지 않는 이유는 agent와 중복 span/주입·서로 다른 부모 선택을 피하기 위해서다. `TraceHeaders.apply` 호출을 전파 보장으로 보지 않고 agent 지원 클라이언트 및 아래 설정을 C3 적용 전제조건으로 삼는다. 표준 `traceparent`/`tracestate`는 OTel W3C propagator로 검증·추출·주입한다. 잘못된 traceparent는 부모 없이 새 SERVER trace로 처리하고 사용자 X-Trace-Id/X-Span-Id/MDC를 대체 원천으로 쓰지 않는다.

agent 설정은 `OTEL_PROPAGATORS=tracecontext,baggage`, 서비스별 고정 `OTEL_SERVICE_NAME`, `OTEL_TRACES_SAMPLER=parentbased_traceidratio`, 비율 `OTEL_TRACES_SAMPLER_ARG`를 환경별 명시한다. baggage는 로그·신원 입력으로 수용하지 않고 별도 허용 정책 없이는 외부로 전파하지 않는다. agent 버전·계측 모듈은 C3 검증 artifact에 고정하며 현재 설치 버전을 추측하지 않는다.

새 `ai.mutuus.common.observability.SpanLineageSpanProcessor`는 SDK `onStart(Context parentContext, ReadWriteSpan span)`에서 **실제로 전달된** 유효 parent SpanContext를 읽어 `(trace.id, span.id) → parent.id`를 저장한다. root는 parent.id를 생략한다. 새 `SpanLineageAccessor`와 `SpanLineageSnapshot`을 `CanonicalLogSnapshot`에 연결한다. 원격 헤더/MDC/일반 KV로 parent.id를 보충하지 않는다. 종료 시 live entry를 제거하고 async 완료 로그는 시작 시 캡처한 불변 snapshot을 사용한다. agent extension의 `AutoConfigurationCustomizerProvider`로 processor를 설치한다. SDK 접근이 불가능하면 parent.id를 생략하고 진단 상태를 남기며 가짜 부모를 만들지 않는다. processor 연결은 C3 필수 검증 항목이다.

설정 키(신규): `mutuus.common.observability.lineage.enabled=true`, `mutuus.common.observability.lineage.max-active-spans=100000`. 한도 초과는 신규 캐시 보관을 생략하고 제한된 진단 카운터만 증가시킨다. parent.id/span.id는 0이 아닌 16자리 lowercase hex, trace.id는 0이 아닌 32자리다. 이슈 수집도 새 `TelemetryContextSnapshot.capture()`의 OTel 문맥을 사용하고 MDC 읽기에 의존하지 않는다.

내부 승인 목적지에는 W3C를 전파한다. 외부 공급자에는 기본 차단하고 승인 host만 허용한다. 신규 `mutuus.common.observability.propagation.allowed-hosts`는 정확한 hostname 목록(와일드카드 금지), `mutuus.common.observability.propagation.external-enabled=false`가 기본이다. 화면 힌트 host allowlist와 별개다. 신규 agent extension `TrustedDestinationPropagationCustomizer`가 최종 주입 경계에서 정책을 적용한다. 공통 인터셉터에서 주입 후 삭제하지 않는다. baggage도 이 경계에서 별도 승인 없이 외부 주입하지 않는다. 지원 불가능한 클라이언트 계측은 배포 승인 대상에서 제외한다.

## 2. HTTP 이벤트·오류·비동기와 자체 서버

**결정:** HTTP 정본은 요청당 완료 **한 건**이다. 서버 `http.server.complete`, 클라이언트 `http.client.completed`를 유지한다. HTTP 시작/종료 쌍을 신규 생성하지 않는다. 서비스·job 쌍과 혼동하지 않는다. 현재 필터가 이미 완료 한 건이며 과거 BO 쌍 테스트는 현재 pass 증거가 아니다([common] `C/logging/AccessLogFilter.java:35-132`, `C/logging/AccessLogger.java:23-80`, `BO/src/test/java/ai/mutuus/backoffice/shared/LoggingConfigurationTest.java:110-140`).

새 `HttpCompletionScope`가 시작 monotonic clock, UUID `golmok.request.id`, 실제 span snapshot을 보관하고 CAS로 마감한다. 필수 출력은 event.action/dataset/outcome/duration, trace.id/span.id(유효할 때), parent.id(실제 부모를 확보한 때), HTTP method/status, query 없는 route, error.code(실패)다. duration 단위는 ns. 미매칭 route는 `/{unmatched}`, 원시 URI/query를 메시지로 우회 출력하지 않는다.

| 종료 | 서버 계약 |
|---|---|
| 2xx/3xx | success, 실제 status |
| 400/401/403/404/405/409/413 및 기타 >=400 | failure, `HTTP_<status>`; 검증 오류도 실제 응답 status 기준, 인증 실패는 검증된 auth 상태만 |
| 미처리 예외 | 500, `HTTP_500`; 본문·예외 message 원문 제외 |
| 확인된 client disconnect | 합성 499, `CLIENT_DISCONNECTED` |
| 기타 IOException | 실제 응답 status가 유효하면 유지, 없으면 500/`HTTP_IO_ERROR`; 모든 IOException을 단절로 간주하지 않음 |
| Servlet async | onComplete/onTimeout/onError/onStartAsync 재등록, timeout=504, error=500, snapshot 복원 후 동일 scope 1회 마감 |
| 취소 | 확인된 취소 사유 코드, 실제 응답이 없으면 status 생략; 성공으로 만들지 않음 |

Access 필터는 CORS·Security·앱 오류 필터보다 바깥에 등록한다. Trace 메타데이터 수용은 Security 앞에 두되 신원은 인증 결과로만 설정한다. 필터 이전 container 거절·프로세스 강제 종료는 완료 보장 밖이며 container/인프라 지표로 탐지한다. actuator 제외는 명시 설정을 유지한다. [app] `B:java/devauth/internal/ProductionCorsConfig.java:43-45`의 동일 최상위 순위 충돌은 C3에서 순서를 명시해 해소한다.

클라이언트는 response/통신 오류/timeout/cancel마다 1회 완료한다. 4xx도 failure로 통일한다(현재 공통은 5xx/예외만 failure: [common] `C/web/HttpClientLoggingInterceptor.java:45-111`). retry는 실제 HTTP attempt마다 별도 CLIENT span/완료, 상위 서비스 호출은 한 쌍이다. RestClient/RestTemplate 외 JDK HttpClient sync/sendAsync도 신규 `ObservedJdkHttpClient` decorator로 완료 기록한다. agent가 span을 만들고 decorator는 span을 중복 생성하지 않는다. 콜백 종료까지 snapshot을 전달한다.

비웹 batch에는 MVC 조건 밖 신규 `CommonHttpClientObservationAutoConfiguration`을 둔다. `AdminHttpServer` 및 `EmailEventServer`는 Servlet 밖이므로 신규 `JdkHttpServerObservationHandler`로 인증·route 분기 전체를 감싼다. SERVER span이 agent로 존재하면 빌리고, 지원되지 않는 자체 서버에는 `mutuus.common.observability.jdk-server.span-owner=manual`을 명시하여 W3C extract→SERVER span→scope close를 어댑터가 수행한다. agent 소유 모드(`agent`)와 manual 모드는 상호 배타적이다. executor 제출 전에 관측 scope를 여는 수락 어댑터를 둔다. 교환 객체에 접근 가능한 자체 admission 경계의 rejection은 503/`HTTP_503`으로 완료하며 수락 후 handler가 1회 마감한다. JDK 서버 내부 executor rejection처럼 애플리케이션이 교환 객체를 받기 전의 거절은 HTTP 이벤트 보장 밖이고 rejection 지표로 기록한다. 현재 두 handler에는 완료 로거가 없다([batch] `R/AdminHttpServer.java:24-99`, `R/EmailEventServer.java:24-44`).

## 3. INTERNAL 서비스 함수 입출력 쌍

**현재:** Controller aspect는 RestController만 대상으로 정상 반환에서만 exit한다. 서비스 전체 쌍·BO 명시 활성화는 없다([common] `C/aop/ControllerMethodLoggingAspect.java:29-61`, `C/aop/CommonMethodLoggingAutoConfiguration.java:15-37`, BO/src/main 조사 범위).

**계약:** 신규 `ai.mutuus.common.aop.ServiceMethodLoggingAspect`, `ServiceMethodLoggingProperties`, `ServiceInvocationScope`, `ServiceMethodLoggingWriter`, annotation `@ObservedServiceOperation`을 도입한다. 설정 접두사는 모두 `mutuus.common.service-logging.`이며 `enabled=false`, `include-packages=[]`, `include-methods=[]`, `exclude-methods=[]`, `require-annotation=true`, `payload-enabled=false`다. 활성화·패키지·메서드·annotation 조건은 모두 AND, exclude가 우선한다. 내부 application service의 공개 업무 진입 메서드만 명시 지정한다. repository/entity/DTO/getter/setter/security/token/암복호화/대량 순회 유틸은 제외한다. Spring proxy의 self-invocation/private 메서드는 자동 보장 대상이 아니며 호출 구조 변경 또는 명시 scope API를 사용한다.

호출마다 실제 INTERNAL span을 하나 만들고 `service.method.enter`와 `service.method.exit` 두 이벤트를 같은 span.id 및 신규 `golmok.service.invocation.id`(UUID)로 연결한다. 부모는 진입 시 current span이다. code.namespace/function, exit의 event.duration(ns), event.outcome(success/failure/cancelled), 허용 error.code를 기록한다. 동기 반환·throw·cancel과 CompletionStage 완료에 모두 CAS exit 1회, span 종료 1회. 비동기는 호출 반환 시점에 exit하지 않는다. reactive Publisher는 이 버전 대상에서 제외하고 opt-in을 거부한다. 예외 stack/message나 DTO.toString 자동 출력 금지다. enter의 입력 및 exit의 출력은 §4 안전 projector가 승인한 값만 포함한다.

초기 적용 대상은 BO issue delivery 업무 service 경계, APP 메뉴 조정 service, MEM 인증 이메일 ingress 업무 service, BATCH RunEngine/DecisionEngine 업무 진입 메서드 중 C3 서비스 담당자가 고정 시그니처로 지정한 목록이다. 조사표의 클래스 수준 후보이며 미제공 메서드명을 사실로 확정하지 않는다. `JobPairLogger`와 서비스 쌍은 다른 dataset이고 같은 실행을 집계 시 중복 합산하지 않는다. 근거: [common] `BO/src/main/java/ai/mutuus/backoffice/support/internal/BackofficeIssueCollector.java:213-230`; [app] `B:java/merchantdiscovery/internal/application/MenuWorkerCoordinator.java:50-78`; [mem] `M/notification/internal/infrastructure/JdbcAuthenticationEmailIngress.java:73,101`; [batch] `R/RunEngine.java:31-87`, `R/DecisionEngine.java:24`.

## 4. 안전 payload·허용 API·크기 제한

**현재:** 공통 payload 기본 disabled, 본문 출력 상한 4096 bytes이고 ALL의 캐싱은 전체 메모리 상한이 아니다. method writer는 문자열화 후 선택적 masker를 적용한다. BO 명시 정책은 없다([common] `C/payload/PayloadLoggingProperties.java:12-68`, `C/payload/PayloadLoggingFilter.java:56-117`, `C/aop/DefaultMethodLoggingWriter.java:36-78`).

**계약:** 신규 `SafePayloadPolicy`, `SafePayloadProjector`, `PayloadPolicyRegistry`를 HTTP/서비스/이슈 전송에 공통 적용한다. 기본 deny, 정책 없거나 파서 실패·masker 부재·알 수 없는 필드는 본문 전체 생략(fail closed). JSON의 승인 scalar 필드만 DTO 전용 projector로 생성한다. 문자열화·reflection 전체 객체 순회·원문 fallback은 금지한다. 비밀번호/토큰/인증코드/session/cookie/Authorization/개인 연락처/이름/주소/메일 본문/결제·계좌·파일·binary/multipart는 성공·오류 모드 모두 차단한다. 메시지/stack/이슈 수집 경로도 같은 차단 경계에 포함하고 고정 오류 코드·예외 class만 허용한다.

허용 API는 신규 `PayloadPolicyRegistry.register(HttpPayloadRule(method, routeTemplate, direction, projectorId))`, `register(ServicePayloadRule(namespace, function, direction, projectorId))` 및 `SafePayloadProjector.project(ApprovedInput)`다. **필드 등록 보완:** 같은 registry에 `register(ProjectorSchema(projectorId, List<AllowedScalarField> fields))`를 둔다. `AllowedScalarField(name, scalarType, maxUtf8Bytes, allowedEnumValues)`는 출력 이름·scalar 타입·문자열 byte 상한·enum 허용값을 명시한다. 중첩 객체·배열·비밀/개인정보 필드 등록은 거부한다. projector 출력도 schema로 다시 검증하며 미등록 필드·타입 불일치가 하나라도 있으면 전체 payload를 생략한다. rule의 미등록 projectorId 참조·중복/충돌 등록은 시작 시 오류로 처리한다. 이는 신규 제안 API이며 현재 존재하는 API가 아니다.

임의 KV나 raw body 전달 API는 제공하지 않는다. 초기 HTTP 승인표는 **빈 목록**, 서비스 입력/출력도 **빈 목록**이다. BO를 포함한 각 서비스가 명시 method+route+방향+field schema를 코드 검토로 등록한 뒤에만 출력 가능하며 URL wildcard·오류 응답 전체 허용은 금지한다. 안전한 예는 enum 상태·건수·허용 오류 코드이고 업무 식별자는 별도 승인 없이 추가하지 않는다.

설정 접두사(신규)는 `mutuus.common.safe-payload.`이며 `enabled=false`, `mode=ERROR_ONLY`, `max-body-bytes=8192`, `max-event-bytes=32768`, `max-depth=4`, `max-fields=32`다. request/response 각각 캡처·파싱·출력 상한은 UTF-8 **8KiB**, 최종 JSON 로그 전체는 **32KiB**다. 두 byte 한도는 설정으로 낮출 수 있지만 상한 초과 설정은 거부한다. 기존 4KiB 설정을 자동 확대하지 않고 별도 명시 opt-in한다. 스트리밍 bounded tee를 사용하며 상한 초과 시 본문을 생략하고 `golmok.payload.omitted.reason=BODY_TOO_LARGE`를 출력한다. 원문 prefix 절단은 금지한다. 직렬화 뒤 32KiB를 초과하면 payload→선택 메타데이터 순으로 제거하고 필수 envelope만 재직렬화한다. 필수 envelope의 문자열도 bounded writer로 제한하여 전체 상한을 지킨다. 잘못된 JSON 절단은 금지한다. 신규 `golmok.payload.policy.id`, `golmok.payload.omitted.reason`, `golmok.payload.truncated`는 v5 보호 필드다. header는 content-type 등 승인 비민감 allowlist만, query는 기본 미기록이다.

## 5. 레지스트리 v3/v4 비교·v5·UI 이벤트

**현재 확인 비교:** v3 `0110d5767` 및 0.4.1 `af7bb9d22`의 R과 v4 `390c768`을 비교한 [common]을 정본 근거로 사용한다. v3=89개, v4=90개다. 파일명 v2, registry version 4, 출력 golmok.schema.version=2는 별개 축이다.

| v3→v4 | 변경과 근거 |
|---|---|
| 추가 | golmok.ui.element.id, golmok.ui.action (`R:1684-1685`) |
| 제거 | golmok.screen.internal_id (`af7bb9d22:R:762-779`) |
| 의미 변경 | golmok.screen.id: 자유 client 보고에서 버전 일치 정본 코드·유일 RESOLVED 별칭으로 (`af7bb9d22:R:744-747`→`R:744-747`) |
| 의미 변경 | golmok.ui.event.id: 임의 UI ID에서 실제 UI action root span ID로 (`af7bb9d22:R:1000-1003`→`R:982-985`) |

**v5 제안:** R 내부 version을 5로 올리고 출력 schema version=2는 유지한다. v4 필드 이름·타입·위 의미를 유지하며 아래 4필드를 추가한다. 모두 protected=true/mdcInput=false, 공통 typed writer만 작성한다.

| 신규 필드 | 타입·값 계약 |
|---|---|
| golmok.service.invocation.id | keyword, 호출별 UUID |
| golmok.payload.policy.id | keyword, 등록 projector/schema 정책 식별자 |
| golmok.payload.omitted.reason | keyword, 고정 코드: POLICY_DENIED / PARSE_ERROR / MASKER_UNAVAILABLE / SCHEMA_MISMATCH / BODY_TOO_LARGE / EVENT_TOO_LARGE |
| golmok.payload.truncated | boolean, 승인 payload/선택 메타데이터를 크기 제한으로 제거했으면 true; 원문 절단을 뜻하지 않음 |

parent.id는 기존 keyword를 재사용하고 출처를 실제 span parent로 강화한다. 추가 필드 수가 4개로 확정될 경우 총 94개이며 C3 실제 registry 산출 시 계산으로 확인한다. payload 출력은 기존 정본 필드 허용 여부를 C3에서 확인하고 없으면 raw 신규 필드를 임의 추가하지 말고 별도 schema 결정 전 출력을 차단한다.

UI 계약: 모바일 action root span의 nonzero lowercase 16hex span.id만 `X-Event-Id`→golmok.ui.event.id로 전달한다. trace.id·CLIENT span.id·HTTP event.id·시간/random ID와 다르다. 하나의 UI 동작에서 발생한 여러 요청은 같은 UI root ID, 서로 다른 CLIENT span ID를 사용한다. override는 실제 root ID 일치가 확인될 때만 허용한다. 화면은 APP/MEM/BO system별 catalog resolver를 명시하고 catalog version과 screen.id가 함께 유효할 때만 UI metadata를 수용한다. 잘못된 힌트는 요청을 거절하지 않고 힌트만 생략한다. internal_id를 MDC로 복구하지 않는다. 현재 모바일 random event ID와 BO 전용 resolver는 수정 대상이다([app] `M/src/telemetry/transport.ts:271-305`, `M/src/api.ts:466-475,565-591`; [common] `C/core/ScreenMetadata.java:19-38`, `C/core/UiCodeResolver.java:16-22`).

## 6. OpenSearch 현황·수신 계약·C7 경계

### 6.1 고정 입력과 실측 범위

이 절의 근거 [I]는 **INFRA@e513fb02a156a1e90ae6722ec2e5f06bbaa92beb**의 [docs/gcom/gcom-c2-logs.md](../../../261021-1049-gcom-c2-contract.a01.read/INFRA/docs/gcom/gcom-c2-logs.md)다. 링크는 이번 read-repo 배치 기준이며 고정 리비전·문서 경로가 원본 식별자다. [I] 첫머리 1038.a01 「C2-1 실측 마감 보고」 및 그 아래 보존된 1031.a01 최종 요청짝·1013.a03 전체 매핑·1013.a01 익명 trace 순서·0927.a02 채움 표를 사용한다. 과거 실패를 소급 pass로 바꾸지 않는다. 모든 관측은 dev이며 운영·0.6.1 적용 결과로 일반화하지 않는다.

채움 범위 W는 UTC `[2026-10-19T23:18:39.175420Z,2026-10-20T23:18:39.175420Z)`, `logs-v2-dev*`. 분모는 서비스 조건에 맞는 전체 문서, 분자는 indexed exists 문서다. 빈 문자열 제거율·요청 성공률이 아니다([I] 0927.a02 ①).

| 서비스 조건 | 분모 | trace.id 및 span.id 각각 | parent.id | golmok.request.id |
|---|---:|---:|---:|---:|
| app, 과거 wildcard `*app*` | 1,029,843 | 1,028,122 / 99.8329% | 0 / 0% | 991,937 / 96.3192% |
| member, 과거 wildcard `*member*` | 4,968 | 2,771 / 55.7770% | 0 / 0% | 2,732 / 54.9919% |
| batch, 과거 wildcard `*batch*` | 43,866 | 36,093 / 82.2801% | 0 / 0% | 0 / 0% |
| BO, 정확 일치 `golmok-bo` | 13,819 | 13,522 / 97.8508% | 0 / 0% | 13,522 / 97.8508% |

서비스 조건·조회 스냅샷이 달라 행을 합산하지 않는다. `*backoffice*` 0건은 BO 부재 증거가 아니다. BO 97.8508%의 pass는 이 필드 존재 집계에만 한정한다.

### 6.2 현재 매핑 대 v5 및 템플릿 영향

[I] 1013.a03: 2026-10-21T01:21:23.152274Z 전체 dev 5인덱스 매핑 HTTP200, base-common@e64c545f4d5b 내부 v5/94필드·생성기 preview와 비교했다. preview는 배포 템플릿 정본이 아니다.

| 인덱스 suffix | 등록 존재/94 | 존재 필드 타입차이 | 루트 dynamic |
|---|---:|---:|---|
| 000001 | 87/94 | 0 | strict |
| 000002 | 86/94 | 0 | strict |
| 000003 / 000004 / 000005 각각 | 89/94 | 0 | strict |

매핑 합집합 90/94, 인덱스 간 타입충돌 0이다. 전체 공통 누락은 `golmok.service.invocation.id`, `golmok.payload.policy.id`, `golmok.payload.omitted.reason`(keyword), `golmok.payload.truncated`(boolean). 추가 scalar는 폐기된 `golmok.screen.internal_id: keyword`다. 인덱스별 나머지 누락은 [I] 1013.a03 원표를 따른다. 이 pass는 매핑 비교 범위만 뜻하며 개별 인덱스 90필드·과거 색인 실패 부재를 보장하지 않는다.

**현재 적용 템플릿 내용은 미확보**다([I] viewer403·관리자 시뮬레이션401 기록). 따라서 현재 템플릿과 v5의 직접 diff는 확정하지 않는다. 위 매핑 차이가 확인된 영향이고, C7/A17이 배포 템플릿과 object별 dynamic/enabled를 별도 대조한다. strict 매핑에 신규 필드를 보내기 전에 explicit template에 위 타입을 추가하고 새 rollover index에서 수용한다. 기존 trace/span/parent는 keyword, duration은 long(ns)을 유지한다. 동적 payload 객체를 허용하거나 과거 parent를 추정 backfill하지 않는다. 대시보드는 완료 수·outcome·duration을 사용하며 수신짝 지표와 분리한다.

### 6.3 01:17(KST) 흐름의 관측 단절과 원인 한계

[I] 1013.a01 「익명 trace 순서」의 UTC `2026-10-20T16:17:48Z`부터 `16:17:53.000000000Z`까지(10-21 01:17 KST)는 **27건·14익명 trace 묶음**, app14/member2/batch6/bo5다. 원응답30건 중 끝 53초 소수부 app3건을 제외한 저장 집계이며 재조회하지 않았다. G3/G4/G8은 batch→BO→app 순서의 공통 trace(총14문서), G10의 member2문서는 별도 trace다. 시간 순서·같은 trace만으로 호출 방향이나 실제 parent를 증명하지 않는다. 과거30건 표의 parent 존재0은 엄격27건에도 적용되지만, 이것이 실제 span 부모 부재를 뜻하지 않는다.

회원과 다른 묶음을 연결할 로그 증거가 없다는 **관측상 단절**이다. 비밀번호재설정→회원→배치이벤트→앱이라는 업무 단건의 인과 단절 확정은 아니다. [mem] §7은 afterCommit JDK 호출의 명시 W3C 주입 부재(런타임 agent 주입은 미확인), [batch] §⑦은 single 예약 INSERT의 envelope 부재를 정적 위험으로 제시한다. 이 둘은 해당 표본의 확정 원인이 아니다. A13은 §7A의 영속 부모 계약, 서비스 담당자는 업무 사건 표지와 agent 설정을 확인해야 한다. 14묶음 pass를 E2E 성공으로 바꾸지 않는다.

### 6.4 수신 계약과 요청짝 관측

[I] 1031.a01 최종 집계: W·정확 서비스 `golmok-app/member/batch/bo`·action prefix `http.server.`. 최종 조회 시작 2026-10-21T01:32:50.848595Z, HTTP200, timeout=false, shards5/5 성공·실패0. 첫 composite의 60,000버킷 상한 결과는 제외했다.

| 서비스 | 수신 문서/고유 R | 완료 문서/고유 C | 교집합 | 수신만 / 완료만 | 중복 수신/완료 | 키누락 수신/완료 | 짝 비율 |
|---|---:|---:|---:|---:|---:|---:|---|
| app | 0/0 | 986134/986134 | 0 | 0/986134 | 0/0 | 0/0 | null |
| member | 0/0 | 2624/2624 | 0 | 0/2624 | 0/0 | 0/0 | null |
| batch | 0/0 | 0/0 | 0 | 0/0 | 0/0 | 0/0 | null |
| BO | 0/0 | 13122/13122 | 0 | 0/13122 | 0/0 | 0/0 | null |

총 1,001,880건의 관측 action은 `http.server.complete`뿐이다. R/C는 같은 서비스 내 고유 request.id 집합, 짝 비율은 `|R∩C|/|R|×100`, 분모0이면 null이다. 중복은 이벤트·키별 `max(n−1,0)` 합, 키누락은 해당 이벤트 문서 중 request.id 부재다. 수신 미관측은 이 prefix·시간창 안의 사실이며 다른 이벤트명·기간 밖 짝·수집 유실 원인은 미확정이다.

**수신 계약 보강안(A05 검토 대상):** 부모 계획의 수신1·종료1 요구와 기존 §2 완료1건 결정은 불일치한다. 기존 완료 의미를 보존하되 신규 `http.server.received`를 요청당 1건, `http.server.complete`를 종료당 1건으로 하는 변경을 **0.6.2 후보**로 분리한다. 수신은 외곽 관측 scope가 request.id를 발급한 직후·보안 분기 전 기록하고, 동일 ID를 async 재진입에도 유지한다. 수신에는 action/dataset/request.id/발생UTC 및 유효 trace/span만, 완료에는 같은 ID와 실제 status/outcome/duration·안전 오류 코드를 기록한다. 기존 §2의 오류·CAS 마감·컨테이너 도달 전 제외 경계를 유지한다. JDK 자체 서버는 수락 어댑터 경계에 동일 계약을 적용한다. 서비스·이벤트·request.id로 중복 제거하고, 시간창 끝의 미완료는 합의한 최대 요청/async 수명만큼 닫힘을 기다린 뒤 판정한다(수명 값은 서비스별 실행 전 고정). key 누락·미완료·중복은 별도 건수로 남긴다.

정적 0.5.0 로거가 완료1건이라는 [common]/[mem] 근거는 미관측의 가능한 설명이지만, 관측 서비스의 resolved 버전·로그 설정·수집 경로가 확정되지 않아 실측 원인으로 단정하지 않는다. A13/A05는 수신명·필터 경계 및 소비 지표를 합의하고 서비스 담당자는 실행 버전/설정을 확인한다. 이번 문서로 §2를 소급 구현하거나 수신 부재를 성공으로 바꾸지 않는다.

### 6.5 적재 지연은 C7 이관

[I] 1038 마감·1013.a03·0927.a02: W 내 최신100건에서 @timestamp 100/100, 적재 후보10필드 각각0/100, 검증된 동일문서 시각쌍0이다. 후보 목록은 event.created/event.ingested/ingest.timestamp/_ingest.timestamp/timestamp/observed_timestamp/observedTimestamp/time/time_unix_nano/observed_time_unix_nano다. 전체5개 매핑의 date/date_nanos는 발생UTC인 @timestamp뿐이다. **지연 수치 계산 불가(delay_ms=null)**이며 0ms나 전체 저장 경로의 미저장을 뜻하지 않는다.

C7/A17은 ingest pipeline에서 동일문서의 `event.ingested`(UTC date 또는 date_nanos, 실제 수집 저장 경계 의미 명시)를 보존하고 원 발생시각 `@timestamp`와 연결한다. 파일→S3→OpenSearch 구간별 지연도 요구하면 파일 기록UTC·S3 도착UTC·색인 경계UTC 및 동일 이벤트 식별자가 추가로 필요하다. 시계 동기·재전송·음수 지연 처리도 기록한다. 산식은 `(검증된 적재UTC−발생UTC)×1000 ms`; 조회시각·인덱스 생성시각·event.duration으로 대체하지 않는다. 표본수·시간범위·누락수·p50/p95/max는 필드 확보 후 C7에서 산출한다.

## 7. 0.6.1 적용 전/후 dev 성능 관문 (이번 실행 없음)

아래 수치(동시성·기간·오류 한도)는 **측정 설계값**이며 기존 실측값이 아니다. 기존 근거로 확정되는 경로는 [mem] §7 `EmailDeliveryEvents.java:69`의 `/internal/jobs/v1/batch/email-delivery/events`뿐이다. 이는 부작용 있는 이메일 예약 경로로, 반복 성능 대상으로 승인되었다는 근거가 없다. BO/APP/MEM의 부하용 method·route·dev base URL, 인증 헤더/환경 키의 실제 이름, 요청 fixture·예상 status는 지정 입력에 없다. 인증이 있다는 사실([mem] §7)만으로 Authorization/Bearer 등의 방식을 추정하지 않는다. **실행 전 서비스 담당자가 안전한 대상과 인증 키 이름·주입 방식을 확정하기 전에는 실행 불가**다. 아래 `PERF_*`는 이 절차의 신규 로컬 변수이며 기존 서비스 키 이름을 주장하지 않는다.

1. 서비스 담당자가 service revision·image digest·적용 전 resolved 공통 버전/8산출물 SHA와 적용 후 **0.6.1 e64c545f** SHA, dev 환경 식별자·실행기 위치를 manifest에 고정한다. C1-1의 16d59fa 내용 대조는 실행 중 이미지 증명이 아니므로 baseline으로 대용하지 않는다. 동일 서비스 revision으로 공통 의존성만 전/후 비교하고, 그 외 차이가 있으면 원인을 분리할 때까지 판정 보류한다. agent 버전/propagator/sampling, JVM/CPU/메모리/replica, DB fixture·캐시 초기화, downstream 응답, 로그 수준/전송기·payload opt-in을 동일하게 고정한다.
2. 담당자는 `PERF_URL`(확정 method/route의 dev URL), `PERF_METHOD`, `PERF_EXPECTED_STATUS`, 필요한 경우 `PERF_BODY_FILE`, `PERF_AUTH_HEADER_NAME`을 manifest에 키 이름만 기록한다. 실제 인증값은 승인된 개발 로그인/비밀 주입 경로에서 `PERF_AUTH_HEADER_VALUE`로 메모리에 주입한다. 로그인 자체는 측정 구간 밖이며 양쪽 동일 권한·유효기간이어야 한다. 출력·명령 인자·원자료에는 값을 남기지 않는다. 인증이 복수 헤더/쿠키이면 아래 단일 헤더 예제를 그대로 실행하지 말고 담당자가 동일 방식으로 fixture를 확정한다.
3. 제안 도구는 **k6**이며 설치 여부·버전·실행 네트워크·스크립트 경로는 미확인이다. 설치하지 말고 실행 전 `command -v k6`와 승인 버전 확인, URL/TLS·인증·비파괴 fixture 확인을 완료한다. 후속 실행자는 아래 스크립트를 승인 출력 디렉터리의 `perf.js`로 저장한다. 정상·예상 오류·async·8KiB 경계 fixture는 지원 엔드포인트와 안전 정책을 먼저 확인해 각각 별도 동일 조건으로 실행한다. 미지원 시나리오를 임의 생성하거나 서로 다른 혼합비를 합산하지 않는다.

```javascript
import http from 'k6/http';
import { check } from 'k6';
export const options = { vus: 10, duration: __ENV.PERF_DURATION || '15m',
  discardResponseBodies: true, summaryTrendStats: ['count','p(95)'],
  thresholds: { checks: ['rate>=0.99'] } };
const body = __ENV.PERF_BODY_FILE ? open(__ENV.PERF_BODY_FILE) : null;
export default function () {
  const headers = {};
  if (__ENV.PERF_AUTH_HEADER_NAME)
    headers[__ENV.PERF_AUTH_HEADER_NAME] = __ENV.PERF_AUTH_HEADER_VALUE;
  if (__ENV.PERF_CONTENT_TYPE) headers['Content-Type'] = __ENV.PERF_CONTENT_TYPE;
  const r = http.request(__ENV.PERF_METHOD, __ENV.PERF_URL, body,
    { headers, redirects: 0, timeout: '30s', tags: { name: 'contract-perf' } });
  check(r, { expected_status: x => x.status === Number(__ENV.PERF_EXPECTED_STATUS) });
}
```

후속 실행 명령(이번 미실행, 환경 변수는 위 선행조건 완료 후 주입):

```sh
PERF_DURATION=5m k6 run "$PERF_SCRIPT" --summary-export "$PERF_RUN_DIR/warmup.json"
PERF_DURATION=15m k6 run "$PERF_SCRIPT" --summary-export "$PERF_RUN_DIR/summary.json" --out "json=$PERF_RUN_DIR/samples.jsonl"
```

`PERF_SCRIPT`는 위 perf.js의 확정 절대경로, `PERF_RUN_DIR`는 새 회차 디렉터리다. 각 회차 10 VU·5분 워밍업을 제외하고 15분 측정, A(전)→B(후)를 3회 교대한다. 각각 같은 fixture byte/hash·method·route·상태 기대값·동시성·기간·timeout·데이터 초기 상태를 사용한다. 도구 자체 오류/출력 손실/환경 변경 시 해당 회차는 무효로 남기고 자동 재실행하지 않는다.

4. 각 회차 표본 n=측정 구간의 전체 HTTP 시도 수, 완료 수·timeout 수·status별 건수·예상 밖 응답률을 남긴다. 예상 밖 응답률=(예상 status 불일치 시도)/n, n=0은 판정 불가다. 정상 fixture는 예상 밖 응답률 ≤1%를 설계 기준으로 사용하고 오류 fixture의 예상4xx/5xx는 별도 분류한다. 양쪽 오류율·처리량·CPU·heap/GC·로그 bytes/s·drop을 함께 검토해 오류/누락 증가로 빨라진 결과를 승인하지 않는다. `http_req_duration`(전송+대기+수신 ms, 연결 단계 제외)을 양쪽 동일 지표로 쓰며 전체 응답시간과 혼동하지 않는다. 측정 표본 전체(실패 포함)의 p95를 각각 구하고 A 3회 p95의 중앙값 PA, B 중앙값 PB로 `((PB/PA)−1)×100 ≤5%`면 지연 관문 통과다. PA≤0·표본/원자료 누락·조건 불일치·오류 기준 초과면 pass 불가다. 처리량 차이도 병기하며 고정 VU 결과를 고정 RPS 결과로 주장하지 않는다.
5. 원자료는 후속 실행의 승인 `TEAM_OUT_DIR/perf/<service>/<A|B>/<round>/<scenario>/`에 manifest(리비전·SHA·환경·도구 버전·fixture hash·UTC), fixture의 비밀 없는 버전, summary.json, samples.jsonl, 자원/로그 계측, 계산표를 보존한다. URL query·비밀/개인 식별자를 fixture에 넣지 않고 HTTP debug를 켜지 않는다. 원자료가 민감 정보를 포함하면 공유 전 비식별 처리하고 지표를 보존한다. 현재 이 경로에는 성능 자료가 생성되지 않았으며 실제 주소·인증 키·안전 엔드포인트·도구 확보는 확인하지 못했다. 실행·판정은 해당 서비스 담당 및 A13/A17 후속 범위다.

## 7A. 비동기·job 및 단건 이메일 영속 연결

현재 JobPairLogger는 job.start/end CAS 마감, 소유 span만 종료하며 beginInCurrentSpan에는 execution.id가 필요하다. 저장 envelope는 owner 검증 뒤 CONSUMER setParent로 복원한다([common] `C/logging/JobPairLogger.java:21-119`, `C/core/TelemetryEnvelope.java:52-92`). 신규 `TelemetryTaskDecorator`는 opt-in executor 및 명시 wrap 경계에서 OTel Context와 불변 로그 snapshot을 캡처/복원/finally 해제한다. MDC만 복사하지 않으며 중복 span을 만들지 않는다. 새 비동기 업무는 CONSUMER/INTERNAL span을 명시 생성한다. 독립 scheduler는 root, 원인이 있는 독립 root는 link로 연결한다. OFF/NO_CLAIM은 실행 pair를 만들지 않고 고정 상태의 admission 결과만 기록하며, claim 실패도 실패 admission으로 분리한다.

**단건 메일 필수 흐름:**

1. MEM 요청 SERVER→인증 이메일 ingress의 INTERNAL span에서 afterCommit callback 등록 시 `TelemetryEnvelope`를 캡처한다. rollback이면 보내지 않는다. callback 실행 시 캡처 context를 복원하고 JDK HTTP CLIENT span을 agent가 생성·W3C 주입한다. 현재 명시 주입이 없는 사실을 실패 확정으로 해석하지 않는다([mem] `M/notification/internal/infrastructure/EmailDeliveryEvents.java:39-85`).
2. BATCH EmailEventServer는 §2 SERVER 경계에서 인증·환경·dispatchId 및 멱등을 검증한 다음 **수신 SERVER context**로 envelope를 캡처한다. 원문 header/body를 저장 로그에 쓰지 않는다. single 예약 INSERT에 envelope와 schema version을 동일 transaction으로 저장한다. dispatchId를 trace ID로 변환하지 않는다.
3. EmailEventService의 stage=single/source=member/limit=1 예약을 scheduler tick이 소비할 때 저장 envelope를 RunEngine claim까지 전달한다. scheduler root로 덮어쓰지 않는다. 현재 INSERT에 envelope가 없어 원 event 연결이 입증되지 않는 구간을 고친다([batch] `R/EmailEventService.java:59-69`, `R/JdbcRunStore.java:112-121`).
4. owner 검증된 envelope로 CONSUMER span(setParent)을 만들고 새 execution.id/attempt ID를 설정한 후 JobPairLogger.beginInCurrentSpan을 연다. owner execute HTTP CLIENT→MEM owner SERVER→메일 전송 INTERNAL/CLIENT가 이어진다. 이메일 주소·본문·공급자 자격증명은 어느 단계에도 로그 payload로 남기지 않는다.
5. 재시도는 저장된 원 인입 envelope를 parent로 유지하는 새 attempt span이다. attempt 간 span ID 재사용은 금지한다. 멱등 중복 수신은 새 완료 이벤트만 남기며 중복 예약/발송하지 않는다. envelope 없음/무효/owner 불일치 시 신뢰하지 않고 새 root와 고정 사유 코드로 처리한다. envelope 8192자 제한은 payload 8KiB와 다른 계약이며 기존 저장 직렬화 상한을 유지한다.

C3 종단 확인은 MEM 원 SERVER부터 BATCH ingress/CONSUMER/owner까지 동일 trace 및 exporter 실제 parent chain, 로그 parent.id 일치, single 성공/실패/중복/재시도/invalid envelope를 대상으로 한다. 이 문서는 그 결과를 주장하지 않는다.

## 8. 새 버전·서비스 적용 순서·위험 및 깨지는 API

이 절은 최초 0.6.0 설계 이력이다. 현재 정식 기준은 **0.6.1 e64c545f**이며 수신 계약 등 추가 불일치 수정은 §6.4의 **0.6.2 후보**로 검토한다. 이하 최초 제안 버전은 BOM/lib/starter 모두 **0.6.0**이다. agent 의존 및 이벤트 의미 변경 때문에 0.5.x patch로 숨기지 않는다. 현재 발행된 버전이 아니며 실제 좌표 충돌/발행 가능 여부는 미확인이다. 서비스별 현재 선언은 BO=0.5.0, APP/MEM=0.4.1, BATCH app=0.3.0/runtime=0.4.1이고 resolved 배포 버전은 미확인이다.

순서: (1) 공통 0.6.0 후보 및 agent extension·registry/template 소비 준비 → (2) GS-BO 제한 적용 → (3) GS-BATCH app/runtime 함께 정렬 → (4) GS-MEM → (5) GS-APP backend/mobile 함께 전환. 메일 trace 연결은 BATCH 수신·저장 지원 이후 MEM 송신을 켜는 두 단계 전환이다. TODO는 등록·고정 입력을 PM이 제공하기 전 적용 대상에서 보류한다. 각 단계는 오류/async 완료 건수·부모 연결·payload 차단·성능 승인 이후 다음으로 진행한다. rollback은 이전 BOM/agent 설정/template 질의를 묶어서 되돌리고 저장 envelope의 이전 reader 호환성을 먼저 확보한다.

| 서비스 | 위험 1 | 위험 2 | 위험 3 | 조사 근거 |
|---|---|---|---|---|
| GS-BO | agent/parent.id 실제 설치 미확인 | 구 HTTP pair 테스트·집계 불일치 | 서비스 쌍·명시 payload 정책 부재 | [common] `BO/pom.xml:25-53`, `BO/src/main/resources/application.yml:57-74`, `BO/src/test/java/ai/mutuus/backoffice/shared/LoggingConfigurationTest.java:110-140`; 서비스/payload 부재는 BO/src/main 검색 범위 |
| GS-BATCH | apply no-op/SDK test scope로 runtime 전파 공백 가능 | HTTP pair→complete 소비 불일치·자체 서버 미포함 | 필드 출처·envelope 복원 조건·single 유실 및 혼합 버전 | [batch] `C/core/TraceHeaders.java:52`, `batch-common/runtime/pom.xml:6-13`, `C/logging/AccessLogger.java:24,38`, `R/AdminHttpServer.java:24-99`, `R/BatchTelemetry.java:43`, `R/EmailEventService.java:59-69`, `batch-app/pom.xml:16-38` |
| GS-MEM | JDK 이메일/이슈 MDC 상관 출처 차이 | HTTP 완료/async 오류 수집 의미 차이 | internal_id 및 Security/Idempotency 직접 API 결합 | [mem] `M/notification/internal/infrastructure/EmailDeliveryEvents.java:69-85`, `M/issuecollector/IssueCollector.java:36-39`, `M/issuecollector/IssueErrorFilter.java:30-33`, `M/shared/ScreenIdentityTraceFilter.java:34`, `M/adminoperation/internal/security/AdminOperationSecurityConfiguration.java:150-157`, `M/shared/WithdrawalIdempotencyConfiguration.java:23-26` |
| GS-APP | JDK apply·agent 의존과 이슈 MDC 공백 | pair/dual/legacy MDC 소비 불일치 | 모바일 random UI ID·BO resolver로 힌트 누락 | [app] `B:java/IssueErrorObservation.java:39-51`, `COMMON:J/core/TraceHeaders.java:52-54`, `B:res/application.yml:97-105`, `M/src/telemetry/transport.ts:271-305`, `COMMON:J/core/UiCodeResolver.java:16-22` |
| TODO(보류) | 실제 의존 버전 미제공 | HTTP/async 경계 미제공 | payload/UI 소비 정책 미제공 | 조직 미등록으로 조사 입력 미제공. 서비스 사실로 단정하지 않음 |

### 깨지는 API·소비 계약 목록 (위험 목록과 별도)

‘깨짐’은 **확인된 의미/필드 호환 단절**, **0.6.0 제안 변경**, **조건부 소스 결합점**을 구분한다. 전체 바이너리 API diff나 컴파일 파손은 조사표에 없으므로 확정하지 않는다. 기존 Java 메서드의 삭제·시그니처 변경을 새로 요구하지 않는다.

| API/필드·소비자 | 현재 근거와 단절 유형 | 0.6.0 제안 전환 |
|---|---|---|
| `TraceHeaders.apply(HttpRequest.Builder, boolean)`; APP claim/push, MEM claim, BATCH owner | 비교 0.5.0 no-op 확인. 기존 호출만으로 전파된다는 의미가 성립하지 않음. 0.4.1 실제 구현 차이는 미확인. [common] `C/core/TraceHeaders.java:52-54`; [app] `B:java/adminoperation/internal/jobs/HttpBoClaimReader.java:30-32`, `B:java/notification/internal/infrastructure/HttpPushTemplateSource.java:33-36`; [mem] `M/jobs/MemberBoClaimClient.java:35`; [batch] `R/HttpOwnerClient.java:66` | 호출 유무 대신 §1 agent 표준·계측 지원을 전파 전제로 명시; 완료 로깅은 ObservedJdkHttpClient로 연결 |
| `AccessLogger.httpPair` 및 HTTP pair action 소비; BO 테스트·APP/MEM/BATCH 집계 | API 잔존과 자동 출력은 별개. 0.5.0 필터는 complete 1건. [common] `C/logging/AccessLogger.java:23-80`, `BO/src/test/java/ai/mutuus/backoffice/shared/LoggingConfigurationTest.java:110-140`; [batch] `C/logging/AccessLogFilter.java:121` | §2 완료 1건으로 테스트·집계/조인 전환; facade 삭제 제안 아님 |
| `AccessLogger`의 `LogFormat`/`parentId` 인자, `format=dual`, MDC trace/parent 소비 | 인자가 canonical 출력·실제 부모를 보장하지 않음. [common] `C/logging/AccessLogger.java:65-80`; [app] `COMMON:J/logging/AccessLogger.java:20-21`, `B:java/IssueErrorObservation.java:39-51`; [mem] `M/issuecollector/IssueCollector.java:36-39` | legacy 필드 전제 제거, `TelemetryContextSnapshot.capture()` 및 실제 lineage로 이슈 상관 전환 |
| `HttpClientLoggingInterceptor` outcome | 현재 4xx success, 제안은 failure. [common] `C/web/HttpClientLoggingInterceptor.java:45-111` | 오류율·알림 기준을 4xx 포함 failure로 변경 |
| `ControllerMethodLoggingAspect` / `DefaultMethodLoggingWriter`, 기존 method/payload opt-in | 정상 반환만 exit·문자열화 경로는 신규 안전 서비스 쌍 계약과 불일치. [common] `C/aop/ControllerMethodLoggingAspect.java:29-61`, `C/aop/DefaultMethodLoggingWriter.java:36-78`, `C/payload/PayloadLoggingProperties.java:12-68` | §3 ServiceMethodLoggingAspect 및 §4 명시 schema opt-in; 구 설정을 신규 안전 경로로 자동 승격하지 않음 |
| `golmok.screen.internal_id`, `ScreenMetadata`/`UiCodeResolver` 입력 | v3→v4 필드 제거 및 정본 코드/version 검증으로 의미 단절. MEM 직접 MDC 소비와 APP 헤더 영향. [common] `af7bb9d22:R:762-779`, `C/core/ScreenMetadata.java:19-38`; [mem] `M/shared/ScreenIdentityTraceFilter.java:34`; [app] `M/src/api.ts:466-475` | screen.id+catalog version 및 서비스별 resolver로 전환; internal_id 복구 금지 |
| `X-Event-Id`/`golmok.ui.event.id`, 모바일 eventId override | 임의 ID→실제 UI root span ID 의미 단절. [common] `af7bb9d22:R:1000-1003`→`R:982-985`; [app] `M/src/telemetry/transport.ts:271-305`, `M/src/api.ts:565-591` | 실제 root span ID 사용·override 일치 검증 |
| MEM의 공통 Security 자동구성 직접 호출, `IdempotencyFilter` 상속·빈 교체 | 조건부 소스 결합점; 시그니처/생성자/빈 이름 변경 시 깨질 수 있으나 실제 파손은 미확인. [mem] `M/adminoperation/internal/security/AdminOperationSecurityConfiguration.java:150-157`, `M/shared/WithdrawalIdempotencyConfiguration.java:23-26`, `M/shared/WithdrawalIdempotencyFilter.java:3,25` | 이 계약은 해당 API 변경을 요구하지 않음. C3에서 유지 여부를 대조하고 변경 시 소비자를 함께 수정 |
| `JobPairLogger.beginInCurrentSpan`, `TelemetryEnvelope` 복원 | execution.id metadata 및 owner 검증 요구. 구 저장 데이터/바이너리 전체 호환은 미확인. [common] `C/logging/JobPairLogger.java:34-53`, `C/core/TelemetryEnvelope.java:52-92`; [batch] `R/BatchTelemetry.java:37-55` | §7A execution.id 설정·owner 검증·single envelope 영속; invalid는 새 root 처리, 기존 reader 호환 확보 |

## 9. C3 인수 기준·근거 한계

C3 구현 위치는 이 문서에서 제안한 클래스의 해당 lib 패키지와 agent extension이며 이번 쓰기 승인은 계약서와 C2 현황 문서뿐이다. 설정은 기존 logging/method/payload 설정과 신규 키를 명확히 분리하고 기존 raw writer의 신규 안전 경로 우회를 차단한다. 샘플은 시작·완료 수가 아니라 실제 exporter 부모·정본 로그 의미를 증명해야 한다.

후속 인수 항목은 유효/무효 W3C와 root/remote/async parent, agent 부재 시 생략, HTTP 정상·모든 표의 오류·비동기·자체 서버·JDK outbound 1회 완료, INTERNAL enter/exit의 예외·취소·비동기 1쌍, payload deny 및 UTF-8 8KiB/32KiB 경계, v5/UI ID 분리, OpenSearch mapping/질의, §7 p95, §7A 단건 메일 영속 chain이다. 이는 후속 구현의 인수 요구이며 이번 작업의 시험 실행 허가가 아니다.

추가 자료는 PM 경유가 필요하다: 서비스별 정확한 opt-in 메서드/API 시그니처 목록, 승인된 payload field schema, agent 고정 버전/계측 지원, OpenSearch 현행 template, A17 실측 및 정식 8산출물 SHA, TODO 등록·고정 입력. 상위 필수 9항목은 더 이상 미제공 자료가 아니다. 미등록 저장소 우회·운영 접근은 하지 않았다.

문서 검증: 이번에 제공된 상위 9항목과 보완 diff를 정독 1회 대조했다. 기존 입력 소스 정독·시험·빌드는 반복하지 않았다. 9항목 대응표, API 단절 목록, 현재/제안 구분 및 조사표 파일:줄 근거를 반영했다. 실제 배포·컴파일·agent extension 실행 가능성·실측·OpenSearch 운영 mapping은 확인하지 않았다.

## 결과 요약 (10줄)

1. 첨부 0528.a01 초안의 계약 결정을 재사용해 문서를 복원했다.
2. 제공된 상위 9항목 대응표를 추가하고 원문 미제공 선언을 정정했다.
3. HTTP agent 단일 소유 결정의 이유와 실제 parent.id 수집 계약을 유지했다.
4. HTTP 완료 1건 및 오류·async·자체 서버 경계를 명시했다.
5. INTERNAL 서비스 쌍·대상 선정·설정 키를 구체화했다.
6. 안전 payload 필드 schema 등록 API와 8KiB/32KiB 상한을 보완했다.
7. v3/v4 차이·v5 필드 타입/의미·UI root span ID 계약을 명시했다.
8. §6은 기존 실측·범위·C7 이관으로, §7은 미실행 성능 절차로 보강했다. C2 전체 완료 판정은 보류한다.
9. 0.6.0 적용 순서·서비스별 위험 3개와 별도 깨지는 API 목록을 명시했다.
10. 단건 메일 envelope 영속 연결을 유지했으며 구현·실측·정식 SHA·미등록 TODO 사실은 추정하지 않았다.
