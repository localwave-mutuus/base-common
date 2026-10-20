# G-COMMON C3 3단계 — registry v5/UI·비동기 envelope

기준: base-common `d2e75a2f4e694440a0926b47e03b3c76ca0bc312` 분리 HEAD. REQ-261021-02 / attempt 261021-0710-a13-ex.a01.

입력 `/mnt/disk/00.workspaces/b04.mutuus-operation/artifacts/261021-0635-a13-ex/261021-0635-a13-ex.a01/patch.diff`를 `git apply`로 적용했다. 입력 62파일을 누적 patch에 보존한다. 원본·다른 저장소·조직 장부는 수정하지 않았다. 계약/0539 계획/기존 step1 보고서는 첨부 상태를 유지한다. patch 적용 시 기존 trailing whitespace 1줄 경고만 있었고 적용 실패는 없었다.

## 명령·실행 근거

명령 공통 접두사/접미사:
```sh
bash ./mvnw -B -pl lib -am '-Dtest=<아래 선택>' -Dsurefire.failIfNoSpecifiedTests=false test
```

| 명령 | 선택 | 로그(TEAM_OUT_DIR) | 결과 |
|---|---|---|---|
| C23-A | ServiceMethodLoggingAspectTest,SafePayloadPolicyTest,BoundedPayloadCaptureTest,RegistryV5ContractTest,CanonicalSafeOutputTest,TelemetryEnvelopeContractTest,UiRootSpanContractTest | a13-c3-step23-tests.log | 18 tests, failure/error/skipped=0, BUILD SUCCESS, 2026-10-20T22:15:54Z |
| C23-B | ServiceProxyPayloadContractTest,ServiceAutoConfigurationContractTest,SafePayloadFilterContractTest | a13-c3-step23-additional.log | 3 tests, failure/error/skipped=0, BUILD SUCCESS, 2026-10-20T22:18:02Z |
| C23-C | BoundedEnvelopeSafetyTest,EnvelopeRetryAdmissionTest | a13-c3-step23-boundary.log | 3 tests, failure/error/skipped=0, BUILD SUCCESS, 2026-10-20T22:19:29Z |

검증 이유 A: 이번 구현의 §3/§4/§5/§7 최소 계약 입력을 처음 확인했다. B: 정적 대조 후 제한한 실제 Spring 프록시 대상, 비동기 안전 DTO, 부분 읽기·비JSON 차단은 A에 없는 신규 입력이다. C: 낮춘 이벤트 상한·오류 코드 차단·동일 attempt 거부·admission도 A/B에 없는 신규 입력이다. 이미 pass한 같은 입력을 다시 실행하지 않았다. 각 실행은 변경 소스를 다시 컴파일하고 registry 산출물을 process-classes에서 생성했다. 마지막 소스 변경은 C 전에 끝났고 이후 보고서·patch만 작성했다. 총 24개 신규 입력 통과이며, A/B의 결과를 C 시점 재실행 결과로 바꾸어 주장하지 않는다.

전체 회귀·sample-api·기존 HTTP/agent pass 입력·교차검토·최종 통합검증은 실행하지 않았다. 발행 버전/워크플로는 변경하지 않았으며 현재 0.5.2 좌표를 유지한다. v0.6.0 정렬·최종 통합·Publish는 후속 단일 작업이다.

## 계약 ↔ 파일 ↔ 명령 ↔ 결과

경로의 J는 `lib/src/main/java/ai/mutuus/common`, T는 `lib/src/test/java/ai/mutuus/common`, R은 `lib/src/main/resources`다.

| 계약 | 파일 | 명령·근거 |
|---|---|---|
| §5 내부 registry=5/출력 schema=2, 기존90+4=94, 보호 typed 입력 | R/common-platform/log-fields.v2.json; J/logging/{TelemetryFieldRegistry,CanonicalLogSnapshot}.java; T/logging/RegistryV5ContractTest.java; test resources/common-platform/log-fields.v4.fixture.json | C23-A: HEAD에서 가져온 독립 v4 fixture 90개 이름/타입 보존, 추가4·protected=true/mdcInput=false/structuredInput=false, reason enum/boolean 타입 거부. process-classes generator v5 산출 성공 |
| §5 v3→v4 의미 보존 | 계약/기존 v4 fixture 및 위 시험 | internal_id 부재, element/action 존재를 고정. v3 원본 blob의 신규 재조회/전체 비교는 하지 않았고 제공된 계약 비교(추가2·제거1)를 따른다 |
| §5 UI root span 형식/명시 system 선택 | J/core/{UiCodeResolver,ScreenMetadata}.java; T/core/UiRootSpanContractTest.java | C23-A: BO system/version/catalog 결합, nonzero lowercase16hex, 0/대문자/traceID/UUID 거부, invalid hint 생략 |
| §4/§5 최종 정본 typed 보호·원문 차단·크기 | J/logging/{CanonicalLogSnapshot,CommonJsonEncoder}.java | C23-A CanonicalSafeOutputTest 및 C23-C BoundedEnvelopeSafetyTest. 일반 KV/MDC/완료 Map으로 새4필드 위조 불가; typed 정책 및 private OTel Context 출처만 채택. bytes 초과는 payload→선택 메타데이터 제거 후 bounded 필수 envelope 재직렬화 |
| §7 opt-in 비동기 capture/restore/finally, MDC 대신 Context | J/core/TelemetryTaskDecorator.java; 기존 observability/TelemetryContextSnapshot 소비; T/core/TelemetryEnvelopeContractTest.java | C23-A: task 예외에도 호출 thread의 이전 context 복원, span 추가 없음. decorator는 자동으로 모든 executor에 설치하지 않음 |
| §7 저장 codec/8192자, owner 검증→CONSUMER setParent, execution 먼저 설정 | J/core/TelemetryEnvelope.java; T/core/{TelemetryEnvelopeContractTest,EnvelopeRetryAdmissionTest}.java | C23-A: 저장/역직렬화 동일, ingress 종료 뒤 scheduler context에서도 원 ingress parent·trace로 복원, CONSUMER 실제 exporter 부모 대조, beginJob의 execution ID 일치. schemaVersion/owner validation API, 무효/없음/owner 불일치 root·고정 사유. 저장8193자 거부 |
| §7 retry 새 span/attempt/execution, admission 분리·소유 span 종료 | J/core/TelemetryEnvelope.java; J/logging/JobPairLogger.java | C23-A: first/retry 각 새 execution/span, 원 ingress parent 유지, 빌린 CONSUMER는 ConsumerScope가 종료. C23-C: 원 attempt 재사용은 ENVELOPE_INVALID root, OFF/NO_CLAIM/CLAIM_FAILED는 job.admission만·execution pair/span 없음 |

## 공통 저장·소비 API 및 실제 적용 경계

1. 검증된 ingress SERVER context에서 `TelemetryEnvelope.captureCurrentVerified(operationId, attemptId)` 호출 후 `toStorageString()`과 `TelemetryEnvelope.VERSION`을 예약 row와 같은 transaction으로 저장한다. JSON codec는 고정 키/타입/중복/버전 및 8192자 한도를 검사한다. 이 저장소의 starter-batch는 lib 전이 의존성으로 API를 제공하고 별도 SDK/provider를 설치하지 않는다.
2. scheduler는 저장된 **원 ingress** string/version/owner를 보관한 채 claim까지 전달한다. 인증·저장 권한 검증으로 얻은 validatedOwner를 `resumeStored(stored,schemaVersion,storageOwner,validatedOwner,newAttemptId)`에 전달한다. header/dispatchId를 owner/trace로 바꾸지 않는다. owner 불일치/없음/무효는 원문 없이 각각 ENVELOPE_OWNER_MISMATCH/ENVELOPE_MISSING/ENVELOPE_INVALID 사유의 새 CONSUMER root다.
3. `try (StoredConsumer consumer=...) { try (var job=consumer.beginJob(...)) { ...; job.success(); } }` 순서다. ConsumerScope 생성 전에 새 execution.id가 설정되므로 JobPairLogger가 임의 보완하지 않는다. retry는 같은 원 ingress blob과 새 attempt ID를 쓰며 새 execution/span을 생성한다. 원 blob의 attempt ID와 같은 값은 invalid로 거부한다. attempt 중복 발급 방지는 저장 owner의 트랜잭션/claim 책임이다.
4. OFF/NO_CLAIM/claim failure는 JobPairLogger.admission으로만 기록한다. 승인된 실행에만 begin/beginInCurrentSpan을 연다. beginRootLinked는 기존 독립 scheduler/root 원인 link API를 유지한다.

**외부 영속 chain은 미인수다.** MEM afterCommit/rollback 전송 제어, BATCH EmailEventServer 인증·환경·dispatchId·멱등, EmailEventService single INSERT와 schemaVersion의 실제 동일 DB transaction, JdbcRunStore/RunEngine claim 전달, owner execute CLIENT/SERVER→메일 INTERNAL/CLIENT, 중복 예약/발송 억제는 이 저장소 밖이다. brief가 금지한 타 저장소 수정/실접속을 하지 않고 공통 codec/복원/fixture까지만 구현했다. 인메모리 저장 string roundtrip을 DB 종단 성공으로 부르지 않는다. 실패 job·중복 인입·rollback의 실제 외부 side effect도 미확인이다.

UI root ID는 모양만 검증한다. `ScreenMetadata.fromHeaders(system,reader)` 또는 명시 `mutuus.ui.system` JVM 설정을 사용한다. APP/MEM/BO를 고르지 않으면 힌트를 수용하지 않으며 등록 catalog에 없는 화면을 추정하지 않는다. 모바일이 실제 action root를 생성하고 여러 CLIENT가 동일 UI ID를 보내는지, override가 실제 root와 같은지는 모바일/서비스 담당 인수 대상이다. CLIENT ID를 UI ID로 바꾸는 공통 구현은 추가하지 않았다.

## 미완료·공유파일/충돌주의

- 외부 승인 schema/실제 서비스·메일 DB chain/모바일 실제 root 증명과 §1·§2 잔여, §6 운영·성능 실측은 미완료다. 이번 로컬 fixture 결과를 배포 승인으로 해석하지 않는다.
- 공유파일은 registry, CanonicalLogSnapshot, CommonJsonEncoder, lib/pom.xml, AutoConfiguration.imports다. 2단계와 한 좌석에서 조정했다. 기존 JobPairLogger begin/beginInCurrentSpan, TelemetryEnvelope 기존 생성자·capture/extract/openConsumer 서명은 유지한다. 새 저장 codec/resume/admission API를 추가했다.
- 기존62개 입력 파일과 새 구현/시험/보고서를 모두 누적 patch로 제출한다. agent-extension/W1 HTTP 파일·§6 설계·계약·0539 계획·step1 보고서의 기존 누적 내용은 보존한다. 버전/발행/배포/파이프라인은 이번에 변경하지 않았다. 최종 통합검증·Publish·정식8SHA 전달은 후속 별도 작업이다.

## 이슈대처 후보

플랫폼 첨부 한도·안내 브랜치 부재에 대한 조치/확인 명령/재발 방지는 step2 보고서의 같은 절에 기록했다. 이번에는 명시 산출물 접근/적용에 성공했고 새 차단 오류나 시험 실패는 없었다.
