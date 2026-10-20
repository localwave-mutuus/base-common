# REQ-261021-02 — v0.6.1 C3 근거표

대상: base-common `e64c545f4d5b6ac6cc938e8aa3f82cc1040d1934` / 작성 attempt `261021-0818-a13-ex.a01`。

기존 결과를 대응한 표다. 기존 pass·버전 결정을 재판정하거나 시험을 재실행하지 않았다. C3 전체 인수·실서비스 가동 승인을 뜻하지 않는다.

## 증거 위치

- S1: [0602·0635 HTTP/agent 보고서](g-common-c3-step1.md), 특히 「최소 검증 기록」과 「0635 후속」.
- S2: [0710 서비스·PayloadPolicy 보고서](g-common-c3-step2.md), C23-A/B/C.
- S3: [0710 envelope 보고서](g-common-c3-step3.md), C23-A/B/C.
- P: `/mnt/disk/00.workspaces/b04.mutuus-operation/artifacts/261021-0722-a13-ex/261021-0722-a13-ex.a01/patch.diff`의 `docs/reports/g-common-0.6.0-publish.md` 사후 「전달 요약 및 정식 8SHA」. 같은 디렉터리 `last.md`, `out-files.md`도 발행 결과를 보존한다. 현재 태그의 [발행 보고서](g-common-0.6.0-publish.md)는 사후 성공 요약 이전 상태이므로 P와 구별한다.
- R: `/mnt/disk/00.workspaces/b04.mutuus-operation/artifacts/261021-0747-a18-ex/261021-0747-a18-ex.a03/patch.diff`의 `docs/ci-cache/261021-0747-a18-ex.a03-receipt.md` 및 `docs/ci-cache/261021-0747-a18-ex.a03-evidence.json`. 같은 디렉터리 `last.md`에도 결과가 있다. 다른 저장소 코드는 읽지 않았으며 전달 결과 문서만 참조했다.
- 아래 로그 이름은 각 보고서에 기록된 해당 attempt의 TEAM_OUT_DIR 상대 위치다. S1/S2/S3의 수치는 기존 보고서에서 확인했으며 원시 로그를 새로 실행해 만든 수치가 아니다.

## C3-1 — HTTP 완료·오류 기록

| 항목 | 시험 클래스·메서드 | 실제 통과 수·시도 | 확인 가능한 증거 | 범위·부족 근거 |
|---|---|---|---|---|
| HTTP status/예외·완료1회·ID 보호 | HttpCompletionContractTest#responseStatusesValidationAndControllerRoutesCompleteOnce, #restartedAsyncRegistersAgainAndCommittedIoKeepsStatus, #completionMetadataCannotReplaceActualLineageOrRequestId; ObservedJdkHttpClientTest; JdkHttpServerObservationHandlerTest 등 | 0602 서로 다른 신규 메서드 총16 pass; 이 행의 클래스별 독립 합계는 근거 없음 — 원시 surefire 결과를 대응해야 채워짐 | S1 「최소 검증 기록」 c3-step1-selected-fix.log(6), outbound-exporter(2), additional-http(3), installation(1), lineage-protection(1), minimum-fix의 선행 pass3 | 200~503 상태, 일반 IO/timeout/async 경합·writer 실패 포함. 실제 client disconnect 인수는 근거 없음 — 실제 container 단절 증거 필요 |
| JDK 최종 수명·양방향 redirect·hidden retry·잔존 헤더 | TransportAttemptContractTest#bothRedirectDirectionsAndHiddenRetryRequireDestinationPolicyAndAttemptCompletions, #previouslyPresentHeadersMustNotSurviveUnapprovedRedirect | 0635 2 pass | S1 a13-0635-jdk-wire.log | wire/CLIENT/완료 각6, 승인4/4, 누출0, 잔존헤더0 |
| HTTP/2 redirect·REFUSED_STREAM retry | Http2TransportContractTest#http2RedirectAndRefusedStreamRetry | 0635 1 pass | S1 a13-0635-h2.log | wire/CLIENT/완료 각6, 승인4/4, 누출0 |
| Spring Simple/JDK transport | SpringTransportContractTest#simpleFactoryRedirectAndHiddenRetry, #restClientRemovesPreviouslyPresentHeadersAndAvoidsOuterCompletion, #jdkFactoryHasAgentAttemptCompletionWithoutInterceptorDuplicate | 0635 각1, 총3 pass | S1 a13-0635-spring-retry-fix.log, spring-residual.log, spring-jdk-token-fix.log | 각 wire/CLIENT/완료6. 앞선 실패 실행은 pass에 합산하지 않음 |
| 비승인 목적지 retry | DeniedTransportRetryTest (개별 메서드명은 근거 없음 — 시험 원본·로그 대응 필요) | 0635 2 pass | S1 a13-0635-denied-compile-fix.log | 각 wire2·전파0·누출0·CLIENT/완료2 |

0635의 서로 다른 입력은 총8 pass다. 고정 agent 2.28.0/JDK 21.0.12.1 및 명시 transport 설정의 로컬 wire 근거다. 후속 token 변경 뒤 앞선 pass를 재실행한 결과로 바꾸지 않는다. S1의 §1·§2 전체 인수 미완료 판단을 유지한다. 실제 서버 적용·CORS 순서·body 소비 이후 오류 등은 근거 없음 — 소비 서비스별 증거가 필요하다.

## C3-2 — 서비스 함수 쌍·PayloadPolicy

| 항목 | 시험 클래스·메서드 | 실제 통과 수·시도 | 증거 | 부족 근거·후속 보강 |
|---|---|---|---|---|
| opt-in·INTERNAL enter/exit·동기/throw/cancel/CAS·context 복원 | ServiceMethodLoggingAspectTest#selectionIsOptInAndExcludeWins, #syncExceptionAndCancelEachHaveOneInternalPair, #completionStageWaitsAndRestoresThreadContext, #asyncCancellationAndFailureCloseExactlyOnce, #reactiveOptInRejectedBeforeBusinessExecution, #explicitScopeCasAndFinallyRestore | 0710 6 pass (C23-A에 포함) | S2 a13-c3-step23-tests.log | 실제 서비스별 opt-in 목록·적용은 근거 없음 — 승인 시그니처와 소비 서비스 결과 필요 |
| policy 봉인·기본 차단·schema·UTF-8/크기 경계 | SafePayloadPolicyTest#failClosedAndSuccessfulSchemaProjection, #rejectsSensitiveNestedAndUnknownOutputAndRegistrations, #utf8InputBoundariesAndParseFailures, #byteLimitsCannotBeRaisedAndLegacyLimitRemains4096; BoundedPayloadCaptureTest | C23-A 전체18 pass; 위 클래스별 로그 통과 수는 근거 없음 — 원시 결과 세분 필요 | S2 C23-A | 8191/8192/8193, 정책/미승인 값 차단. 정본 projection 내용 출력은 의도적으로 차단된 상태 |
| 실제 Spring proxy·기본 off·부분읽기/비JSON 차단 | ServiceProxyPayloadContractTest, ServiceAutoConfigurationContractTest, SafePayloadFilterContractTest (개별 메서드명은 근거 없음 — 원시 결과 대응 필요) | 0710 C23-B 3 pass | S2 a13-c3-step23-additional.log | 승인 DTO/schema·실제 앱/이슈 적용은 근거 없음 — 소비자별 승인 및 결과 필요 |
| 최종 정본·오류 코드·낮춘 envelope 한도 | CanonicalSafeOutputTest(C23-A), BoundedEnvelopeSafetyTest 및 EnvelopeRetryAdmissionTest(C23-C); 개별 메서드 대응은 아래 C3-3 외 근거 없음 — 원시 결과 필요 | C23-C 전체3 pass | S2 a13-c3-step23-boundary.log | C23-A/B/C 총24 신규 입력. 표 사이 중복 인용을 별도 통과로 합산하지 않음 |

## C3-3 — starter-batch traceparent

| 항목 | 시험 클래스·메서드 | 실제 통과 수·시도 | 증거 | 부족 근거·후속 보강 |
|---|---|---|---|---|
| ingress 저장 roundtrip·새 execution/span·원 parent 유지·무효 root·decorator finally | TelemetryEnvelopeContractTest#storageRoundTripRetryKeepsIngressParentAndFreshExecution, #invalidMissingOwnerMismatchAlwaysStartNewRoot, #decoratorRestoresSnapshotAndThreadContextEvenOnFailure | 0710 C23-A 전체18 pass에 포함; 이 클래스의 개별 통과 수 원시 증거 없음 — surefire 대응 필요 | S3 a13-c3-step23-tests.log | exporter parent fixture·문자열 저장 codec 근거. 실제 DB transaction 증거로 해석하지 않음 |
| 재사용 attempt 거부·admission 분리 | EnvelopeRetryAdmissionTest#reusedAttemptIsInvalidAndNoClaimNeverStartsExecutionPair | 0710 C23-C 전체3 pass에 포함; 개별 결과 수 근거 없음 — surefire 대응 필요 | S3 a13-c3-step23-boundary.log | OFF/NO_CLAIM/CLAIM_FAILED는 job.admission만 |
| starter-batch 제공·패키징 | 시험 메서드 해당 없음: 0722 -DskipTests package | 6모듈 BUILD SUCCESS, 시험 통과 수 해당 없음(시험 생략) | 발행 보고서 final-integration-build.log 및 P | lib 전이 API 제공·별도 SDK 설치 없음. starter runtime traceparent 종단 증거는 근거 없음 — 실제 배치 ingress→저장→claim→CONSUMER/CLIENT 증거 필요 |
| MEM/BATCH 실제 transaction·claim·메일 실행 | 근거 없음 — 소비 서비스별 실행 증거 필요 | 근거 없음 — A18 0810 후속 결과 필요 | S3의 외부 영속 chain 미인수 기록 | rollback/중복 억제·owner·메일 CLIENT/SERVER/INTERNAL 종단은 보강 대상 |

## C3-4 — 발행·정식 SHA·CI 캐시

| 항목 | 시험 클래스·메서드 | 실제 결과·시도 | 증거·한계 |
|---|---|---|---|
| v0.6.1 발행 | sample-api OtelRegistryGenerationContractTest, 개별 메서드명 근거 없음 — run 시험 상세 필요 | 0722 소비 시험1개 success, Publish completed/success | P, [run 39508417303](https://github.com/localwave-mutuus/base-common/actions/runs/39508417303); main/태그 e64c545f4d5b6ac6cc938e8aa3f82cc1040d1934 |
| 실제 내려받은 정식8파일 | 시험 메서드 해당 없음, SHA256 대조 | 0722 8개 다운로드; lib registry version=5/94필드, POM5개 0.6.1 | P의 published-0.6.1/sha256.json 및 아래 표 |
| 지정 build/gcom/v0.6.1-sha.md | 해당 없음 | 근거 없음 — 현재 사본과 지정 read-repo 경로에 파일 없음 | PM이 지정 파일을 제공하면 P/R의 8 SHA와 직접 대응 가능. 아래는 P에서 확보한 값이며 해당 파일을 읽었다고 주장하지 않음 |

v0.6.0은 게시 후 소비 시험 실패(run 39507892246); 수정된 v0.6.1 성공이라는 기존 결정을 보존한다. 버전 재결정·게시·덮어쓰기는 수행하지 않았다.

| 좌표 | 파일명 | SHA256 |
|---|---|---|
| `ai.mutuus.common:common-platform-parent:pom:0.6.1` | `common-platform-parent-0.6.1.pom` | `2e1643ca515ba507e8c5b7af521d2b2202df6d776001d6fd9899a5c66d57a370` |
| `ai.mutuus.common:common-platform-bom:pom:0.6.1` | `common-platform-bom-0.6.1.pom` | `e92bfa642e673e42fb268ee8298aa163b8487ba83307c8253b7c3e3e6a3937cc` |
| `ai.mutuus.common:common-platform:pom:0.6.1` | `common-platform-0.6.1.pom` | `d63b5a43d224a553d6326ebeb5b6005684cada833b6646c88a4f23f453e2b274` |
| `ai.mutuus.common:common-platform:jar:0.6.1` | `common-platform-0.6.1.jar` | `18502baf82e259c69efefe3af11a7f6d8a9ffec715e28460dbf787300e17ccf3` |
| `ai.mutuus.common:common-platform-starter-web:pom:0.6.1` | `common-platform-starter-web-0.6.1.pom` | `808bb71f57aa3cf747d12274d706b7d8e2a895a18f99bac0e9c396b90e1fa24f` |
| `ai.mutuus.common:common-platform-starter-web:jar:0.6.1` | `common-platform-starter-web-0.6.1.jar` | `d1095f58ae595250df1e63868c72505e47161aeacdb5c2caf333df2e661307d2` |
| `ai.mutuus.common:common-platform-starter-batch:pom:0.6.1` | `common-platform-starter-batch-0.6.1.pom` | `96f7234b578d51046285bdb60d3024bc9fca12501a672435e44ff39d0b00a907` |
| `ai.mutuus.common:common-platform-starter-batch:jar:0.6.1` | `common-platform-starter-batch-0.6.1.jar` | `e03f678743c1811aef86c0e67d83fed19627eb550fd921fdb11cade0d699b47c` |

### 러너별 영수증

R의 정식8 SHA는 위 P의8 SHA와 동일하다. L=`/var/lib/gs-ci-job/.m2/repository`, E=`/mnt/gs-ci-cache/seed/.m2/repository`; 각 루트의 `ai/mutuus/common/<모듈>/0.6.1/` 대상이다.

| 러너·저장소 | 시도·영수증 | 실제 확인 수·시험 | 부족 근거·보강 대상 |
|---|---|---|---|
| GS-CIWORKER-DEV-01, L | 0747-a18-ex.a03, R 설치·경로·복귀 표 | 신규8/8 SHA 일치, stopped 복귀 | BO 로그 끝부분 미회수 — 보존 로그 회수 필요 |
| GS-CIWORKER-DEV-02, L | 동일 R | 신규8/8 SHA 일치, stopped 복귀 | 서비스별 배치/회원/앱 캐시 완료로 확대하지 않음 |
| GS-CIWORKER-DEV-03, L | 동일 R | 신규8/8 SHA 일치, stopped 복귀 | 동일 |
| dev EFS, E (DEV-01만 쓰기) | 동일 R | 신규8/8 SHA 일치, DEV-01 로컬 포함16/16 | 물리적 dev 독점 사용 보증 근거 없음 — 별도 인프라 확인 필요 |
| BO 오프라인 해석, DEV-01 임시 입력 | R 「BO 오프라인 의존성 해석」 | dependency:resolve 1회 rc=0; SSM 8b84ba59-21ba-41a9-853d-8ad727ede235 Success/ResponseCode0. 시험 메서드/통과 수 해당 없음 | SSM 24KB 제한으로 끝 문구 없음. `/var/tmp/a18-061-a03/bo-resolve.log` 회수 필요; BO 빌드·시험 증거 없음 |
| 배치 러너 | A18 0810 진행 중(brief 기준) | 근거 없음 — 해당 러너 캐시 영수증 필요 | 실제8 SHA 대조·해석 결과 수신 후 보강 |
| 회원 러너 | 동일 | 근거 없음 — 해당 러너 캐시 영수증 필요 | 동일 |
| 앱 러너 | 동일 | 근거 없음 — 해당 러너 캐시 영수증 필요 | 동일 |

R의 캐시 꾸러미 SHA256은 `d2fc6972c1c4a4c69aba79fb09bffaa0162018ea12caeb237d959e6058921aad`다. 개별 artifact SHA와 구별한다. 3대/EFS 설치를 다른 서비스 러너의 완료 증거로 대체하지 않는다.

## 확보·부족·후속 보강

확보: S1 HTTP 완료·오류와0635 8입력, S2/S3 0710 24입력의 기존 통과 기록, P 발행 run·정식8 SHA, R BO/dev3대·EFS 캐시 영수증.

부족: 지정 SHA 파일 원본, 일부 개별 메서드·통과 수 원시 로그, BO 해석 로그 말미, 배치/회원/앱 러너 영수증, 실제 DB/메일/서비스 traceparent 종단 증거. 각 표의 「근거 없음」을 후속 수신 자료로 보강한다. 기존 보고서의 미인수 범위와 버전 결정을 유지한다.

## 문서 확인

검증 이유: 주장·기존 증거 대응과 누락 표시를 확인하기 위해 문서 정독1회만 수행한다. 시험 재실행·전체회귀·교차검토 없음.

## 이슈대처 후보

- 증상: `build/gcom/v0.6.1-sha.md: No such file or directory` (지정 read-repo에서도 동일).
- 원인: 현재 제공 경로에 지정 파일이 없음; 생성/보존 경위는 확인하지 못함.
- 확인 명령: 지정 파일 직접 읽기 및 0722 patch의 사후 발행 표 확인. `team-lesson search "No such file or directory"` 결과는 이번 증거 파일 누락 해결에 직접 맞지 않았음.
- 조치: P에서 정식8 SHA를 확보하고 R 영수증과 대응. 지정 파일 자체의 미확보는 별도 표시.
- 재발 방지: 태그 이후 작성된 증거는 접근 가능한 산출물 절대경로와 파일을 함께 전달한다.

교훈 무관: kb://core/ops/lessons/topic-9d939aa20431030e63ab6c76.md — team-task 제출·시험 runner를 사용하지 않았다.
