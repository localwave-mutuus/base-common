# G-COMMON C3 1단계 구현 결과 — 0635 수명 wire·Spring transport·HTTP/2

REQ-261021-02 / 최신 attempt 261021-0635-a13-ex.a01 (0627 기록 보존) (아래 0602 기록 보존) / 기준 `d2e75a2f4e694440a0926b47e03b3c76ca0bc312`.

**0635 최신:** 최종 수명 보완 후 JDK wire, Spring 7.0.8의 Simple/JDK factory, 실제 HTTP/2 wire를 아래 「0635 후속」에서 구분한다. §1·§2 전체 인수·배포 승인은 여전히 미완료다.

**0627 당시: 지정 JDK wire case 해소. §1·§2 전체 인수·배포 승인은 아니다.** 최신 구현·검증 및 한계는 아래 「0627 후속」을 따른다. 다음 문장부터는 0622 당시 판단을 보존한다. **0622 판정: unable.** 고정 agent 설치와 실제 CLIENT 연결은 후속 로컬 harness에서 확인했다. 최초 URL 기반 host 허용은 redirect 누출로 기각했으며 최종 코드는 outbound 전면차단 fallback이다. 승인 내부 host 전파가 남아 완료 조건을 충족하지 않는다. 0614 기록은 보존하며 최신 조사·신규 실패 근거는 「0622 후속」 절을 따른다.

**아래는 0602 시점 기록이다.** SDK fixture에서 실제 lineage·정본 연결 및 HTTP 완료 경계를 구현·검증했다. 고정 Java agent 프로세스의 설치·wire 전파·실제 CLIENT span 연결과 외부 목적지 기본차단은 입증하지 못했다. 배포 가능한 C3 완료나 0.6.0 발행 성공으로 해석하지 않는다.

## 첨부 보존

C1 POM·기존 시험·0.5.2 보고서는 기준 커밋에 이미 있어 중복 적용하지 않았다. 로컬 Git 객체에서 첨부 C3 부분을 복원했다. 계약 blob `accb5e36e2be18d220df88b2374976663e30ea77`, 설계 blob `1c8af57a0c160b4a6badee2d4e7e58a60024fb9f`, 기존 0.6.0 보고서 blob `cde989d84`를 보존했다. 계약·설계·과거 보고서를 완화하거나 현재 결과로 덮어쓰지 않았다. snapshot/accessor/decorator 및 기존 snapshot 시험도 patch에 포함했다. decorator는 첨부 보존분이며 §7 신규 구현은 하지 않았다.

## 계약 대응 및 구현 근거

| 계약 | 변경 파일·동작 | 확인 범위와 잔여 |
|---|---|---|
| §1 실제 부모 | `agent-extension/.../observability/SpanLineageSpanProcessor.java`: SDK onStart의 parentContext와 실제 span ID로 live map 저장, onEnd 제거. root 부모 없음. 한도 기본 100000, 초과 신규 보관 생략, 포화 카운터. | SDK 실제 생성/종료·remote/root·한도·제거 확인. exporter 실제 parent와 정본 parent 일치 확인. unsampled 비기록 span의 부모를 추정하지 않음. |
| §1 설치 | `agent-extension/.../agent/LineageAutoConfigurationCustomizerProvider.java` 및 서비스 파일: enabled 기본 true, SDK builder에 processor 설치. | SPI 발견, provider 적용 후 SDK onStart 연결, disabled 미설치 확인. 고정 Java agent 설치 미확인. |
| §1 classloader·snapshot | `PlatformSpanLineageAccessor`, `TelemetryContextSnapshot`, `CanonicalLogSnapshot`: JVM platform MBeanServer의 String 인자로만 조회. lib에 SDK runtime 의존성 없음. | 애플리케이션 accessor를 별도 URLClassLoader/platform parent로 로드해 같은 SDK 저장소 조회. 종료 후 snapshot async 복원 확인. 실제 agent loader 전체 구조를 재현한 시험은 아님. |
| §1 미연결 진단 | MBean 부재/조회 실패 시 parent 생략, `PlatformSpanLineageAccessor.unavailableCount()` 포화 카운터. processor Active/Dropped MBean 속성. | agent 부재를 root로 보정하지 않음. JMX 사용 금지 환경에서는 부모 생략. live 캐시 한도 진단과 연결 부재 진단을 분리. |
| §2 단일 완료 | `HttpCompletionScope`: UUID, monotonic ns, 불변 snapshot, CAS 마감. 캡처 ID를 완료 메타데이터로 덮어쓰기 금지. 로깅 RuntimeException은 포화 진단 카운터 후 요청 결과 보존. | 동시 취소 20회 경합 1건, status 없는 failure, writer 실패 1회, ID 덮어쓰기 차단 확인. |
| §2 Servlet | `AccessLogFilter`: 기존 Controller route와 검증 신원 경로 유지. 상태만으로 401 auth.failed를 만들지 않음. 일반 IO를 단절로 단정하지 않음. 500/HTTP_500, async 504/HTTP_504·500/HTTP_500. | 200/302/400/401/403/404/405/409/413/422/500/503, 미처리 예외, IO, timeout/error/complete 경합, onStartAsync 재등록, committed IO 상태 유지 확인. 미매칭 /{unmatched}, query 없음. |
| §2 단절 | 확인된 Tomcat ClientAbortException/Jetty EofException만 499/CLIENT_DISCONNECTED. 예외 message 검사·로그 없음. | 실제 연결 단절/해당 container 예외 fixture는 미실행. 다른 container는 명시 판별 추가 전 일반 IO로 처리. |
| §2 Spring outbound·비웹 | `HttpClientLoggingInterceptor`, `CommonHttpClientObservationAutoConfiguration`, `CommonWebAutoConfiguration`, imports. MVC 조건 밖 등록. 4xx failure, 고정 error.code. 원문 query/path 출력 차단. | MVC 없는 context에서 단일 빈 확인. 실제 agent CLIENT ID는 확인하지 못해 호출자의 ID를 CLIENT로 출력하지 않고 생략. |
| §2 retry | `HttpRetryInterceptor`: 관측을 attempt 내부에 배치. retry 활성화 시 별도 logging customizer 중복 적용 제외. 기존 단일 인자 생성자 유지. | IO 실패→403 응답 두 attempt 각각 완료, 두 건 failure 확인. Spring 자동구성 경로의 동일 관측 빈 사용 확인. |
| §2 JDK client | `ObservedJdkHttpClient`: sync/sendAsync/PushPromise overload, 응답·예외·timeout·cancel 완료. 원래 future 반환으로 취소 전파 유지. close/shutdown/WebSocket 등 delegate 위임. | 실제 로컬 서버의 sync/async 401·취소, 통신 예외·timeout·동기 sendAsync throw 확인. redirect/내부 transport retry 개별 attempt는 decorator로 관측 불가. |
| §2 JDK server | `JdkHttpServerObservationHandler`: 전체 auth/router를 감싸는 API. AGENT는 current span 차용, MANUAL만 W3C extract→SERVER 생성/종료. 활성 span이 있는 manual 진입 거부. | 유효/무효 W3C, 403/500, manual 실제 JDK 서버 204, agent 모드 span 차용·미종료 확인. 두 모드 중복 방지. |
| §2 admission | `JdkHttpServerAdmissionAdapter`: 교환 객체를 받은 뒤 executor 제출 전 scope 생성. 거절 503/HTTP_503, 수락 handler와 같은 scope. | 거절 1건, 실제 수락 서버 1건 확인. 교환 객체 전달 전 JDK 내부 executor 거절은 관측 범위 밖이며 새 metric 계측은 미구현. |

`agent-extension/pom.xml`은 SDK/SPI 1.62.0을 provided로 사용한다. 이는 기존 parent의 SDK API 기준이며 Java agent 버전 확정이 아니다. 신규 extension은 deploy skip이며 발행 좌표 확정/발행하지 않았다. root POM에는 시험 reactor 참여만 추가했다. lib POM·기존 facade/Security/Idempotency API는 변경하지 않았다.

## 외부 host 정책의 구체적 제약

OTel SDK 1.62.0의 [AutoConfigurationCustomizer](https://github.com/open-telemetry/opentelemetry-java/blob/v1.62.0/sdk-extensions/autoconfigure-spi/src/main/java/io/opentelemetry/sdk/autoconfigure/spi/AutoConfigurationCustomizer.java)는 propagator와 config를 교체할 수 있지만 HTTP 목적지 인자를 제공하지 않는다. [TextMapPropagator.inject](https://github.com/open-telemetry/opentelemetry-java/blob/v1.62.0/context/src/main/java/io/opentelemetry/context/propagation/TextMapPropagator.java)는 Context·generic carrier·setter만 받으며 carrier는 null도 허용한다. 범용 SPI만으로 모든 클라이언트의 hostname을 읽는 계약은 없다. 클라이언트별 carrier/계측 확장으로 목적지를 공급하는 연결이 추가로 필요하다는 판단이다. 모든 agent 버전에서 구현 불가능하다는 주장은 아니다.

따라서 `TrustedDestinationPropagationCustomizer`의 host 판정을 추측 구현하지 않았다. `external-enabled=false`, 정확한 allowed-hosts/와일드카드 거부, baggage 별도 승인 차단을 **실제로 강제하는 extension·설정 바인딩은 미구현**이다. 문서상 기본값만으로 현재 전파가 차단된다고 주장하지 않는다. 공통 인터셉터의 명시 inject/주입 후 삭제는 추가하지 않았다. 현재 agent의 전파는 이 patch로 제한되지 않는다.

추가 입력은 고정 Java agent 버전·artifact, 지원 Rest/JDK 계측 모듈 및 destination을 최종 주입 경계에 공급하는 해당 버전 API다. [공식 extension 안내](https://opentelemetry.io/docs/zero-code/java/agent/extensions/)의 SDK customizer 경로는 구현했으나 agent artifact 없이 실제 설치 완료로 판정하지 않았다. 승인 내부 host 허용/외부 차단/baggage 차단을 wire로 확인하기 전 해당 클라이언트 배포 근거가 없다.

## 최소 검증 기록

각 실행 이유는 해당 신규 변경 계약 확인이다. 동일 입력 pass는 반복하지 않았다. 기존 snapshot 신규 5개와 C1 기존 28개, sample-api·전체 회귀는 실행하지 않았다. 로그는 attempt 출력 디렉터리에 보존했다.

공통 명령은 `bash ./mvnw -B -pl lib,agent-extension -am '-Dtest=<선택>' -Dsurefire.failIfNoSpecifiedTests=false test`이며 아래 마지막 두 실행의 -pl은 각각 agent-extension, lib다.

| 로그 | 선택 입력·이유 | 결과 |
|---|---|---|
| c3-step1-minimum.log | HttpCompletionContractTest, SpanLineageSpanProcessorTest: HTTP 완료·SDK 연결 | 컴파일 실패. 새 decorator가 package-private CONTEXT_KEY를 참조. public 상수로 수정. 시험 미실행. |
| c3-step1-minimum-fix.log | 위 미실행 입력 | HTTP 4개 중 3 pass, 1 fail. 기존 신원 anonymous를 필드 부재로 잘못 기대한 시험을 수정. SDK 시험은 reactor 중단으로 미실행. |
| c3-step1-selected-fix.log | HttpCompletionContractTest#responseStatusesValidationAndControllerRoutesCompleteOnce, ObservedJdkHttpClientTest, SpanLineageSpanProcessorTest, JdkHttpServerObservationHandlerTest | 6개 pass. 이전 pass 3개 제외. |
| c3-step1-outbound-exporter.log | HttpClientObservationConfigurationTest, SpanLineageSpanProcessorTest#exporterParentMatchesCanonicalAndSpiProviderIsDiscoverable | 2개 pass. 비웹 retry 및 exporter 부모/정본 일치. |
| c3-step1-additional-http.log | HttpCompletionContractTest#restartedAsyncRegistersAgainAndCommittedIoKeepsStatus, ObservedJdkHttpClientTest#timeoutAndCommunicationFailuresHaveOneSafeCompletionEach, JdkHttpServerObservationHandlerTest#realManualServerAdmissionAndAgentBorrowingHaveOneCompletionAndNoExtraSpan | 3개 pass. 재등록·IO 상태·JDK 실패/timeout·실제 수락 서버. |
| c3-step1-installation.log | LineageInstallationTest | 1개 pass. provider의 실제 SDK builder 설치 및 disabled 미설치. |
| c3-step1-lineage-protection.log | HttpCompletionContractTest#completionMetadataCannotReplaceActualLineageOrRequestId | 1개 pass. 완료 메타데이터의 ID 덮어쓰기 차단. |

서로 다른 신규 시험 메서드 16개가 pass했다. 모든 입력을 마지막 source에서 다시 실행한 전체 회귀 결과는 아니다. Mockito의 ByteBuddy 테스트 agent 경고는 OTel Java agent 실연동 증거가 아니다.

## 잔여 및 충돌 주의

- §1·§2 미완료: 고정 agent 실제 설치/loader 검증, 목적지별 W3C/baggage 차단, 실제 CLIENT snapshot·exporter·wire 중복 주입/중복 span 확인. outbound IDs는 확보 전 생략하며 이 상태를 전체 계약 완료로 간주하지 않는다.
- JDK decorator는 외부 send 호출당 완료 하나다. agent 내부 redirect/transport retry attempt별 로그와 callback 시점 CLIENT snapshot은 별도 agent 계측 연결이 필요하다. Spring retry의 명시 attempt 관측과 구분한다.
- 자체 서버의 manual/agent 선택은 명시 enum API다. `mutuus.common.observability.jdk-server.span-owner` 자동 바인딩과 실제 AdminHttpServer/EmailEventServer 적용은 미구현이다. 다른 저장소를 수정하지 않았다.
- Access=HIGHEST_PRECEDENCE, Trace=+1은 유지했다. 소비 앱 CORS 필터의 동일 최상위 순위 충돌은 이 저장소에서 해소하지 못했다. 소비 앱은 CORS/앱 오류를 더 안쪽에 등록해야 한다. container 사전 거절/강제종료 지표 연동도 미확인이다.
- 실제 client disconnect, 모든 인증/검증 프레임워크 경로, push promise·redirect·Spring 실제 wire·모든 async race·서비스 소비자 바이너리 호환을 포괄 검증하지 않았다.
- 다음 단계 충돌 파일: `CanonicalLogSnapshot.java`(public context key 및 lineage 연결), auto imports, root POM(agent 모듈), `CommonWebAutoConfiguration.java`, 신규 `CommonHttpClientObservationAutoConfiguration.java`, `HttpClientLoggingInterceptor.java`, `HttpRetryInterceptor.java`, observability snapshot/accessor 및 agent-extension 전체. shared writer/registry/버전 통합 때 첨부 API와 이번 변경을 함께 보존해야 한다.
- §3·§4·§5·§7 신규 구현, §6 OpenSearch/성능 실측, 버전 0.6.0 정렬·Publish·배포는 수행하지 않았다. publish workflow·기존 태그·원격/다른 저장소·GS는 변경하지 않았다. 자격 조회/사용은 없었다.

교훈 무관: kb://core/ops/lessons/topic-9d939aa20431030e63ab6c76.md — team-task/verify_allow 제출 경로를 사용하지 않았다.


## 0614 후속 — 고정 agent·CLIENT 연결 및 전파 정책 실패 근거

첨부 0602의 32개 blob을 지정 객체에서 그대로 복원했다(기준 커밋에 이미 있는 C1 부분은 재적용하지 않음). snapshot/decorator·계약·계획·과거 보고서를 모두 전체 patch에 보존했다. 새 작업/서브에이전트/다른 저장소 변경/발행은 없다.

### 고정 artifact와 API 근거

- Java agent: `io.opentelemetry.javaagent:opentelemetry-javaagent:2.28.0`, Maven Central 공개 artifact.
- SHA256: `130606aed07f101458fe42b8f453ef3a2536f6bce8a7ae64f222f5688ada2500`. harness 실행 전에 해당 digest와 일치를 검사한다.
- [2.28.0 instrumentation BOM](https://repo.maven.apache.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-bom/2.28.0/opentelemetry-instrumentation-bom-2.28.0.pom)은 OTel BOM **1.62.0**을 import한다. extension의 provided SDK/SPI 1.62.0과 일치한다. artifact 버전은 dev harness 기준이며 운영 설치 사실이 아니다.
- JVM: OpenJDK 21.0.12.1. extension은 `AutoConfigurationCustomizerProvider` 서비스 파일로 로드하고 별도 application SDK를 초기화하지 않는다.
- [고정 agent HttpClientInstrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/instrumentation/java-http-client/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/javahttpclient/HttpClientInstrumentation.java)은 public send/sendAsync 경계에서 CLIENT span을 만든다. [HttpHeadersInstrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/instrumentation/java-http-client/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/javahttpclient/HttpHeadersInstrumentation.java)은 headers() 결과와 Context로 주입하며 custom propagator에 최종 request URI를 전달하지 않는다.
- `ReadableSpan.getAttribute(url.full/http.url)`를 이용한 최초 목적지 승인 후보는 로컬 redirect 시험에서 실패했다. 이 속성을 최종 wire 목적지 증명으로 사용하지 않는다. **이 버전의 범용 SDK propagator customizer만으로 해결하지 못했다**는 결론이며, 모든 agent 확장 방식이 기술적으로 불가능하다는 뜻이 아니다.

### 항목별 결과

| 계약 | 최신 구현·실행 근거 | 판정/한계 |
|---|---|---|
| §1 고정 agent 설치·loader | 고정 jar + production extension 클래스를 포함한 dev jar를 별도 JVM `-javaagent`로 실행. 테스트 전용 HarnessExporter는 jar에만 추가하며 production 서비스 파일에는 포함하지 않음 | 설치·실행 확인. agent loader의 processor와 application의 accessor가 같은 platform MBeanServer에서 실제 데이터를 공유 |
| §1 실제 CLIENT lineage | processor에 bounded observation token 추가. JMX beginClient로 dispatch thread만 연결하고 onStart의 실제 CLIENT 또는 이미 활성인 CLIENT의 snapshot을 보관. detachClient는 send/sendAsync 반환 finally에서 thread 연결을 해제. finishClient는 완료 시 snapshot 반환·삭제 | JDK sync/sendAsync, Spring RestTemplate + SimpleClientHttpRequestFactory의 6 CLIENT span에 대해 exporter/log trace·span·parent 일치 확인. 가짜 caller ID/헤더/MDC를 부모로 사용하지 않음 |
| §2 async snapshot·해제 | AgentClientObservationBridgeTest: INTERNAL은 채택하지 않음, CLIENT 종료 후 다른 스레드에서 불변 snapshot 회수, 중복 회수 빈 값, detach 후 나중 span 오염 없음, 이미 활성 CLIENT 차용 | 새 1개 메서드 pass. observation map과 live map은 각각 maximum 한도로 제한. SDK 미연결/한도 초과/unsampled 비기록 span은 CLIENT ID 생략 |
| §1 외부 기본차단·baggage | TrustedDestinationPropagationCustomizer가 최종 propagator의 outbound inject를 전면 no-op. inbound extract는 기존 propagator로 위임. allowed-hosts는 exact hostname 문법 검증·wildcard 거부하나 승인 근거로 사용하지 않음. external-enabled 기본 false이며 true는 지원 불가 설정으로 거부 | 최종 fallback에서 localhost→127.0.0.1 redirect의 비승인 목적지에 W3C가 없는 것 확인. baggage도 새로 주입하지 않음. **내부 승인 host까지 차단되므로 §1 미완료** |
| §1 승인 host W3C | 최초 URL 후보에서 직접 호출은 승인 host W3C 1건, 비승인 host 차단, baggage 차단 성공했으나 redirect 누출 발견 | 후보 폐기. 그 pass는 최종 정책의 승인 host 전파 증거가 아님 |
| §2 HTTP 소유권 | lib는 CLIENT span 생성/inject/주입 뒤 삭제를 하지 않음. 새 bridge만 사용. server/manual 및 기존 완료 scope는 첨부 보존 | HTTP span·주입 소유권은 agent 유지. redirect/transport retry 개별 attempt 완료 로깅은 여전히 미완료 |

### 재현 명령·실행 기록

공개 agent artifact 준비(자격·외부 서비스 접속 없이 Maven artifact만 다운로드):

```sh
mkdir -p agent-extension/target/harness
curl -fsSL https://repo.maven.apache.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/2.28.0/opentelemetry-javaagent-2.28.0.jar -o agent-extension/target/harness/opentelemetry-javaagent-2.28.0.jar
sha256sum agent-extension/target/harness/opentelemetry-javaagent-2.28.0.jar
```

선택 시험의 공통 명령:

```sh
bash ./mvnw -B -pl lib,agent-extension -am '-Dtest=<아래 선택>' -Dsurefire.failIfNoSpecifiedTests=false test
```

| 로그(TEAM_OUT_DIR) | 선택·실행 이유 | 결과 |
|---|---|---|
| c3-agent-minimum.log | TrustedDestinationPropagationCustomizerTest,FixedAgentHarnessTest: 신규 정책·실제 agent 확인 | testCompile 실패. lib의 optional Spring web이 extension 시험에 전이되지 않음. extension POM에 spring-web **test** 의존성만 추가 |
| c3-agent-compile-fix.log | 같은 미실행 입력 | 최초 URL 정책 단위 1 pass. harness 실패: wire flags를 01 상수로 잘못 기대. agent 1.62.0이 실제 flags 03을 사용해 snapshot 값과 비교하도록 수정 |
| c3-agent-wire-fix.log | FixedAgentHarnessTest: 직전 실패 harness만 | 1 pass. 최초 정책에서 6개 실제 CLIENT/exporter/log/wire 일치·baggage 차단·중복 없음·Active=0 확인. **최종 정책 변경 전 결과** |
| c3-agent-redirect-bridge.log | AgentClientObservationBridgeTest,FixedAgentHarnessTest#redirectDestinationContract: 새 스레드/종료 snapshot 및 새 redirect 입력 | bridge 1 pass, redirect 1 fail. localhost 승인 후 302 Location=127.0.0.1 이동에서 비승인 목적지가 traceparent 수신. 실패 로그는 ID/헤더 원문을 출력하지 않음 |
| c3-agent-redirect-failclosed.log | FixedAgentHarnessTest#redirectDestinationContract: 실제 누출 원인 수정 확인 | 최종 전면차단 fallback에서 1 pass, BUILD SUCCESS (2026-10-20T21:20:41Z). sha 고정 확인 포함 |

고정 agent 설정은 runner에 코드로 보존: `otel.javaagent.extensions=<dev jar>`, `otel.service.name=common-c3-local-harness`, `otel.propagators=tracecontext,baggage`, `otel.traces.sampler=parentbased_traceidratio`, `otel.traces.sampler.arg=1.0`, traces/metrics/logs exporter=none, allowed-hosts=localhost, external-enabled=false. 로컬 서버만 127.0.0.1/localhost로 접속하며 테스트 전용 in-process exporter가 SDK SpanData를 JMX로 공개한다. production exporter/외부 endpoint/자격을 사용하지 않는다.

이전 16개·snapshot 5개·0540 28개와 sample-api/전체 회귀는 재실행하지 않았다. 통과한 최초 URL 정책 단위 및 6호출 harness는 최종 전면차단에 맞게 기대값을 수정했으나 같은 입력을 반복하지 않아 **수정 후 미실행**이다. final redirect pass와 bridge pass만 최종 변경에 해당하는 새 근거로 구분한다. 최종 정책에서 승인 host 전파 성공을 주장하지 않는다. harness는 공개 jar를 위 경로에 준비한 dev 전용 선택 시험이다.

### 잔여·계약을 유지할 대안·충돌주의

1. **미완료:** 승인 내부 host에 W3C를 전파하면서 비승인 최종 host는 차단하는 transport destination 연결. `TextMapPropagator.inject(Context,C,TextMapSetter<C>)`의 일반 carrier와 최초 CLIENT URL만으로는 redirect 최종 host를 승인할 수 없었다. 현재 전면차단은 기밀 경계의 보수적 fallback이며 정상 내부 전파까지 완성했다는 뜻이 아니다. 이 상태로 배포 승인할 수 없다.
2. 대안은 고정 agent의 클라이언트별 transport extension에서 **최종 request URI**를 검증된 Context에 전달한 뒤 바로 그 주입에서 exact allowlist를 검사하는 방식이다. redirect/내부 retry 각 attempt에도 같은 경계를 적용하고 exporter/로그 완료를 attempt별 연결해야 한다. generic SDK SPI만으로 된다고 추정하지 않는다. 다른 대안은 자동 redirect/숨은 transport retry를 금지한 클라이언트 프로파일만 별도 인수하고 나머지를 배포 대상에서 제외하는 것이다. 이번 patch는 해당 프로파일을 강제하지 않으므로 완료로 간주하지 않는다.
3. 전면차단은 agent가 새로 주입하는 헤더에 적용한다. 호출자가 이미 직접 넣은 traceparent/baggage를 지우는 기능은 추가하지 않았다. 공통 인터셉터 삭제 우회는 금지 그대로다.
4. §2 redirect/transport retry attempt별 완료, jdk-server.span-owner 설정 자동 바인딩 및 실제 서비스 handler 적용, 소비 앱 CORS 순서/사전 거절 지표 등 이전 잔여는 그대로다. 다른 저장소/§3·§4·§5·§7 신규 구현/§6 실측/발행은 수행하지 않았다.
5. 충돌주의: 첨부의 CanonicalLogSnapshot·imports·root POM·web 자동구성 파일 외에 **SpanLineageSpanProcessor, agent 서비스 파일, AgentClientObservation, HttpCompletionScope, ObservedJdkHttpClient, HttpClientLoggingInterceptor, agent-extension POM**. JMX action 이름과 String/Map 반환 계약을 함께 보존해야 한다. production 서비스 파일은 lineage와 fail-closed policy 두 provider만 포함한다. 테스트 HarnessExporter를 배포 jar에 넣지 않는다.

최종 판정 **unable**: 고정 agent 실행 및 실제 CLIENT 연결 근거는 확보했지만 승인 host 전파와 최종 목적지 정책·attempt별 완료가 남아 §1·§2 잔여 해소 조건에 미달한다.


## 0622 후속 — transport 경계 조사와 신규 실패 근거

판정: **unable**. 첨부0614의 40개 파일을 지정 Git blob에서 복원했다. 기준 d2e75a2f의 기존 C1 변경은 유지하고 중복 적용하지 않았다. 계약·계획·이전 보고서·snapshot/decorator도 누적 patch에 보존한다. 이번 production 코드는 첨부 상태 그대로이며 승인 전파·잔존 헤더 제거·attempt 계측을 구현하지 못했다. 아래는 실패 진단용 fixture와 구체적인 대안이며 해결된 구현이 아니다.

### 지원 API와 실제 transport의 경계

고정 agent 2.28.0 공식 소스를 직접 조회하고 TEAM_OUT_DIR에 저장했다. JVM은 OpenJDK 21.0.12.1이다.

- [HttpHeadersInstrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/instrumentation/java-http-client/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/javahttpclient/HttpHeadersInstrumentation.java): headers() exit advice는 HttpHeaders만 받고 Context.current()로 setter를 부른다. 실제 HttpRequest/URI를 propagator에 넘기는 hook이 없다. 따라서 원래 URL 속성을 다시 이용하지 않았다.
- [HttpHeadersSetter](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/instrumentation/java-http-client/library/src/main/java/io/opentelemetry/instrumentation/javahttpclient/internal/HttpHeadersSetter.java): original.map()을 복사한 뒤 inject한다. no-op inject에는 삭제 연산이 없으므로 이미 있는 traceparent/tracestate/baggage가 남는다. 이 클래스는 공식 주석에도 internal/unstable API로 명시되어 있다.
- [HttpClientInstrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/instrumentation/java-http-client/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/javahttpclient/HttpClientInstrumentation.java): public send/sendAsync 전체에 instrumenter.start/end를 적용한다. 내부 redirect/transport retry를 각각 CLIENT로 만드는 계측이 아니다.
- [InstrumentationModule](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/javaagent-extension-api/src/main/java/io/opentelemetry/javaagent/extension/instrumentation/InstrumentationModule.java): TypeInstrumentation/TypeTransformer를 등록하는 확장 SPI는 존재한다. 따라서 agent 전체에서 기술적으로 불가능하다고 결론내리지 않는다. **현재 SDK customizer만으로는 부족하며 새 transport 계측 모듈이 필요하다.**
- 로컬 `javap --module java.net.http -p -c jdk.internal.net.http.HttpRequestImpl`에서 redirect 생성자가 이전 userHeaders를 복사/재사용하는 것을 확인했다. `MultiExchange` bytecode에서 responseFilters→새 HttpRequestImpl/Exchange→responseAsyncImpl 및 실패 retry 경로를 확인했다. `Exchange.request()`와 responseAsync()가 요청/응답 경계 후보지만 JDK internal ABI다. headers()만 수정하면 attempt 시작·완료·취소/실패 수명과 연결되지 않는다. bytecode 두 파일을 TEAM_OUT_DIR에 보존했다.

### 신규 최소 wire 시험 (각 1회)

새 파일은 `TransportAttemptContractMain.java`, `ResidualHeaderRedirectMain.java`, `TransportAttemptContractTest.java` 세 개다. 기존 runner/고정 digest/로컬 exporter를 재사용하되 기존 시험 메서드는 실행하지 않았다. 테스트는 계약 위반을 실제 실패로 남긴다. 이 누적 patch는 시험 green 상태가 아니다.

1. 이유: 승인→비승인 및 역방향 redirect와 응답 전 연결 종료로 발생하는 실제 JDK 내부 retry의 목적지 헤더/완료 건수를 대조한다.
   명령: `bash ./mvnw -B -pl lib,agent-extension -am -Dtest=TransportAttemptContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
   이 실행 당시 클래스에는 첫 메서드만 있었다. 결과: 1 test / 1 failure, BUILD FAILURE. `c3-transport-attempt-contract.log` 및 harness `transport-attempt-contract.log`.
   실제 wire 6건(양방향 redirect 각 2건, hidden retry 2건), 승인 4건 중 traceparent 수신 0건, 비승인 누출 0건, 완료 3건, CLIENT span 3건. 승인 전파와 attempt별 완료 모두 계약 미달이다. retry는 애플리케이션 retry 루프가 아니라 하나의 send 안에서 첫 TCP 요청을 읽고 응답 없이 닫아 발생시켰다.
2. 이유: setter의 원래 헤더 복사 경로에서 잔존 propagation 헤더가 비승인 redirect로 넘어가는지 별도 입력으로 확인한다.
   명령: `bash ./mvnw -B -pl lib,agent-extension -am '-Dtest=TransportAttemptContractTest#previouslyPresentHeadersMustNotSurviveUnapprovedRedirect' -Dsurefire.failIfNoSpecifiedTests=false test`
   결과: 1 test / 1 failure, BUILD FAILURE. `c3-residual-header-contract.log` 및 harness `residual-header-contract.log`.
   localhost→127.0.0.1 최종 wire에 traceparent/tracestate/baggage 3종이 모두 잔존했다. fixture가 합성 값을 **미리 넣은** 입력이며, 현재 fallback agent가 이를 새로 주입했다고 주장하지 않는다. 실제 헤더 값은 출력하지 않았다.

agent jar는 공개 Maven Central에서 기존 경로로 받았으며 runner의 기존 SHA256 고정 검사가 두 실행 모두 통과했다. 기존16개·snapshot5개·0540의28개·0614 시험·전체회귀·sample-api는 재실행하지 않았다. 증상으로 team-lesson search를 수행했으나 관련 원인 해결 자료가 없어 적용하지 않았다.

### 계약을 유지하는 구체적 대안과 미구현 범위

필요한 대안은 SDK customizer 보강이 아니라 고정 JDK/agent에 대한 transport 계측 교체다. 다음을 하나의 agent 모듈로 구현·인수해야 한다.

1. 기존 java-http-client public send CLIENT 계측과 새 계측을 동시 활성화하지 않는 설정 게이트를 둔다. 외부 send는 호출 부모 Context만 전달하고, 실제 각 transport attempt에서 새 CLIENT를 agent Instrumenter로 생성한다. agent가 span/전파를 계속 소유하며 lib interceptor는 inject하지 않는다.
2. Exchange의 실제 HttpRequestImpl URI를 읽고, 헤더 직렬화 직전 원래 세 propagation 헤더를 대소문자 구분 없이 제거한 복사본을 만든다. 그 attempt의 정확한 host가 승인된 경우에만 agent W3C propagator로 traceparent/tracestate를 넣고 baggage는 별도 승인 전까지 넣지 않는다. 같은 최초 URL/기존 헤더를 다음 hop의 승인 근거로 쓰지 않는다. HTTP/1.1과 HTTP/2의 실제 serialize 경로 모두 증명해야 한다.
3. responseAsync()의 비동기 결과/예외와 취소에 attempt별 CAS 종료를 연결한다. 302 attempt도 다음 hop 전에 개별 종료하고, 실패한 retry attempt도 status 없이 failure로 종료한다. 같은 request 객체를 재사용하는 retry도 별도 식별자/CLIENT를 갖는다.
4. completion을 agent→application의 JDK 타입 bridge로 한 번 전달하고, transport 관측이 활성화된 호출에서는 ObservedJdkHttpClient의 외부 send 완료를 억제해 중복 이벤트를 막는다. observation token·snapshot·종료/취소 해제와 한도 초과 경로도 연결해야 한다. 로그 없는 단순 exporter span만으로 계약 완료를 대신하지 않는다.
5. 고정 agent 2.28.0/JDK 21.0.12.1의 module 접근·helper 주입·Context 전달을 검증하고 지원하지 않는 ABI는 명시적으로 거부한다. Spring의 별도 transport는 JDK 구현 경계로 해결되었다고 간주하지 말고 해당 transport 계측을 따로 연결해야 한다.

이 대안의 extension SPI 존재까지 조사했지만 새 module/helper/Instrumenter·완료 bridge를 구현·실행하지 못했다. 위 경계가 모두 불가능하다는 증거는 아니며, 안전한 최소 구현을 제출할 수 없는 상태다. 자동 redirect/숨은 retry를 금지하는 좁은 프로파일은 현재 요구의 양방향 redirect/내부 retry 인수를 충족하지 않으므로 완료 대안으로 제출하지 않는다. 전면차단 유지도 완료로 간주하지 않는다.

잔여: 승인 W3C 전파, 실제 목적지 정책, 잔존 헤더 제거, redirect/retry 각 CLIENT/완료 및 중복 방지 전부 미해소. §3·§4·§5·§7 후속과 §6 설계는 그대로 유지했다. 발행/push/태그/배포/타 저장소/자격 사용은 없다.

후속 충돌 파일: 첨부의 agent-extension POM·서비스 파일·TrustedDestinationPropagationCustomizer·SpanLineageSpanProcessor와 lib AgentClientObservation·HttpCompletionScope·ObservedJdkHttpClient·HttpClientLoggingInterceptor. 새 transport module용 서비스 등록은 현재 SDK provider 파일과 다른 InstrumentationModule 서비스 경로여야 한다. 테스트 전용 HarnessExporter를 production 서비스에 추가하지 않는다. 신규 실패 fixture 세 개도 후속 구현 시 계약을 만족하도록 연결해야 한다.

## 이슈대처 후보

- 증상: 지정 문서 세 개 `No such file or directory`.
- 원인: 작업 사본은 C1 기준이며 첨부0614는 아직 적용되지 않았다.
- 확인 명령: `git cat-file -t <첨부 index의 blob>`.
- 조치: 첨부 index의 40개 blob을 각 지정 경로에 복원했다. team-lesson 검색 결과는 PowerShell 등 다른 원인이라 미적용했다.
- 재발 방지: 이 분리 HEAD에서는 첨부 적용 여부와 기준 커밋을 구별하고, 원격 브랜치나 다른 사본에서 임의 파일을 가져오지 않는다.


## 0627 후속 — JDK transport 실제 구현 및 지정 wire 계약 해소

기준 `d2e75a2f4e694440a0926b47e03b3c76ca0bc312`. 첨부 43파일을 각 지정 blob에서 복원·보존했다. 계약·계획 원문은 변경하지 않았다. 이번 신규 구현은 **고정 JDK HttpClient transport**이며, 이전의 최초 URL 정책이나 전면차단을 성공으로 바꾸지 않았다. 아래 두 지정 실패 case는 production transport 구현을 사용해 통과했다. Spring의 별도 transport까지 해결했다는 결과는 아니다.

### 확장 지점·구현 대안

고정 agent 2.28.0의 [InstrumentationModule](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/javaagent-extension-api/src/main/java/io/opentelemetry/javaagent/extension/instrumentation/InstrumentationModule.java) / TypeInstrumentation / TypeTransformer SPI로 `JdkTransportModule`을 등록한다. SDK provider 서비스와 별개인 `META-INF/services/io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule`을 추가했다. provided 의존성은 extension-api 2.28.0-alpha, instrumentation-api 2.28.0이다. SDK/Instrumenter는 agent가 제공하며 lib에 SDK 의존성·명시 inject를 추가하지 않았다.

[InstrumentationModuleInstaller](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/javaagent-tooling/src/main/java/io/opentelemetry/javaagent/tooling/instrumentation/InstrumentationModuleInstaller.java)의 `useIndy`는 distribution의 indy 플래그를 먼저 확인한다. `isIndyModule()` 선언만으로는 격리되지 않는다. 따라서 이번 모듈은 `otel.javaagent.experimental.indy=true`와 명시 helper 목록을 사용한다. 공식 [extension 예제](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/examples/extension/build.gradle.kts)도 helper/계측 의존성을 agent 쪽에 둔다. 조회 소스와 JDK bytecode는 출력 디렉터리에 보관했다.

| 경계 | 실제 구현 | 계약 대응 |
|---|---|---|
| MultiExchange 생성 | caller Context를 호출 객체 식별 기준으로 저장 | 최초 caller 부모를 각 hop/retry에 전달. 상위 CLIENT를 만들지 않음 |
| Exchange.responseAsync | 실제 request로 agent Instrumenter CLIENT 시작, 반환 future의 응답/예외에서 CAS 종료 | redirect 302 및 응답 전 연결 종료 retry를 각각 별도 span·완료로 취급 |
| Response 생성 | 실제 Exchange와 statusCode 필드로 응답 상태 연결 | 응답 없는 실패는 status 없이 failure, 302/204는 각 실제 상태 |
| HttpRequestImpl.getUserHeaders | 원본을 복사해 traceparent/tracestate/baggage를 대소문자 무관 제거. 그 request의 현재 URI host를 exact allowlist와 대조해 W3C tracecontext만 agent helper가 주입 | localhost→127.0.0.1 및 역방향 모두 각 hop의 목적지 기준. 원본/최초 URL·기존 헤더를 승인 근거로 쓰지 않음. baggage는 항상 제거 |
| agent→lib 완료 | processor MBean의 `transportComplete(Map)`가 JDK Notification/Map으로 전달. ObservedJdkHttpClient가 정본 완료 snapshot을 로깅 | agent만 HTTP span/주입 소유. lib의 공통 인터셉터에는 inject/삭제 없음. transport 활성 시 decorator의 외부 send 완료를 억제 |
| 수명 | attempt CAS, 호출 종료 정리, 실패·취소 시 잔여 attempt 정리, 객체 식별 Map, 호출/attempt 각각 100000 상한 | 같은 내용의 동시 요청끼리 snapshot을 공유하지 않음. 한도 초과는 명시 예외이며 가짜 성공 span/전파로 대체하지 않음 |

JDK HTTP/1.1 `Http1Request` 생성자와 HTTP/2 `Stream.headerFrame`의 bytecode 모두 `HttpRequestImpl.getUserHeaders()`를 읽는 것을 확인했다. 이 공통 accessor를 수정하지만 **실제 wire 검증은 아래 HTTP/1.1 입력뿐**이다. HTTP/2를 시험 통과로 표기하지 않는다. 응답 헤더 수신을 attempt 종료 경계로 잡으며 body 소비 이후 실패까지 확인한 결과도 아니다.

모듈은 기본 off다. 명시 활성화 시 기존 `otel.instrumentation.java-http-client.enabled=false`가 아니면 provider가 거부한다. `otel.javaagent.experimental.indy=true`도 필수다. JDK runtime은 검증 대상인 `21.0.12.1+1-1-24.04.4-Ubuntu`와 정확히 같아야 한다. 운영 JDK 지원을 추정하지 않는다. 기존 generic propagator fallback은 다른 transport에 그대로 남아 있다.

### 지정 검증과 명령 결과

검증 이유: 실제 구현을 바꿨으므로, 0622에서 실패한 양방향 redirect·숨은 retry와 잔존 헤더의 두 최소 case만 실행했다. 시험 fixture의 입력·assertion은 완화하거나 변경하지 않았다. runner만 새 production 모듈 서비스·교체 설정을 포함하도록 변경했다. 마지막 정적 정리에서 교체 설정은 지정 두 Main에만 활성화하도록 제한했다. 이전 고정 harness의 fallback 설정은 유지한다.

```sh
bash ./mvnw -B -pl lib,agent-extension -am -Dtest=TransportAttemptContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

| 출력 로그 | 결과·원인 |
|---|---|
| c3-0627-transport.log | 2 failure. wire 전에 JdkTransportState NoClassDefFoundError. 격리 advice 활성 플래그 누락 |
| c3-0627-isolated.log | 2 failure. 격리 loader는 사용하나 helper 명시 등록 누락으로 같은 클래스 로드 실패 |
| c3-0627-helpers.log | helper 등록 수정 후 2 tests / 0 failure / 0 error / 0 skipped, BUILD SUCCESS, 2026-10-20T21:31:07Z |

성공 wire 근거:

- `TRANSPORT EVIDENCE wire=6 approved=4 approvedTrace=4 deniedLeak=0 completions=6 clientSpans=6`.
- 양방향 redirect 각 2 wire, hidden retry 2 wire. 승인 전파 0/4→4/4, 완료/CLIENT 각 3→6, 비승인 누출 0 유지.
- 완료 로그는 302 두 건·204 세 건·응답 없는 HTTP_IO_ERROR 한 건. 중복 상위 send 완료가 추가되지 않았다.
- `RESIDUAL EVIDENCE deniedHeaderKinds=0`, `RESIDUAL CONTRACT PASS`. 기존 합성 traceparent/tracestate/baggage 3종의 비승인 redirect 잔존 3→0.
- agent artifact는 기존 SHA256 `130606aed07f101458fe42b8f453ef3a2536f6bce8a7ae64f222f5688ada2500` 검사를 두 case에서 통과했다. 공개 Maven Central artifact만 준비했다.

통과 뒤 정적 검토에서 Map의 값 동등성으로 동시 요청이 섞일 가능성과 호출 Context 정리 누락을 발견했다. 객체 식별 Map·호출 종료/실패/취소 정리·100000 상한을 추가했다. **이 마지막 수명 보완 뒤 동일 wire 입력은 재실행하지 않았다.** 따라서 위 wire pass는 helper 등록까지의 구현에 대한 근거이고, 마지막 보완은 아래 컴파일 근거로 구분한다.

검증 이유: 마지막 수명 보완에 추가된 advice의 Java 타입 연결만 확인하고 pass한 시험은 반복하지 않기 위함이다.

```sh
bash ./mvnw -B -pl lib,agent-extension -am -DskipTests test-compile
```

`c3-0627-cleanup-compile.log`: BUILD SUCCESS, 2026-10-20T21:32:45Z. 시험 실행 없음. 기존 pass 입력·sample-api·전체 회귀·교차검토는 실행하지 않았다.

### 잔여·지원 한계와 충돌 파일

- 지정 JDK HTTP/1.1 실패 case를 해결했다. Spring SimpleClientHttpRequestFactory의 HttpURLConnection 등 **별도 transport 모듈은 미구현**이고 generic 전면차단이 남는다. JDK 모듈 활성화를 Spring 전파 승인으로 해석하지 않는다. 이번 좁은 완료 근거는 0622의 두 지정 JDK case에 한정한다.
- HTTP/2 실제 wire, async/cancel/timeout/body 오류, 용량 한도/동시 요청, 최종 수명 보완 이후 wire는 미검증이다. API/bytecode 경계 및 최종 컴파일만 확인한 항목과 실제 wire pass를 구별한다.
- 완료 bridge는 ObservedJdkHttpClient 클래스가 설치한 listener를 사용한다. 해당 클래스 초기화 이전 raw HttpClient 호출의 완료 로그는 보장하지 않는다. agent 자체의 CLIENT/헤더 정책과 lib 완료 listener 설치는 별개다.
- §1·§2 전체 인수 및 Spring까지 포함한 배포 승인은 미완료다. §3·§4·§5·§7은 후속, §6 설계 유지. push·태그·Publish·타 저장소·GS 수정·배포·운영/자격 사용 없음.
- 이번 추가 충돌: `agent-extension/pom.xml`, `LineageAutoConfigurationCustomizerProvider`, `SpanLineageSpanProcessor`, 신규 `agent/transport/{JdkTransportModule,JdkTransportAdvice,JdkTransportState}.java`, InstrumentationModule 서비스, `ObservedJdkHttpClient`, `FixedAgentHarnessTest`, 이 보고서. 첨부 43파일도 누적 patch에 유지한다. HarnessExporter는 production 서비스에 등록하지 않았다.

### 0627 이슈대처 후보

- 증상: `NoClassDefFoundError: ai/mutuus/common/agent/transport/JdkTransportState`.
- 원인: agent 2.28.0의 격리 advice는 indy 활성 플래그와 helper 등록을 함께 요구한다. deprecated isIndyModule override만으로는 충분하지 않았다.
- 확인 명령: 위 TransportAttemptContractTest 및 고정 InstrumentationModuleInstaller/InstrumentationModuleClassLoader 소스 조회. team-lesson search는 관련 해결 노트를 반환하지 않았다.
- 조치: `experimental.indy=true` 강제 게이트, isHelperClass 및 getAdditionalHelperClassNames 등록 후 두 지정 case pass.
- 재발 방지: extension 모듈의 helper loader 경로를 고정 agent 소스와 별도 JVM harness로 함께 증명한다. SDK provider가 로드된 사실만으로 transport helper도 로드된다고 판단하지 않는다.


## 0635 후속 — 최종 수명 wire·Spring transport·HTTP/2

기준 d2e75a2f4e694440a0926b47e03b3c76ca0bc312. 첨부0627의 47개 지정 blob을 그대로 복원한 뒤 이 절과 신규 transport/최소 fixture를 추가했다. 기준에 이미 있는 C1 변경을 중복 적용하지 않았다. 계약·계획·과거 보고서와 확정된 agent 2.28.0/SHA256, helper의 indy 활성·등록 해결 근거를 보존했다. 이 작업 사본과 누적 patch만 산출한다.

### 최종 수명 변경과 검증 입력의 분리

0627 wire pass 이후 변경은 JdkTransportState의 호출/attempt/request 객체 식별 Map, MultiExchange.responseAsync 종료 cleanup advice, 실패/취소 시 남은 attempt 정리 및 100000 상한이다. 첨부는 이 상태로 test-compile만 통과했다. 이번 첫 실행은 **첨부 그대로**의 TransportAttemptContractTest 두 메서드이며, 최종 수명 보완에 닿는 JDK HTTP/1.1 입력을 1회 검증했다.

결과: 2 tests, failures/errors/skipped=0. wire=6, approved=4, approvedTrace=4, deniedLeak=0, completions=6, clientSpans=6. 잔존헤더 deniedHeaderKinds=0. 이전 0627 pass와 이번 최종 수명 상태 pass를 같은 실행으로 합산하지 않는다.

이후 추가한 JDK 변경은 Spring JdkClientHttpRequestFactory의 비동기 관측 토큰 전달이다. MultiExchange 생성 시 dispatch token을 함께 저장하고, 각 실제 CLIENT 시작 시 processor에 연결한 뒤 호출 종료에서 token map도 제거한다. CLIENT snapshot은 onStart 실제 부모와 token의 기대 부모가 일치할 때만 연결한다. 이 변경에 닿는 신규 Spring JDK factory 입력이 실패 수정 후 통과했다. 기존 raw JDK pass 입력을 재실행하지 않았다.

### Spring transport 종류·버전 근거와 계약 대응

- parent/pom.xml은 Boot parent 4.1.0을 상속한다. 로컬에 해석된 spring-boot-dependencies-4.1.0.pom의 spring-framework.version은 **7.0.8**이다. extension 시험의 spring-web도 7.0.8이다.
- 계약 §2는 RestTemplate/RestClient를 요구하지만 특정 request factory나 Apache/Reactor 버전을 지정하지 않는다. 기존 FixedAgentMain이 명시한 SimpleClientHttpRequestFactory를 우선 대상으로 삼았다.
- spring-web-7.0.8.jar의 SimpleClientHttpRequestFactory.createRequest/openConnection bytecode는 URL.openConnection → HttpURLConnection → SimpleClientHttpRequest를 사용한다. JdkClientHttpRequestFactory는 java.net.http.HttpClient를 받아 JdkClientHttpRequest를 만든다. Spring 자체를 하나의 transport라고 가정하지 않았다.
- 이번 구현·wire 확인 범위는 **RestTemplate + Simple**, **RestClient + Simple**, **RestClient + JDK**다. Apache HttpComponents/Reactor/Jetty factory는 이 계약/POM에서 고정된 사용 입력이 없고 구현·승인하지 않았다. 임의 버전을 의존성에 추가하지 않았다.
- Simple용 신규 UrlTransportModule은 agent 2.28.0 InstrumentationModule/TypeInstrumentation SPI를 사용한다. pinned JDK의 sun.net.www.http.HttpClient.writeRequests(MessageHeader, PosterOutputStream)가 최초 전송과 내부 retry의 공통 경계다. 3인자 overload는 최초 진입용이며 숨은 retry가 2인자를 직접 호출하므로 최종 hook은 2인자다.
- 실제 HttpClient.url과 요청 method를 각 attempt에서 읽고 새 CLIENT를 agent Instrumenter로 시작한다. MessageHeader의 traceparent/tracestate/baggage를 대소문자 무관 제거한 뒤 정확한 최종 host 승인 시 agent W3C propagator만 주입한다. baggage는 차단한다. parseHTTPHeader의 실제 status/예외와 재전송 시작 시 이전 실패 attempt를 CAS 종료한다.
- JdkTransportModule은 Spring JDK factory에도 동일한 각-hop 경계를 적용한다. lib interceptor에는 inject/헤더 삭제/span 생성이 없다.
- SpanLineageSpanProcessor는 실제 transportComplete를 받은 observation token만 표시한다. HttpCompletionScope는 listener 설치와 그 token의 실제 완료가 모두 확인된 경우에만 외부 interceptor 완료를 생략한다. 전체 Spring 호출을 설정값 하나만으로 생략하지 않는다. token은 finishClient에서 snapshot/표시와 함께 제거하며 shutdown에서도 비운다.
- 두 모듈은 기본 off다. 공통으로 experimental.indy=true 및 정확한 JDK runtime 21.0.12.1+1-1-24.04.4-Ubuntu를 요구한다. JDK 모듈은 java-http-client=false, URL 모듈은 http-url-connection=false가 필수다. URL 모듈의 MessageHeader 접근은 **--add-exports=java.base/sun.net.www=ALL-UNNAMED**를 추가로 요구하며 누락 시 provider가 거부한다. 기본 계측과 새 transport 계측을 중복 활성화하지 않는다.
- helper loader 경계 때문에 URL helper는 java.net.http를 참조하지 않는 별도 Instrumenter<URL,Integer>다. JDK helper를 bootstrap transport에서 재사용하지 않는다. production 서비스에는 UrlTransportModule만 추가했고 HarnessExporter는 test jar에만 둔다.

### 실제 HTTP/2 wire

Python h2 4.3.0의 loopback TLS 서버가 ALPN h2를 협상하고 실제 HTTP/2 HEADERS를 받는다. Java fixture는 응답 version=HTTP_2도 확인한다. 양방향 redirect와 REFUSED_STREAM(error 7) 뒤 JDK 내부 retry를 사용한다. 사전 합성 traceparent/tracestate/baggage도 redirect 입력에 포함한다. HTTP/1.1 bytecode만 보고 HTTP/2 성공으로 판정하지 않았다.

결과: wire=6, approved=4, approvedTrace=4, deniedLeak=0, completions=6, clientSpans=6. 별도 새 비승인 retry 입력도 wire=2, approvedTrace=0, deniedLeak=0, completions=2, clientSpans=2다. 서버 evidence에는 헤더 값 대신 승인 여부와 헤더 존재 boolean만 저장한다. TLS 인증서/키는 로컬 시험용으로 생성하며 target 안에만 두고 patch/출력 산출물에 포함하지 않는다. trust-all은 테스트 Main에만 있다.

### 최소 명령·로그·결과

공통 명령:
```sh
bash ./mvnw -B -pl lib,agent-extension -am '-Dtest=<선택>' -Dsurefire.failIfNoSpecifiedTests=false test
```

| 로그(TEAM_OUT_DIR) | 선택·필요 이유 | 결과 |
|---|---|---|
| a13-0635-jdk-wire.log | TransportAttemptContractTest: 첨부 최종 수명 변경의 지정 JDK wire 2개 | 2 pass, 21:36:38Z |
| a13-0635-h2.log | Http2TransportContractTest: 새 HTTP/2 redirect·REFUSED_STREAM retry | 1 pass, 6 wire/CLIENT/완료 |
| a13-0635-spring.log | SpringTransportContractTest (당시 Simple 메서드 1개): 신규 실제 Spring transport | 실패: bootstrap helper가 java.net.http.HttpRequest를 로드할 수 없음 |
| a13-0635-spring-loader-fix.log | 위 실패 원인만 수정 | 1 failure: 6 wire지만 CLIENT/완료 5. 숨은 retry가 3인자 hook을 우회 |
| a13-0635-spring-retry-fix.log | 2인자 공통 전송 hook으로 수정한 실패 case만 | 1 pass, 승인4/4·누출0·CLIENT/완료 각6 |
| a13-0635-spring-residual.log | SpringTransportContractTest#restClientRemovesPreviouslyPresentHeadersAndAvoidsOuterCompletion: 신규 RestClient·혼합 대소문자 잔존헤더 및 실제 완료 기반 중복 억제 | 1 pass, 승인4/4·누출0·CLIENT/완료 각6 |
| a13-0635-spring-jdk.log | SpringTransportContractTest#jdkFactoryHasAgentAttemptCompletionWithoutInterceptorDuplicate: 신규 Spring JDK factory 비동기 경계 | 1 failure: CLIENT6/완료9. dispatch token이 다른 스레드 attempt에 연결되지 않음 |
| a13-0635-spring-jdk-token-fix.log | 위 token 전달 실패만 수정 | 1 pass, 승인4/4·누출0·CLIENT/완료 각6 |
| a13-0635-denied-retry.log | DeniedTransportRetryTest: 신규 비승인 목적지 retry 두 transport | testCompile 실패: 신규 Simple fixture의 제거한 redirect loop 끝 괄호가 남음. 시험 실행 없음 |
| a13-0635-denied-compile-fix.log | 위 미실행 입력의 fixture 문법만 수정 | 2 pass, 각 wire2·전파0·누출0·CLIENT/완료2 |

HTTP/2 준비는 저장소 루트에서:
```sh
bash agent-extension/src/test/python/prepare-h2.sh
```
Python 의존성은 target/h2-python에만 설치한다(requirements.txt: h2 4.3.0/hpack 4.2.0/hyperframe 6.1.0). OpenSSL/ Python3/pip가 필요하다. agent jar는 기존 공개 Maven Central 좌표와 고정 SHA256을 그대로 사용한다. runner가 모든 별도 JVM 실행 전 digest를 검사했다. 신규 runner는 관련 Main에서만 모듈을 켠다.

서로 다른 입력 8개가 통과했다. 실패한 원인을 바꾼 case만 재실행했으며 기존 pass 동일 입력·전체회귀·sample-api·교차검토는 실행하지 않았다. 최종 production 토큰 변경은 Spring JDK 신규 입력으로 확인했고, 앞선 HTTP/2 pass와 Simple pass를 변경 후 재실행한 결과로 둔갑시키지 않는다. 마지막 fixture 준비 변경은 stale HTTP/2 evidence 삭제와 재현용 준비 스크립트 추가뿐이며 추가 시험은 실행하지 않았다.

### 잔여·§1/§2 판정·충돌 파일

이번 brief의 최종 수명 wire, 지정 Spring transport 최소 구현, HTTP/2 로컬 wire 근거를 확보했다. **§1·§2 전체 인수는 미완료**다. HTTP/2 전반의 모든 오류/동시 multiplexing, cancel/timeout/body 소비 이후 오류, capacity 동시 경합, 프록시/인증 재시도/HTTPS URLConnection transport 등은 이번 최소 wire로 검증하지 않았다. 지원 범위를 다른 JDK/agent/transport로 확대하지 않는다. URL 모듈은 응답 헤더 경계를 사용하며 스트리밍 body 완료를 보장하지 않는다.

기존 자체 서버 span-owner 자동바인딩·실제 AdminHttpServer/EmailEventServer 적용, container 사전 거절 지표, 소비 앱 CORS 순서 및 실제 client disconnect 인수는 남는다. listener는 ObservedJdkHttpClient 또는 공통 Spring interceptor 초기화를 통해 설치한다. 이 경계 이전 raw client의 완료 로그는 여전히 보장하지 않는다. §3·§4·§5·§7은 후속, §6 설계 유지. 운영 배포/성능/OpenSearch/자격/GS 변경/발행은 하지 않았다.

추가 충돌 파일: JdkTransportState, SpanLineageSpanProcessor, LineageAutoConfigurationCustomizerProvider, InstrumentationModule 서비스, 신규 UrlTransportModule/Advice/State, lib AgentClientObservation/HttpCompletionScope/HttpClientLoggingInterceptor/ObservedJdkHttpClient, FixedAgentHarnessTest 및 신규 Spring/HTTP2/denied retry fixture·Python 준비 파일, 이 보고서. 기존 47파일 전체도 누적 patch에 보존한다. 계약 문서와 계획은 첨부 상태를 유지한다. POM 버전/발행 좌표 변경 없음.

### 0635 이슈대처 후보

- 증상: 지정 문서 No such file or directory. 원인: 기준 C1 사본에는 첨부 미적용. 확인: git cat-file -t 지정 blob. 조치: 첨부 47 blob 복원. 예방: 분리 HEAD의 기준과 첨부 적용 상태를 구별한다. 증상 검색은 관련 해결 자료를 반환하지 않았다.
- 증상: ClassNotFoundException: java.net.http.HttpRequest. 원인: HttpURLConnection bootstrap 대상의 isolated helper에서 platform-loader java.net.http에 연결. 확인: 최초 Spring wire와 loader stack. 조치: URL 전용 helper를 java.base/agent API로 분리. 예방: 같은 agent라도 대상 클래스 loader가 다르면 helper 가시성을 따로 확인한다.
- 증상: wire6/CLIENT5/완료5. 원인: HttpClient 숨은 retry가 writeRequests 2인자를 직접 호출. 확인: pinned javap bytecode 및 Spring wire. 조치: 최초/재전송 공통 2인자 hook. 예방: 최초 호출 overload만 보지 않고 retry의 실제 call site를 대조한다.
- 증상: Spring JDK CLIENT6/완료9. 원인: thread-local observation token이 비동기 attempt로 전달되지 않음. 확인: 신규 JDK factory wire. 조치: MultiExchange 식별에 token 전달, 실제 onStart 부모 일치 snapshot과 연결. 예방: async transport의 중복 억제는 설정값 대신 해당 호출의 실제 완료 증거로 결정한다.
