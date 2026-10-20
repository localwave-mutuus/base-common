# G-COMMON 0.6.0 사전 구현 계획

상태: REQ-261021-02 / C3 사전 설계. 이 문서는 구현·발행 승인이 아니다.
입력 정본: brief 첨부 `g-common-trace-log-contract.md`(0534.a01). 현재 사본 `390c768`(0.5.0)은 구조 확인에만 사용했다. C1 0533.a02는 검증 fail이며 v0.5.2와 정식 8SHA는 미확정이다. C1 기존 patch를 통합하지 않는다.

## 1. 구현 시작 게이트와 범위

- **G0 닫힘:** C1 v0.5.2 정식 발행 성공, GitHub의 해당 태그가 가리키는 확정 커밋, 정식 8SHA 및 검증 pass 근거를 PM이 제공하기 전에는 아래 구현·의존성/버전 변경을 시작하지 않는다. 실제 구현은 그 GitHub v0.5.2 확정 커밋에서 새로 시작한다. 현재 390c768에 구현을 얹지 않는다.
- G0 해제 후 해당 커밋에서 아래 경로·API를 재대조한다. C1 변경으로 달라진 경로는 PM과 계획을 갱신한 뒤 착수하며, 여기의 신규 경로는 제안이지 현재 존재한다는 뜻이 아니다.
- C3 **첫 커밋**에 첨부 계약을 `docs/contracts/g-common-trace-log-contract.md`로 원문 포함하고 출처 0534.a01 및 실제 기준 커밋을 기록한다. 이번 작업은 이 계획 파일만 쓴다.
- 승인 범위는 dev까지다. GS-* 수정·push·태그·Publish·배포·파이프라인 실행은 이번에 하지 않는다. 타 저장소를 읽거나 변경하지 않는다. GS-BO도 후속 조사 시 읽기전용이다. 후속 작업의 배정과 승인 판단은 PM 소관이다.

## 2. 경로 표기와 구조 근거

아래 표의 `J`는 `lib/src/main/java/ai/mutuus/common`, `T`는 **신규** `lib/src/test/java/ai/mutuus/common`, `R`은 `lib/src/main/resources`다. `{A,B}.java`는 각각 별도 정확한 파일을 뜻한다. `T` 시험은 향후 lib의 시험 의존성/SDK test fixture를 통합 담당이 준비하며 로컬 sample-api 반복 시험을 대체한다.

현재 root reactor는 parent/bom/lib/starter-web/starter-batch와 sample-api/sample-batch를 포함한다. `J/logging/CanonicalLogSnapshot.java`는 실제 부모 없이 OTel trace/span을 읽고 message·throwable stack을 수용한다. `J/logging/CommonJsonEncoder.java`는 JSON 전체 byte 상한이 없다. `J/web/CommonWebAutoConfiguration.java`는 MVC 조건 아래 outbound customizer를 둔다. `J/core/ScreenMetadata.java`는 UI ID 형식만 검증하고 `J/core/UiCodeResolver.java`의 screen 경로는 BO에 고정돼 있다. 이 사실만 구조 설계에 사용하며 0.5.2에서 동일하다고 단정하지 않는다.

## 3. 구현 작업 3개와 독점 쓰기 구역

아래는 후속 구현의 작업 분할 제안이며 이번에 작업을 생성하거나 배정하지 않는다. 각 작업의 쓰기 구역은 서로 배타적이다. 목록 밖 파일은 변경 전에 PM이 구역을 조정한다.

### W1 — lineage·agent 정책·HTTP 완료 (§1·§2)

**독점 구역:** `J/observability/` 전체, `J/web/` 전체, `J/logging/{AccessLogFilter,AccessLogger}.java`, `T/observability/`, `T/web/`, `T/logging/HttpCompletionContractTest.java`, 신규 `agent-extension/` 전체.

**기존 수정:** 위 logging 두 파일, `J/web/{TraceFilter,HttpClientLoggingInterceptor,CommonWebAutoConfiguration,HeaderPropagationInterceptor}.java`. 기존 header interceptor는 화면/승인 메타데이터만 담당하고 W3C inject 또는 주입 뒤 삭제를 추가하지 않는다.

**신규 파일:**

- `J/observability/{SpanLineageAccessor,SpanLineageSnapshot,TelemetryContextSnapshot,CommonObservabilityProperties}.java`.
- `J/web/{HttpCompletionScope,ObservedJdkHttpClient,CommonHttpClientObservationAutoConfiguration,JdkHttpServerObservationHandler,JdkHttpServerAdmissionAdapter}.java`.
- `agent-extension/pom.xml`, `agent-extension/src/main/java/ai/mutuus/common/agent/{LineageAutoConfigurationCustomizerProvider,TrustedDestinationPropagationCustomizer}.java`, `agent-extension/src/main/resources/META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider`.
- `agent-extension/src/test/java/ai/mutuus/common/agent/TrustedDestinationPropagationCustomizerTest.java`, `T/observability/TelemetryContextSnapshotTest.java`, `T/web/{HttpCompletionScopeTest,ObservedJdkHttpClientTest,JdkHttpServerObservationHandlerTest}.java`.

계약 FQCN을 유지하도록 processor는 신규 `agent-extension/src/main/java/ai/mutuus/common/observability/SpanLineageSpanProcessor.java`에 두고, 시험은 `agent-extension/src/test/java/ai/mutuus/common/observability/SpanLineageSpanProcessorTest.java`에 둔다. SDK 의존성을 lib로 전이하지 않는다.

**소유권·설치:** HTTP SERVER/CLIENT 생성 및 W3C 추출/주입은 agent 소유다. lib는 accessor/snapshot API만 가지며 agent SDK를 별도 초기화하지 않는다. extension의 `onStart(parentContext, span)`에서 실제 부모를 수집하고 onEnd에서 live entry를 제거한다. root/SDK 미연결은 parent 생략, 진단은 제한된 카운터다. 기본 enabled=true, max-active-spans=100000이며 한도 초과 신규 보관을 건너뛴다. 종료 후 로그는 캡처한 불변 snapshot을 사용한다. 클래스 로더를 가로지르는 accessor 연결 방식은 고정 agent의 extension API로 확인하고 동일 저장소를 읽는지 fixture로 입증한다. SDK 연결 불가 상태를 정상 연결로 위장하지 않는다.

**agent extension은 필수:** SDK processor 설치와 최종 주입 지점의 목적지 정책은 lib interceptor로 보장할 수 없다. `AutoConfigurationCustomizerProvider`로 processor를 등록하고 고정 agent 버전이 제공하는 propagator/계측 확장 지점에서 destination 정책을 강제한다. 해당 provider만으로 목적지 hostname을 얻을 수 있다고 가정하지 않는다. 목적지 정보를 최종 주입에 전달하는 지원 API/계측 모듈이 확인되지 않으면 W1 완료·해당 클라이언트 배포 승인을 보류한다. 우회 명시 inject는 금지한다.

`OTEL_PROPAGATORS=tracecontext,baggage`, 고정 서비스명, parentbased_traceidratio 및 환경별 비율을 artifact에 기록한다. 내부 승인 목적지 전파, 외부 기본 차단(`external-enabled=false`), 정확한 `allowed-hosts`(wildcard 거부), baggage 별도 승인 정책을 적용한다. 화면 힌트 allowlist와 분리한다. 무효 traceparent는 새 root이며 사용자 헤더/MDC를 trace 부모로 채택하지 않는다.

**HTTP 완료:** UUID request.id와 monotonic ns를 가진 scope를 CAS로 1회 마감한다. 서버 `http.server.complete`, 클라이언트 `http.client.completed`만 출력한다. 필수 envelope·오류·route는 아래 대응표를 따른다. Access 필터를 CORS/Security/앱 오류보다 바깥에 두고 Trace 메타데이터는 Security 앞, 신원은 인증 결과 뒤에 둔다. 동일 최상위 순위 충돌은 소비 앱 전환 조건으로 명시한다. MVC 밖 자동구성으로 RestClient/RestTemplate 및 JDK sync/sendAsync를 지원한다. agent CLIENT span이 interceptor 진입 전 생성된다고 추정하지 않고 실제 CLIENT snapshot 확보 시점을 exporter 시험으로 확인한다.

JDK 서버는 agent span 차용 또는 명시 `jdk-server.span-owner=manual` 중 하나만 쓴다. manual만 W3C extract/SERVER 생성의 예외다. executor 제출 전 수락 scope를 열고 교환 객체가 있는 rejection은 503, 수락 후 handler는 동일 scope를 마감한다. 교환 객체 전달 전 rejection은 지표 경계다.

**완료 조건:** agent 중복 span/주입 없음, 실제 exporter parent와 로그 parent 일치, 종료 후 live entry 제거, 전파 정책 강제, HTTP 각 종료 1건 및 비웹 활성화를 변경 클래스 시험으로 증명한다. lib 안전 writer/정본 연결은 W3 이후 통합 완료로 판정한다.

### W2 — INTERNAL 서비스 쌍·안전 payload (§3·§4)

**독점 구역:** `J/aop/`, `J/payload/`, `T/aop/`, `T/payload/` 전체.

**기존 수정:** `J/aop/{CommonMethodLoggingAutoConfiguration,ControllerMethodLoggingAspect,DefaultMethodLoggingWriter}.java`, `J/payload/{PayloadLoggingFilter,PayloadLoggingProperties,BoundedPayloadResponseWrapper,DefaultRequestPayloadLogger,DefaultResponsePayloadLogger,CommonPayloadLoggingAutoConfiguration}.java`.

**신규 파일:**

- `J/aop/{ObservedServiceOperation,ServiceMethodLoggingAspect,ServiceMethodLoggingProperties,ServiceInvocationScope,ServiceMethodLoggingWriter,CommonServiceMethodLoggingAutoConfiguration}.java`.
- `J/payload/{SafePayloadPolicy,SafePayloadProjector,PayloadPolicyRegistry,HttpPayloadRule,ServicePayloadRule,ProjectorSchema,AllowedScalarField,ApprovedInput,SafePayloadProperties,BoundedPayloadRequestWrapper,SafePayloadEventWriter,CommonSafePayloadAutoConfiguration}.java`.
- `T/aop/{ServiceMethodLoggingAspectTest,ServiceInvocationScopeTest}.java`, `T/payload/{PayloadPolicyRegistryTest,SafePayloadPolicyTest,BoundedPayloadCaptureTest,SafePayloadEventWriterTest}.java`.

**서비스 계약:** `service-logging.enabled=false`, include-packages/include-methods/exclude-methods 빈 목록, require-annotation=true, payload-enabled=false. 활성화·패키지·메서드·annotation 모두 AND, exclude 우선이다. opt-in 업무 공개 메서드만 INTERNAL 하나와 같은 span/UUID invocation.id의 enter/exit 쌍을 만든다. 동기/throw/cancel/CompletionStage를 CAS로 마감하고 Stage 반환 시 exit하지 않는다. reactive opt-in은 거부한다. self-invocation/private는 명시 scope API로 처리하고 자동 보장하지 않는다. JobPairLogger와 dataset을 분리한다. 서비스별 고정 시그니처가 없으므로 실제 서비스 등록은 보류한다.

**안전 계약:** HTTP/서비스/이슈가 동일 policy/projector/registry를 사용한다. rule 두 종류 및 projector schema를 시작 시 검증하고 중복/충돌/미등록 projector·중첩/배열·금지 필드를 거부한다. 승인 scalar 출력도 schema로 재검증하며 하나라도 불일치하면 전체 생략한다. raw body/KV·toString/reflection·원문 fallback을 제공하지 않는다. 정책/파서/masker 부재는 fail closed다. 민감 필드는 성공/오류 모두 차단하고 query 기본 미기록, header는 승인 비민감 allowlist만 쓴다. 초기 승인 rule/schema는 빈 목록이다.

`safe-payload.enabled=false`, mode=ERROR_ONLY, max-body-bytes=8192, max-event-bytes=32768, max-depth=4, max-fields=32. byte 상한 증가는 거부하고 기존 4096 설정은 자동 확대하지 않는다. bounded tee로 request/response 캡처·파싱·출력 각각 UTF-8 8KiB를 지키며 초과 본문은 생략한다. 최종 JSON은 payload→선택 메타데이터 제거→필수 bounded envelope 재직렬화로 32KiB 이하를 유지하며 prefix/JSON 절단은 금지한다. `SafePayloadEventWriter`가 안전 결과와 고정 생략 코드만 전달하고 실제 정본 최종 방어는 W3 encoder에 둔다. 신규 보호 필드는 일반 KV로 기록하지 않는다.

**의존성·완료:** W1 snapshot API를 소비한다. W3가 제공할 typed 안전 결과 입력 API와 payload 정본 필드 결정을 먼저 합의한다. payload 정본 필드가 없거나 승인되지 않으면 raw 필드 추가 없이 payload 출력은 차단한다. 구 method/payload opt-in을 새 안전 경로로 자동 승격하지 않고 구 raw writer 우회도 막는다. message/stack/이슈 경계는 W3 통합 시험까지 완료 조건에 포함한다.

### W3 — v5/UI·비동기 envelope·공유 통합·발행 준비 (§5·§7·§8)

**독점 구역:** 아래 정확한 파일 및 시험만 소유한다. W1/W2 구역을 직접 수정하지 않고 필요한 수정을 해당 작업에 돌려준다.

- 기존 `J/logging/{CanonicalLogSnapshot,CommonJsonEncoder,TelemetryFieldRegistry,TelemetryRegistryGenerator,JobPairLogger}.java`.
- 기존 `J/core/{TelemetryEnvelope,TelemetryScope,ScreenMetadata,UiCodeResolver,TraceHeaders}.java`.
- 신규 `J/core/TelemetryTaskDecorator.java`, `T/core/{TelemetryTaskDecoratorTest,TelemetryEnvelopeContractTest,UiRootSpanContractTest}.java`, `T/logging/{CanonicalSafeOutputTest,RegistryV5ContractTest,JobPairLoggerContractTest}.java`.
- 기존 `R/common-platform/log-fields.v2.json`, `R/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- 기존 `pom.xml`, `parent/pom.xml`, `bom/pom.xml`, `lib/pom.xml`, `starters/starter-web/pom.xml`, `starters/starter-batch/pom.xml`, `samples/sample-api/pom.xml`, `samples/sample-batch/pom.xml`, `.github/workflows/publish-github-packages.yml`.
- 신규 `docs/contracts/g-common-trace-log-contract.md`, `docs/plans/g-common-0.6.0-acceptance-evidence.md`.

**v5/UI:** registry 내부 version=5, 출력 schema.version=2를 유지한다. invocation.id/policy.id/omitted.reason/truncated 네 필드는 protected=true, mdcInput=false로 typed writer만 쓴다. reason은 POLICY_DENIED/PARSE_ERROR/MASKER_UNAVAILABLE/SCHEMA_MISMATCH/BODY_TOO_LARGE/EVENT_TOO_LARGE다. 실제 필드 수는 계산해 94 예상과 대조하고 v4 이름/타입 및 v3→v4 추가 2·제거 1·screen/UI ID 의미 변경을 회귀 fixture로 보존한다. parent.id는 실제 lineage만 사용한다. message/stack/이슈에 민감 원문이 통과하지 않도록 정본에서 고정 메시지·허용 오류 코드/예외 class 및 bounded 문자열을 사용하고 throwable message/stack raw 출력을 막는다.

UI ID는 nonzero lowercase 16hex의 실제 모바일 action root span ID다. 여러 요청의 UI ID는 같고 각 CLIENT ID는 다르다. 형식 검증만으로 실제 root임을 증명했다고 주장하지 않는다. APP/MEM/BO system별 catalog resolver를 명시 선택하고 screen.id+version이 유효할 때만 힌트를 받는다. 잘못된 힌트는 생략하며 요청 거부/internal_id 복구를 하지 않는다. 공통은 검증·resolver API를 제공하고 모바일 생성/override 일치 증명은 서비스 담당 인수 입력이다.

**비동기/batch envelope:** opt-in decorator가 OTel Context와 불변 snapshot을 캡처/복원/finally 해제하며 MDC 복사나 중복 span 생성으로 대체하지 않는다. 독립 scheduler root/원인 link, 새로운 업무 CONSUMER/INTERNAL, OFF/NO_CLAIM admission 및 claim 실패 분리를 명시한다. JobPairLogger에는 execution.id를 먼저 설정하고 소유 span만 종료한다. envelope는 owner 검증→setParent, 기존 8192자 저장 상한을 유지한다. 무효/없음/owner 불일치는 새 root와 고정 사유이며 retry는 원 인입 envelope를 부모로 새 attempt/execution 식별자를 사용한다.

**저장소 밖 연계는 명세만:** MEM afterCommit 등록 시 INTERNAL envelope 캡처, rollback 미전송, callback 복원 후 agent CLIENT→BATCH 수신 SERVER에서 인증/환경/dispatchId/멱등 검증 후 envelope 재캡처→single 예약과 schema version 동일 transaction 저장→scheduler가 RunEngine claim까지 전달→CONSUMER→owner CLIENT/SERVER→메일 INTERNAL/CLIENT 순서다. dispatchId를 trace로 바꾸지 않는다. 중복은 완료만 기록하고 중복 예약/발송하지 않는다. 이 저장소는 EmailEventService/JdbcRunStore/MEM callback을 수정할 수 없으므로 공통 API/fixture까지만 완료로 판단하며 실제 영속 chain 인수는 외부 의존성으로 남긴다.

## 4. 의존성 및 공유 파일 조정 순서

W1·W2는 소스 쓰기 구역이 겹치지 않는다. W3의 파일은 여러 기능이 요청하는 **공유 파일의 단일 소유 구역**이다. W1/W2가 imports/POM/registry/snapshot에 직접 편집하지 않으므로 중복 쓰기가 없다.

1. G0 해제 → W3 첫 계약 커밋 → W3가 W1/W2와 snapshot/accessor·typed payload 결과·invocation 보호 입력 API를 고정한다. SDK는 agent-extension에만 두는 의존성 경계를 확정한다.
2. W3가 `lib/pom.xml` 시험 fixture/필요 의존성, root 모듈 등록을 준비한다. agent-extension/pom.xml의 내부 구현은 W1만 소유한다. extension artifact 좌표·발행 범위는 PM 확정 전 예약하지 않는다.
3. W1 snapshot/accessor → W2 서비스/안전 projector 순서로 연결한다. W3는 registry v5 → CanonicalLogSnapshot lineage/typed 안전 입력 → CommonJsonEncoder 크기·원문 방어 → UI/envelope 순으로 편집한다. W1/W2 단위 시험은 해당 API가 준비된 뒤 실시한다.
4. W1/W2 자동구성이 확정된 후 W3가 imports를 한 번에 등록한다. 비웹 client 자동구성과 MVC 기존 빈의 중복 등록을 제거하는 수정은 W1이 담당한다. W3가 imports 최종 중복·조건을 대조한다.
5. 기능 통합 후 W3가 parent/BOM/lib/starter-web/starter-batch 및 root/sample 참조를 0.6.0으로 정렬한다. parent 상속·BOM 관리 버전·starter 전이 의존성을 모두 대조한다. 그 뒤 workflow의 대상 시험 목록을 수정하고 증거 문서를 고정한다. 기존 API 삭제/서명 변경은 요구하지 않으며 Security/Idempotency 결합점은 유지한다.

## 5. 계약 항목 ↔ 파일 ↔ 최소 시험 대응표

시험명은 향후 신규 시험 또는 명시된 기존 파일의 변경 대상이다. **이번에는 전부 미실행**이며 각 행의 이유가 향후 해당 시험을 실행하는 이유다. 변경 클래스 단위와 §9 인수 범위만 허용하고 같은 입력 pass 재실행/전체 회귀를 계획하지 않는다.

| 계약 항목 | 담당·대상 | 최소 시험과 필요한 이유 / 완료 증거 |
|---|---|---|
| §1 W3C·실제 lineage·SDK 부재/한도 | W1 processor/accessor/snapshot/extension; W3 CanonicalLogSnapshot | SpanLineageSpanProcessorTest, TelemetryContextSnapshotTest: root/remote/무효 W3C/async의 exporter parent와 로그 일치, onEnd 제거·한도·agent 부재 시 생략을 검증 |
| §1 기본차단·agent 단일 소유 | W1 TrustedDestinationPropagationCustomizer, HeaderPropagationInterceptor, TraceFilter | TrustedDestinationPropagationCustomizerTest 및 agent fixture: 내부 승인/외부 차단/정확 host/와일드카드 거부/baggage 차단, 지원 Rest/JDK 계측별 실제 wire와 exporter로 중복 span·주입 없음 확인 |
| §2 서버 완료·필터 순서 | W1 AccessLogFilter/AccessLogger/HttpCompletionScope | HttpCompletionContractTest, HttpCompletionScopeTest: 2xx/3xx success, 400/401/403/404/405/409/413 및 기타 >=400 failure/HTTP_status, 미처리 500, 검증 실제 status, 인증 신원 출처, route /{unmatched}·query 없음·ns·UUID·필수 envelope 확인 |
| §2 단절/IO/cancel/async | W1 동일 scope/필터 | HttpCompletionScopeTest: 확인 단절 499/CLIENT_DISCONNECTED, 기타 IO 실제 유효 status 또는 500/HTTP_IO_ERROR, 취소 고정 코드·응답 없을 때 status 생략, onComplete/timeout=504/error=500/onStartAsync 재등록·경합에도 1건; 캡처 snapshot 사용 |
| §2 outbound·비웹·자체 서버 | W1 interceptor/ObservedJdkHttpClient/비웹 auto config/handler/admission | ObservedJdkHttpClientTest, JdkHttpServerObservationHandlerTest 및 HttpCompletionContractTest: sync/sendAsync response/오류/timeout/cancel과 retry attempt별 완료 1건·4xx failure, MVC 없는 활성화, agent/manual 상호배타·수락 rejection 503·이전 rejection 지표 경계 확인 |
| §3 INTERNAL 쌍·선정·설정 | W2 aop 신규 클래스 | ServiceMethodLoggingAspectTest, ServiceInvocationScopeTest: annotation AND allowlist/exclude 우선/default off, INTERNAL 실제 span·UUID/동일 span enter/exit, throw/cancel/Stage 완료 CAS, reactive 거부·민감 범주 제외·self-invocation 보장 경계 확인 |
| §4 등록 API·deny·민감정보 | W2 registry/projector/policy 및 기존 writer; W3 snapshot | PayloadPolicyRegistryTest, SafePayloadPolicyTest, CanonicalSafeOutputTest: 빈 승인표, 미등록/충돌/알 수 없는 필드·타입/중첩/배열 거부, 파싱·masker 실패 전체 생략, 성공/오류/이슈/message/stack에 비밀·PII 없음과 raw 우회 차단 확인 |
| §4 bounded capture/8KiB·32KiB | W2 bounded wrappers/event writer; W3 encoder | BoundedPayloadCaptureTest, SafePayloadEventWriterTest, CanonicalSafeOutputTest: UTF-8 경계 직전/정확/초과, 스트리밍 메모리 상한, 기존 4KiB opt-in 유지, 상한 초과 설정 거부, 필수 envelope도 bounded, payload→선택 메타 제거 및 유효 JSON·생략 reason/truncated 검증 |
| §5 v3/v4→v5 및 보호 | W3 registry/generator/typed snapshot | RegistryV5ContractTest: version=5/schema=2, 기존 타입/의미 유지·신규 네 필드·실제 개수, KV/MDC 덮어쓰기 차단, payload 정본 필드 미승인 시 출력 차단 확인 |
| §5 UI root ID·system resolver | W3 ScreenMetadata/UiCodeResolver | UiRootSpanContractTest: nonzero 16hex, 같은 UI root/다른 CLIENT fixture, screen+version·system 선택, 무효 힌트 생략; 실제 모바일 root 생성/override 증거는 외부 담당 입력 필요 |
| §6 mapping·소비 질의 | W3 증거 문서; A17 외부 의존 | A17이 keyword/boolean/long(ns) explicit template·타입 충돌·동적 payload 금지·rollover·완료 분모/구신 분리 질의를 대조. 운영 template 미제공으로 공통 로컬 시험이 대신 통과시킬 수 없음 |
| §6 p95 | W3 증거 문서; A17 외부 의존 | 동일 artifact/agent/JVM/자원/DB/목적지/혼합/샘플링 고정, 5분 warmup+15분 부하 교대 3회, p95 중앙값 증가 <=5%(기준 0 판정불가). 정상/오류/async/8KiB 포함·완료율/drop/CPU/heap/GC/bytes/s 및 histogram 보관. 문서 단계 실측 없음 |
| §7 task/job/admission | W3 decorator/JobPairLogger/TelemetryScope | TelemetryTaskDecoratorTest, JobPairLoggerContractTest: Context/snapshot 복원·finally 해제, span 중복 없음, execution.id/소유 종료, root/link·OFF/NO_CLAIM/claim 실패 admission 분리 fixture |
| §7 single 영속 chain | W3 TelemetryEnvelope 및 외부 MEM/BATCH 의존 | TelemetryEnvelopeContractTest: owner/8192자/무효·없음 root·새 retry span. 외부 인수는 afterCommit/rollback·동일 transaction·scheduler claim 전달·성공/실패/중복/재시도/invalid 전 구간 동일 trace 및 exporter 실제 parent/log parent 일치; 공통 fixture만으로 종단 완료 주장 금지 |
| §8 버전·호환·적용 순서 | W3 POM/workflow/증거 문서 | 변경 POM 의존성 대조 및 Publish 1회의 계약 해당 consumer 시험: 0.6.0 정렬·발행물 소비·기존 facade/Security/Idempotency 유지 확인. GS 소비자 바이너리 전체 호환은 미확인 |
| §9 통합 인수 | W1/W2 단위 증거 + W3 증거 문서 | 위 행을 §9 체크리스트로 취합하며 누락을 완료로 표기하지 않음. sample-api는 아래 Publish 워크플로 1회로만 확인 |

## 6. 통합 후 발행·승인 계획 (이번 미실행)

현재 `.github/workflows/publish-github-packages.yml`은 일반 버전에서 전체 clean verify 및 sample-api 전체 시험을 실행한다. 이 동작을 그대로 0.6.0에 사용하면 본 brief의 최소 검증 원칙과 충돌하므로, W3가 0.6.0 경로에 변경 클래스/위 표 §9 해당 시험만 선택하도록 먼저 준비한다. 기존 rc 분기를 재사용했다고 간주하지 않는다. sample-api에 있는 기존 `OtelFieldRegistryContractTest`, `OtelRegistryGenerationContractTest`, `ScreenMetadataLoggingTest`, `AccessLogFilterTest`, `PayloadLoggingFilterTest` 중 실제 변경 계약에 필요한 케이스만 고정하고 새 lib/agent 시험과 중복 입력을 분리한다. 로컬에서 sample-api 시험은 실행하지 않는다.

모든 단위 계약 및 필수 외부 입력이 갖춰지고 PM이 별도 발행 승인한 뒤, 0.6.0 BOM/lib/starter-web/starter-batch(상속 parent 포함)의 좌표/참조를 최종 대조하고 **통합 커밋 하나에 대해 Publish 워크플로를 1회만** 수행하는 계획이다. 그 실행에서 발행 후 깨끗한 consumer Maven 저장소로 sample-api의 고정 영향 시험만 1회 수행한다. 이미 pass한 입력의 반복·전체 회귀·강제 push·기존 태그 이동은 금지한다. 실패 시 PM에게 근거를 돌려주고 이 계획만으로 재발행을 허가하지 않는다. 이번에는 push/태그/workflow/Publish 어느 것도 실행하지 않는다.

증거에는 확정 GitHub v0.5.2 기준 커밋, 0.6.0 통합 커밋, agent 버전/계측 모듈/설정, workflow run, 발행 좌표 및 정식 **8SHA**를 기록한다. 8SHA가 가리키는 정확한 8개 산출물 목록·해시 알고리즘/표현 규칙은 입력에 없으므로 PM 제공 목록으로 하나씩 대응하고 임의 목록·SHA를 만들지 않는다. 태그-커밋-패키지-소비 시험 연결 및 8개 전부 일치 확인 후 PM이 완료를 판단한다. 신규 agent artifact가 기존 8개에 포함되는지 여부도 사전 확정 대상이다.

후속 서비스 적용 순서는 공통+agent+registry/template 준비 → GS-BO 제한 적용 → BATCH app/runtime 동시 정렬 → MEM → APP backend/mobile다. 이메일은 BATCH 수신/저장 지원 후 MEM 송신을 켠다. 각 단계 HTTP 완료/부모/payload/p95 승인 후 다음으로 넘어가며 rollback은 이전 BOM·agent 설정·질의를 묶고 저장 envelope 구 reader 호환을 먼저 확보한다. 이 순서는 외부 담당에게 넘길 의존성 설명이며 이번 GS 수정 권한은 아니다.

## 7. 부족한 입력과 확인하지 못한 것

- v0.5.2 확정 GitHub 커밋/발행 pass/정식 8SHA 및 산출물 목록: G0 차단 입력이다.
- agent 고정 버전·지원 계측·최종 destination 접근 API·SDK/accessor 클래스 로더 연결·runtime 설정/배치 설치 경로: 설치 사실과 extension 실행 가능성 미확인. 고정 버전 확인 전 기술 SPI를 확정하지 않는다.
- 서비스 opt-in 정확한 시그니처·payload 승인 schema/정본 출력 필드·APP/MEM/BO catalog 입력·모바일 root/override 증거: 추정 등록하지 않는다. 기본 deny/보류다.
- MEM/BATCH 외부 수정 구역·DB envelope column/schema migration·기존 reader 호환·실제 영속 성공/실패/중복/재시도 chain: 타 저장소를 보지 않았으며 공통 API 시험과 별도 인수다.
- A17 현행 OpenSearch template/운영 질의/p95 실측 및 artifact: 미제공. 숫자나 운영 반영 사실을 만들지 않는다. TODO 서비스는 등록/고정 입력 전 제외한다.
- 실제 컴파일·시험·agent 실행·Publish·배포·성능·운영 mapping은 이번 검증 대상이 아니다.

## 8. 이번 설계 자체 대조

검증 이유: 문서 완료 조건인 계약 항목 누락과 쓰기 구역 중복만 확인하기 위함이다.

첨부 §1~§9와 상위 9항목을 위 대응표에 정독 대조했다. §1·§2=W1, §3·§4=W2, §5·§7=W3, §6·§8·§9=W3 취합/외부 인수로 모두 대응했다. 필수 agent 소유권·명시 주입 금지·v0.5.2 게이트·첫 계약 커밋·0.6.0 정렬·Publish 1회·8SHA 확인을 포함한다. W1/W2의 패키지 및 정확 파일과 W3 공유 파일 목록은 교집합이 없고, 공유 연결은 §4의 단일 소유 순서로 조정한다. 신규 processor의 최종 경로는 계약 FQCN을 유지하는 observability 패키지다. 이번 변경은 `docs/plans/g-common-0.6.0-implementation-plan.md`뿐이다. 시험·빌드·전체 회귀는 실행하지 않았다.

## 실행 승인에 따른 기준 정정

출처: 계약 0534.a01 / 설계 0539.a01 / REQ-261021-02 실행 brief. 기존 G0와 별도 발행 승인 게이트는 현재 brief로 대체됐다. C1 통합 기반은 `d2e75a2f4e694440a0926b47e03b3c76ca0bc312`이며 GitHub main에 빠른 진행 반영했다. v0.5.2 run 39494450268은 completed/cancelled, head `2e666237731f21ea18803b73c10f9b3210bab7df`다. 계약 원문은 변경하지 않았다.
