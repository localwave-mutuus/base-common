# G-COMMON C3 2단계 — INTERNAL·안전 payload

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

경로의 J는 `lib/src/main/java/ai/mutuus/common`, T는 `lib/src/test/java/ai/mutuus/common`이다.

| 계약 | 파일 | 명령·근거 |
|---|---|---|
| §3 기본 off, annotation/패키지/메서드 AND, exclude 우선, 업무 공개 경계 | J/aop/{ObservedServiceOperation,ServiceMethodLoggingProperties,ServiceMethodLoggingAspect,CommonServiceMethodLoggingAutoConfiguration}.java | C23-A ServiceMethodLoggingAspectTest 6 pass. C23-B ServiceAutoConfigurationContractTest: default off 및 켜도 무관한 final bean을 proxy하지 않음 |
| §3 실제 INTERNAL, UUID 호출별 enter/exit, 동기/throw/cancel/CompletionStage, CAS, thread 복원 | J/aop/{ServiceInvocationScope,ServiceMethodLoggingWriter,ServiceMethodLoggingAspect}.java | C23-A exporter의 실제 span kind/parent 및 event snapshot·UUID 쌍. 완료 전 exit/span end 없음, 비동기 성공/실패/취소 각 종료1회. C23-B ServiceProxyPayloadContractTest 실제 Spring proxy + 비동기 ApprovedInput |
| §3 controller 경로/키 분리·구 raw writer 차단 | J/aop/{ControllerMethodLoggingAspect,DefaultMethodLoggingWriter}.java; 기존 CommonMethodLoggingAutoConfiguration/MethodLoggingProperties 보존 | 구 `mutuus.common.method-logging`은 controller 전용, 새 `mutuus.common.service-logging`과 승격 없음. controller는 custom writer에도 빈 인자/null 반환만 전달. default writer의 toString/deepToString 제거. C23-A 정본 raw method 필드 차단 시험 |
| §4 등록 API, 시작 시 검증/봉인, schema 재검증 | J/payload/{PayloadPolicyRegistry,HttpPayloadRule,ServicePayloadRule,ProjectorSchema,AllowedScalarField,ApprovedInput,SafePayloadProjector,SafePayloadPolicy,CommonSafePayloadAutoConfiguration}.java | C23-A SafePayloadPolicyTest: 정책없음/disabled/masker없음/미승인 값/중첩/배열/필드/중복/미등록 projector 거부. 정책 등록은 시작 전에, 봉인 뒤 변경 금지 |
| §4 별도 설정·기본 ERROR_ONLY·8KiB 경계 | J/payload/{SafePayloadProperties,BoundedPayloadRequestWrapper,BoundedPayloadResponseWrapper,SafePayloadFilter}.java | C23-A: JSON UTF-8 8191/8192 허용·8193 생략, 잘린 UTF-8 거부, 한글로 채운 tee 8191/8192/8193에서 원 요청/응답 bytes 동일·초과 시 캡처 전체0. C23-B: 일부만 읽은 JSON prefix 및 multipart 승인 금지 |
| §4 안전 typed 결과·원문 유출 방어·32KiB | J/payload/{SafePayloadEventWriter,DefaultRequestPayloadLogger,DefaultResponsePayloadLogger,PayloadLoggingFilter}.java; J/logging/{CanonicalLogSnapshot,CommonJsonEncoder}.java | C23-A CanonicalSafeOutputTest: 원 message/throwable stack/본문/구 method/query 및 위조 보호필드 차단. 최종 JSON 32767/32768/32769 bytes 경계, 유효 JSON 재직렬화. C23-C: 2048 byte 낮춘 한도·escape 문자열·필수 ID 유지, 미승인 error.code는 OPERATION_FAILED |

## API·설정과 제한

- 서비스 설정은 계약의 기본값을 따른다. `@Service` 또는 `@ObservedServiceOperation` 업무 후보를 Spring이 proxy하고 include/exclude를 추가 대조한다. repository/controller 및 getter/setter·명시 보안/DTO 등은 제외한다. 공개 업무 메서드의 정확한 서비스별 opt-in 목록은 미제공이라 등록하지 않았다. self-invocation/private는 ServiceInvocationScope 명시 API 경계이며 자동 보장하지 않는다. reactive 선언형 opt-in은 거부한다.
- `safe-payload`와 구 `payload-logging`은 별도다. 구 설정은 새 정책을 승인하지 않고 필터의 raw 캡처도 중단한다. 기존 4096바이트 설정은 확대하지 않았다. 새 기본 8192/32768, max-depth=4/max-fields=32이며 중첩은 깊이와 무관하게 거부한다. max-event-bytes는 필수 envelope를 위한 최소 2048 미만 및 32768 초과를 거부한다. encoder의 logback setter도 지원하며 auto configuration의 한도를 함께 적용한다.
- 등록표 기본은 빈 목록이다. `register(ProjectorSchema)`, `register(projectorId, SafePayloadProjector)`, `register(HttpPayloadRule/ServicePayloadRule)` 뒤 시작 시 검증/봉인한다. ApprovedInput은 검토된 DTO adapter의 marker다. reflection/toString/raw fallback은 없다. 문자열은 검토된 기술 enum만 허용해 자유 문자열보다 좁고, number/boolean scalar만 그 외 허용한다. 비밀/개인정보 이름·중첩/배열·미등록 필드·타입 불일치는 거부하며 전체 생략한다.
- 서비스 payload-enabled는 명시 ApprovedInput 한 개 및 승인된 반환 DTO만 policy에 넘긴다. 예외 자체는 projector에 전달하지 않는다. 오류 전용 모드의 정상 enter는 본문을 출력하지 않는다. 예외 stack/message는 정본에서 출력하지 않으며 error.code는 고정 허용 분류 외 OPERATION_FAILED로 바꾼다. generic KV로 구 본문·인자·헤더·query·보안 자유본문도 복구할 수 없다.
- **안전 projection의 실제 내용 출력은 의도적으로 차단한다.** 정본에는 HTTP body text 필드는 있으나 HTTP/서비스/이슈 공통 projection을 담을 승인된 정본 필드/방향 schema가 확정되지 않았다. 신규 동적 payload 객체나 raw JSON text 필드를 만들지 않고 typed policy ID/생략 사유만 기록한다. Result.fields는 재검증된 scalar 결과 API이며 정본 writer는 이를 직렬화하지 않는다. 계약의 미승인 출력 차단을 유지한다. 이슈 소비자는 같은 policy API를 사용할 수 있지만 외부 이슈 전송기는 이번 수정 대상이 아니다.
- HTTP payload는 routeTemplate가 확보되고 application/json인 동기 완료에서만 평가한다. query/header는 기록하지 않는다. 부분 읽기는 PARSE_ERROR, overflow는 BODY_TOO_LARGE, 비JSON은 POLICY_DENIED다. async servlet payload는 부분 결과를 승인하지 않고 생략한다. HTTP 완료·전파·agent 소유권 코드는 수정하지 않았다.

## 미완료·공유파일/충돌주의

서비스별 실제 opt-in 시그니처/승인 schema와 안전 projection 정본 필드 결정, 실제 앱/이슈 적용, async servlet payload 완료별 projection은 후속 인수다. §6 OpenSearch·성능/자격/실접속은 제외했다. §1/§2 잔여는 기존 step1 보고서대로 유지한다.

공유파일: lib/pom.xml(test scope SDK fixture만 추가), AutoConfiguration.imports(서비스/안전 payload 2행 추가), CanonicalLogSnapshot/CommonJsonEncoder, registry v5는 한 좌석에서 3단계와 함께 조정했다. imports 기존 항목, agent-extension/HTTP 1단계 구현, 계약/계획/step1 보고서는 보존했다. patch를 파일별 새 patch로 대체하지 말고 누적 전체로 검토한다.

## 이슈대처 후보

- 증상: 앞선 플랫폼 첨부 333451B 한도초과·안내 작업브랜치가 원본에 없음(brief 전달 사실). 이번에는 branch 조회나 추측 재구현 없이 명시 산출물 파일을 읽었다.
- 원인: 대형 누적 patch를 첨부/브랜치 이름만으로 전달할 수 없는 상태.
- 확인 명령: `git apply /mnt/disk/00.workspaces/b04.mutuus-operation/artifacts/261021-0635-a13-ex/261021-0635-a13-ex.a01/patch.diff` — exit 0, 62개 입력 경로 확보.
- 조치: 같은 호스트의 원본 누적 산출물을 사본에 적용하고 2·3단계 변경을 합쳤다. 입력 접근 누락 없음.
- 재발 방지: 고정 base SHA와 접근 가능한 산출물 절대경로를 함께 전달한다. 이번 새 차단 오류가 없어 증상 검색은 실행하지 않았다. 관련 AWS/DB 이슈대처는 이번 경계와 무관하다.
