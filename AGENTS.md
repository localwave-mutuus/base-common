# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 프로젝트 개요

`common-platform`의 **핵심 산출물은 단일 jar 공통 라이브러리**(`ai.mutuus.common:common-platform`)다. 타 API 서비스가 Maven 의존성으로 추적/로깅/예외/i18n/인증/세션 기능을 재사용한다. 실행 가능한 애플리케이션이 아니라 **소비 서비스에 흡수되는 라이브러리**라는 점이 모든 설계 결정의 근간이다. 소비 편의를 위해 **BOM(`common-platform-bom`)과 큐레이션 스타터(`common-platform-starter-web`/`-batch`)를 함께 발행**한다 — 소비자는 스타터 1개로 표준 스택을, BOM 으로 버전을 관리한다.

- **레포 레이아웃은 Maven reactor**다(루트 `pom.xml`은 `packaging=pom` aggregator, 게시 대상 아님). 모듈 셋:
  - `lib/` — **산출물 라이브러리**(`ai.mutuus.common:common-platform`). 라이브러리 소스/리소스는 전부 `lib/src/...`에 있다.
  - `parent/` — **`common-platform-parent`**(`packaging=pom`). 게시 모듈(bom·lib·starters)의 공용 부모 — **게시 좌표(Nexus distributionManagement)·배포 정책(`deployAtEnd`)·공통 버전(java/logstash/springdoc)을 한 곳에서** 관리(자식에 복붙되던 중복 제거). 소비자가 lib/starter pom 을 해석할 때 필요하므로 parent 도 게시 대상.
  - `bom/` — **`common-platform-bom`**(`packaging=pom`). 자사 아티팩트(lib·스타터들) + 라이브러리가 노출하는 optional 3rd-party(logstash·springdoc) 버전 + **`spring-modulith-bom` import**(소비자가 모듈러 모놀리스 구성 시 `spring-modulith-*`를 버전 없이 추가)를 한 곳에서 고정 → 소비자는 버전 없이 추가, CVE/상향은 BOM 한 곳만 올리면 일괄 롤아웃. 공통 버전(java/logstash/springdoc/spring-modulith)의 단일 출처는 `common-platform-parent`.
  - `starters/starter-web`, `starters/starter-batch` — **큐레이션 스타터**(코드 없는 의존성 취합 jar, `spring-boot-maven-plugin skip`). web = lib + 표준 웹 스택(web/security/oauth2/actuator/validation/restclient/aspectj) + JSON 로깅(logstash); batch = lib + JSON 로깅(web/security 제외 → 비전이 유지). 소비자 온보딩 1줄.
  - `samples/sample-api`, `samples/sample-batch` — 라이브러리를 끌어쓰는 **소비 검증용 샘플**(아래 "테스트 위치" 규칙). 현재 샘플은 **스타터+BOM 경유**로 소비한다(sample-api→`starter-web`, sample-batch→`starter-batch`, 둘 다 `common-platform-bom` import). **sample-api 는 Spring Modulith 로 구조화**돼 있다 — 최상위 패키지(board·demo·logs·persistence·web)를 모듈(=바운디드 컨텍스트)로 `@ApplicationModule`(package-info)로 선언하고 `ModulithVerificationTest`(`ApplicationModules.verify()`)로 경계를 잠근다. 모듈 간 비동기 이벤트(`@ApplicationModuleListener`)에서도 공통모듈 `TraceContextTaskDecorator` 로 traceId 가 전파된다(회귀 `ModulithEventTracePropagationIntegrationTest`). 상세는 [docs/260702.010.MODULITH_INTEGRATION.md](docs/260702.010.MODULITH_INTEGRATION.md).
  - 이 reactor 구조의 이유: IDE(jdt.ls)가 라이브러리와 샘플을 한 창에서 각각 별도 프로젝트로 임포트하려면, 라이브러리를 `lib/` 하위로 내려 samples 와 형제로 둬 Eclipse 의 '프로젝트 위치 중첩 금지'를 피해야 한다. 또한 reactor 빌드 시 samples 가 lib 를 .m2 stale jar 가 아니라 방금 빌드한 모듈에서 해석한다. 단, **`samples` 가 reactor 모듈이 됐어도 "독립 소비자" 충실도는 유지**된다 — 샘플 pom 은 여전히 `spring-boot-starter-parent` 를 상속하고 라이브러리를 GAV 의존으로만 참조한다(라이브러리 내부에 결합하지 않음).
- Java 21 / Spring Boot 4.1.0 (Spring Framework 7) / Spring Modulith 2.1.0
  - 베이스라인은 **JDK 21 고정**이다(`parent/pom.xml`의 `java.version`/`maven.compiler.release`). 브랜치명·일부 커밋에 "Java 25"가 보여도 작업 트리는 안정성·소비 서비스 강제 회피를 이유로 21로 되돌려져 있다 — 산출물 타깃은 21이다.
- 라이브러리 기능은 `ai.mutuus.common` 아래 **패키지로만** 구분된다(라이브러리 내부엔 별도 Maven 모듈 없음). README 등에서 `common-web`, `common-core`로 부르는 것은 모듈이 아니라 패키지를 가리킨다.

## 빌드 / 테스트 명령

Maven Wrapper(`./mvnw`)가 포함돼 있고 **Maven 3.9.9로 고정**(`.mvn/wrapper/maven-wrapper.properties`)이라 로컬 Maven 설치 없이 재현 가능하게 빌드된다. JDK 21만 있으면 된다(아래 명령의 `mvn`은 `./mvnw`로 대체 가능). 루트에서의 `./mvnw`는 **reactor 전체**(parent → bom → lib → starters → samples)를 대상으로 한다.

```bash
# 라이브러리만 (대부분의 라이브러리 작업은 이걸로 충분)
./mvnw -pl lib clean install     # lib 만 컴파일 + 로컬 .m2 설치 (라이브러리 모듈엔 테스트 없음)

# 테스트 후 배포(한 방): deployAtEnd 라 리액터 전체(샘플 테스트 포함) 성공 후에만 게시 모듈
#   (parent·bom·lib·starters)을 일괄 업로드. samples·aggregator는 deploy.skip.
#   Nexus 가 self-signed 라 MAVEN_OPTS 로 커스텀 truststore 지정 필요 → 메모리 nexus-publishing.
./mvnw clean deploy

# reactor 전체 (bom → lib → starters → samples 순서로 빌드, 샘플 테스트까지 실행)
./mvnw clean install             # 전 모듈 설치 후 samples 빌드/테스트 — 라이브러리+소비검증 일괄
```

**테스트는 라이브러리 모듈이 아니라 소비 서비스(`samples/*`)에서 수행한다(아래 "테스트 위치" 규칙 참조).** reactor 빌드(`./mvnw clean install`/`test` 를 루트에서)는 samples 가 라이브러리를 **방금 빌드한 lib 모듈**에서 해석하므로, 라이브러리를 고친 뒤에도 별도 재설치 없이 한 번에 검증된다.

```bash
./mvnw clean test                                      # 0) reactor 전체: lib 빌드 후 두 샘플 테스트 일괄
cd samples/sample-api   && ../../mvnw clean test       # 2a) 웹 소비자만 테스트
cd samples/sample-batch && ../../mvnw clean test       # 2b) 비웹 소비자만 테스트 (비전이 검증)
cd samples/sample-api && ../../mvnw -Dtest=ClassName test            # 단일 클래스
cd samples/sample-api && ../../mvnw -Dtest=ClassName#methodName test # 단일 메서드
```

> **샘플만 단독으로** 돌릴 때(`cd samples/sample-api && ../../mvnw test`)는 라이브러리를 reactor 가 아니라 **.m2 에 설치된 jar** 로 해석한다. 따라서 라이브러리 API(시그니처/타입)를 바꾼 뒤 샘플을 단독 실행하려면 **먼저 `./mvnw -pl lib install` 로 재설치**하고, 샘플에서 **반드시 `clean`** 한다(소스가 안 바뀐 테스트는 Maven 이 재컴파일을 건너뛰어 stale `.class` 재사용 → `NoSuchMethodError`). 루트에서 reactor 로 돌리면 이 함정이 없다.

`lib/pom.xml`의 `spring-boot-maven-plugin`은 `<skip>true</skip>`로 설정돼 있다 — 라이브러리이므로 **실행 가능 jar로 repackage하지 않는다**. 이 설정을 건드리지 말 것.

## 핵심 아키텍처

### 1. optional 의존성 + 조건부 자동구성 (가장 중요한 원칙)

라이브러리 jar에는 **모든 기능 코드가 포함**되지만, 통합 의존성(web/security/actuator/otel/redis 등)은 `lib/pom.xml`에서 전부 `<optional>true</optional>`이다. 따라서 소비 서비스로 **강제 전이되지 않는다**.

각 자동구성은 `@ConditionalOnClass`로 보호되어, **소비 서비스의 classpath에 해당 starter가 있을 때만** 활성화된다. 예:
- `CommonWebAutoConfiguration` → `@ConditionalOnClass(DispatcherServlet.class)`
- `CommonSecurityAutoConfiguration` → `@ConditionalOnClass(SecurityFilterChain.class)`

**새 통합 기능을 추가할 때는 반드시 이 패턴을 따른다**: 의존성은 optional로, 자동구성은 `@ConditionalOnClass`(필요시 `@ConditionalOnProperty`/`@ConditionalOnMissingBean`)로 보호. 그래야 비(非)웹 배치 서비스가 이 라이브러리를 써도 web/security가 끌려 들어가지 않는다.

### 2. 자동구성 등록 (Spring Boot 3+ 방식)

자동구성 클래스는 `lib/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`에 한 줄씩 등록한다. **새 `@AutoConfiguration` 클래스를 만들면 이 파일에도 추가해야** 활성화된다.

별도로 `META-INF/spring.factories`에는 `EnvironmentPostProcessor`만 등록돼 있다(컨텍스트 초기화 *이전*에 동작해야 해서 이 메커니즘이 별도로 필요).

### 3. 설정 기본값 주입 흐름

`CommonEnvironmentPostProcessor`가 Boot 컨텍스트 초기화 **전에** 기본 프로퍼티(`management.tracing.*`, `spring.messages.*`, `mutuus.common.*` 등)를 Environment에 `addLast`(= 최저 우선순위)로 주입한다. 따라서 소비 서비스의 `application.yml`이 같은 키를 정의하면 **항상 애플리케이션 값이 우선**한다. 새 "convention" 기본값은 여기서 추가한다.

### 4. e2e 추적 정보 계층 (web 패키지의 핵심)

`X-Trace-Id`, `X-Span-Id`, `X-Screen-Id`, `X-Event-Id`, `X-Device-Level`, `X-Device-Id`, `X-User-Id`, `X-Locale` 8개 + 인스턴스 식별 `X-App-Code`(4자리)·`X-Instance-Id`(6자리) 커스텀 헤더로 화면→이벤트→API 체인을 추적한다. 헤더 상수와 전파 목록은 `core/HeaderNames.java` 한 곳에 정의 — **헤더를 추가/변경하면 여기부터 수정**한다.

- **어플리케이션코드(`app-code`, 4 alnum)·인스턴스구분코드(`instance-code`, 6 alnum)**: `mutuus.common.*` 프로퍼티. **구동 전 `CommonEnvironmentPostProcessor`가 해석**(미지정 시 app-code는 service-name에서 결정적 도출, instance-code는 자동 생성)하고 포맷 검증 후 **시스템 프로퍼티**(`mutuus.appCode`/`mutuus.instanceCode`/`mutuus.logFileBase`)로 노출한다 — logback이 로그 파일명/필드에서 `${...}`로 읽기 위함(컨텍스트·로깅 초기화 *이전* 시점이라 시스템 프로퍼티가 필요). 두 코드는 `TraceFilter`가 응답 헤더 + MDC/TraceContext에 싣고(→ 로그 포함 + 아웃바운드 전파), 로그 파일명 `<app>-<instance>.log`에 쓰인다.

흐름:
1. **인입**: `TraceFilter`(`@Order(HIGHEST_PRECEDENCE)`, `/*`)가 헤더를 추출해 `TraceContext`(ThreadLocal) + SLF4J `MDC`에 적재. `X-Trace-Id`가 없으면 신규 UUID 생성. 응답에 `X-Trace-Id` 회신. **요청 종료 시 `finally`에서 반드시 `MDC.clear()` + `TraceContext.clear()`** (ThreadLocal 누수 방지 — 이 정리를 빠뜨리지 말 것).
2. **아웃바운드**: `HeaderPropagationInterceptor`(`ClientHttpRequestInterceptor`)가 `TraceContext`의 헤더를 하위 호출에 자동 부착. 단 `X-Span-Id`는 전파하지 않고 호출 구간마다 **새로 발급**한다. 이 인터셉터는 `CommonWebAutoConfiguration`의 중첩 설정이 등록하는 `RestClientCustomizer`/`RestTemplateCustomizer`를 통해 **Boot가 자동구성한 `RestClient.Builder`/`RestTemplateBuilder`에 자동 적용**된다(소비 서비스가 그 빌더로 만든 클라이언트는 무설정으로 전파됨). Boot 4에서 RestClient 자동구성이 별도 모듈로 분리됐으므로, optional 의존성 `spring-boot-restclient`가 소비 서비스 classpath에 있을 때만(`@ConditionalOnClass(RestClientCustomizer)`) 커스터마이저가 활성화된다. 회귀 가드: `samples/sample-api/OutboundPropagationIntegrationTest`(RANDOM_PORT 실서버).

   - **아웃바운드 타임아웃/재시도/에러디코딩(HTTP 클라이언트)**: 무한 대기 방지를 위해 `CommonEnvironmentPostProcessor`가 `spring.http.client.connect-timeout`(2s)·`read-timeout`(10s)를 **컨벤션 기본값**으로 주입한다(Boot 가 자동구성한 RestClient/RestTemplate 빌더에 적용, 소비자는 `spring.http.client.*`로 재정의). 아웃바운드 **재시도**는 `HttpRetryInterceptor`(멱등 GET/HEAD/OPTIONS의 `IOException`만, 상태코드 기반 아님)로 제공하되 **기본 OFF**(`mutuus.common.http.retry.enabled=true`로 opt-in, `max-attempts`). 에러디코딩: I/O 실패는 `ResourceAccessException`→`GlobalExceptionHandler`가 **타임아웃(SocketTimeout·HttpTimeout)→504**, 그 외 연결오류→502로 매핑. 회귀 가드: `HttpRetryInterceptorTest`·`HttpTimeoutIntegrationTest`(RANDOM_PORT).

`TraceContext`는 ThreadLocal 기반이라 스레드 경계를 자동으로 넘지 못한다. 이를 위해 `async` 패키지가 전파 수단을 제공한다: `TraceContextPropagation.wrap(Runnable/Callable)`로 직접 만든 스레드 풀에 적용하고, `@Async` 기본 실행기에는 `CommonAsyncAutoConfiguration`이 등록하는 `TraceContextTaskDecorator`(Boot의 task executor 자동구성이 단일 `TaskDecorator` 빈을 자동 적용)로 추적ID/사용자/MDC가 유지된다. `mutuus.common.tracing-enabled=false`면 전파도 비활성화된다.

### 5. 표준 예외 응답

`GlobalExceptionHandler`(`@RestControllerAdvice`)가 모든 예외를 표준 `ApiResponse` 봉투로 변환한다. `BusinessException`은 **`ErrorCode` 인터페이스**(`code`/`status`/`messageKey`)를 들고 다닌다 — 라이브러리 기본 코드는 **`CommonErrorCode` enum**이 구현하고, **소비 서비스는 `ErrorCode`를 구현한 자체 enum으로 도메인 코드를 추가**할 수 있다(라이브러리를 안 건드리고 동일 봉투/흐름으로 처리 — 회귀 가드 `samples/sample-api/CustomErrorCodeIntegrationTest`, 소비자 예 `ai.mutuus.sample.demo.SampleErrorCode`). 응답 메시지는 `MessageResolver`(i18n)로 로케일별 변환, `traceId`/`screenId`/`timestamp`를 부가 프로퍼티로 첨부한다. **라이브러리 에러 추가 = `CommonErrorCode` 항목 + `messages*.properties` 키**, **소비자 에러 추가 = 자체 `ErrorCode` 구현 enum + 자체 messages 번들**(`spring.messages.basename` 확장)이 한 세트다.

**성공 응답 자동 래핑(opt-in, `response` 패키지)**: `mutuus.common.response-wrapper.enabled=true`면 `ApiResponseWrapperAdvice`(`ResponseBodyAdvice`)가 컨트롤러가 반환한 **평범한 객체를 표준 `ApiResponse.ok(...)` 봉투로 자동 래핑**한다(컨트롤러는 `return dto;`만으로 봉투 응답). **이미 `ApiResponse`/`ProblemDetail`, `String`/`byte[]`/`Resource`, 비(非)JSON, 제외 경로**(`exclude-path-prefixes` 기본 `/actuator`·`/v3/api-docs`·`/swagger-ui`)는 손대지 않는다(이중 래핑·직렬화 파손 방지 — springdoc OpenAPI 스펙이 깨지지 않는 이유). **기본 OFF**(응답 규약을 바꾸는 오지랖이라 명시적 opt-in). 회귀 가드 `samples/sample-api/ResponseWrapperIntegrationTest`.

**공통 OpenAPI 문서(opt-in, `openapi` 패키지)**: 소비자가 springdoc 를 classpath 에 추가하면 `CommonOpenApiAutoConfiguration`(`@ConditionalOnClass(OpenAPI)`)이 공통 `OpenAPI` 빈(info + **Bearer JWT 보안 스킴**, 자원 서버 JWT 와 정합 → Swagger UI 의 Authorize)을 제공하고 springdoc 이 이를 베이스로 경로/스키마를 채운다. `@ConditionalOnMissingBean` 이라 소비자가 자체 `OpenAPI` 빈을 정의하면 비켜선다. springdoc 은 lib 에서 optional(`springdoc-openapi-starter-common`), 버전은 `common-platform-parent`/BOM 의 `springdoc.version`. 회귀 가드 `samples/sample-api/OpenApiCommonConfigIntegrationTest`.

**표준 페이징 응답(`api/PageResponse<T>`)**: 목록 API 가 페이지 메타(`content`/`page`/`size`/`totalElements`/`totalPages`/`first`/`last`/`numberOfElements`)를 일관 형태로 반환하는 **순수 DTO**(spring-data 비의존이라 어디서나 사용). 수동 페이징은 `PageResponse.of(content, page, size, totalElements)`, Spring Data `Page` 는 `persistence/PageResponses.from(page)`(optional 유틸, 호출 시점에만 로드되어 순수성 유지)로 변환. 보통 `ApiResponse` 의 `data` 로 실린다(성공 응답 자동 래핑과 결합). 회귀 가드 `PageResponseTest`·`PageResponsesTest`·`PageResponseIntegrationTest`.

### 6. 보안 (인증 위임 모델)

이 라이브러리는 인증을 **수행하지 않는다**. 별도 인증/인가 MSA가 발급한 JWT를 검증하는 OAuth2 Resource Server 기본 설정만 제공한다(`CommonSecurityAutoConfiguration`). `SecurityFilterChain`/`JwtAuthenticationConverter`는 `@ConditionalOnMissingBean`이라, **소비 서비스가 자체 빈을 정의하면 즉시 대체**된다. permit-all 경로·roles claim·authority prefix는 `mutuus.common.security.*`로 설정.

인증 확정 후 `AuthenticatedUserContextFilter`(`AuthorizationFilter` 뒤)가 **검증된 인증 주체**(JWT subject)를 `TraceContext`/MDC `X-User-Id`에 반영한다 — 인입 헤더 위장값보다 우선하므로 이후 모든 로그가 신뢰 가능한 사용자 식별자를 갖는다.

**보안 감사 로깅(`security/audit` 패키지, 기본 ON)**: 인증/인가 위배를 트래픽 로그와 **분리된** 전용 로거 `ai.mutuus.common.security.audit`(`SecurityAuditLogger`)로 구조화 기록한다 — 401 `security.authn.failed`, 403 `security.authz.denied`(principal·clientIp·reason 포함). 401/403 은 필터 단계(`Logging{AuthenticationEntryPoint,AccessDeniedHandler}`)와 **MVC 단계**(`SecurityExceptionAdvice`, `@Order(HIGHEST_PRECEDENCE)`) 양쪽에서 잡는다 — 특히 `@PreAuthorize` 등 메서드 보안이 던지는 `AccessDeniedException` 이 `GlobalExceptionHandler`(Exception 포괄)에 걸려 **500 으로 오분류되던 것을 403 + 보안 로그로 바로잡는다**(AOP 유무와 무관하게 일관). 토글 `mutuus.common.security.audit.enabled`. 검증 기준 화면 `/demo/security.html` + 회귀 가드 `samples/sample-api/SecurityAuditLoggingIntegrationTest`. 보안 하드닝 로드맵(**Phase 0~3 전부 완료** — 신뢰경계·JWT audience·정보 미노출·전파 allowlist·roles/no_authorities·startup 설정 점검·CSRF 조건부·보안 응답 헤더)은 [docs/260702.006.SECURITY_HARDENING.md](docs/260702.006.SECURITY_HARDENING.md).

### 7. API 생명주기 자동 로깅 (logging 패키지)

소비 서비스는 **별도 코드 없이** 라이브러리가 지정한 컴포넌트를 거치며 4개 지점에서 자동 로깅된다. 중심은 `AccessLogger`(단일 진입점, 로거 이름 `ai.mutuus.common.access`) — SLF4J 2 fluent API(`addKeyValue`)로 구조화 필드를 남기고 logstash 인코더가 JSON으로 렌더한다(추적ID/사용자는 MDC 경유 자동 포함).

`lib/src/main/resources/logback-common.xml`은 **JSON 콘솔 + 롤링 파일** appender를 제공한다(파일명 `<app-code>-<instance-code>.log`, 크기 100MB+일자 롤링, `${LOG_DIR:-logs}`). 소비자가 `logback-spring.xml`에서 `<include resource="logback-common.xml"/>`하면 활성화되며, **logstash 인코더는 optional이라 소비자가 `net.logstash.logback:logstash-logback-encoder`를 직접 추가**해야 한다(미추가 시 `ClassNotFoundException`으로 컨텍스트 기동 실패). 5개 시나리오는 `AccessLogFilter`(정상 유입/응답) + `GlobalExceptionHandler`(잘못된 포맷→`MALFORMED_REQUEST` 400, 비즈니스→`error.business`, 런타임→`error.server` 500, 네트워크/아웃바운드 실패 `ResourceAccessException`→502/504)에서 모두 추적키와 함께 남는다.

- **요청 수신/응답**: `AccessLogFilter`(order `HIGHEST_PRECEDENCE + 10`, 즉 `TraceFilter` 바로 뒤). 보안 체인을 **감싸므로** 응답 로그가 401/403을 포함한 최종 상태코드를 본다. → `request.received` / `request.completed`(5xx·느린 요청은 WARN)
- **인증 체크(실패)**: `CommonSecurityAutoConfiguration`이 `LoggingAuthenticationEntryPoint`(401)/`LoggingAccessDeniedHandler`(403)를 자원 서버 DSL에 주입. 둘 다 **기본 Bearer 핸들러에 위임**하여 `WWW-Authenticate` 등 표준 응답을 보존한다. → `auth.failure` / `auth.denied`
- **오류**: `GlobalExceptionHandler`가 `error.business`(WARN)/`error.server`(ERROR, 스택 포함) 기록.

`AccessLogger` 빈은 항상 제공(보안/예외 모듈이 `ObjectProvider`로 주입, 없으면 무동작 폴백)하고, 요청/응답 필터는 웹 환경에서만 등록한다. 토글·튜닝은 `mutuus.common.logging.*`(`enabled`, `include-query-string`, `exclude-path-prefixes`(기본 `/actuator`), `slow-request-threshold-millis`). 본문(body) 로깅은 PII·비용 문제로 기본 미포함. 회귀 가드: `samples/sample-api/AccessLoggingIntegrationTest`.

**추가 3계층(모두 기본 OFF, opt-in)** — 액세스 로깅이 못 보는 "컨트롤러 안쪽"을 점점 더 깊이 관측한다. 계층별 위치·이벤트·의존성:
- **입출력 본문(`payload` 패키지, 로거 `ai.mutuus.common.payload`)**: `PayloadLoggingFilter`(order `HIGHEST_PRECEDENCE+20`, AccessLogFilter 바로 안쪽)가 요청/응답 본문을 캐싱해 `request.payload`/`response.payload` 기록. 진입/종료 출력은 각각 `RequestPayloadLogger`/`ResponsePayloadLogger` 빈(교체 가능). 토글 `mutuus.common.payload-logging.*`. 문서 [260701.002.PAYLOAD_LOGGING.md](docs/260701.002.PAYLOAD_LOGGING.md).
- **컨트롤러 진입(`intercept` 패키지, 로거 `ai.mutuus.common.controller`)**: `ControllerEntryInterceptor`(MVC `preHandle`, 인자 역직렬화 **전**)가 명칭/패키지/URL 로 타게팅한 메서드 진입 시 `controller.entry` 기록. 판별(`ControllerMethodMatcher`)·동작(`ControllerEntryHandler`) 분리. 토글 `mutuus.common.controller-entry.*`. 문서 [260701.001.CONTROLLER_ENTRY.md](docs/260701.001.CONTROLLER_ENTRY.md).
- **메서드 인자/리턴(`aop` 패키지, 로거 `ai.mutuus.common.method`)**: `ControllerMethodLoggingAspect`(`@Around @within(@RestController)`, 인자 역직렬화 **후**)가 `method.enter`(인자 객체)/`method.exit`(리턴 객체) 기록. 출력은 `MethodLoggingWriter` 빈(교체 가능). **AOP라 소비자가 `spring-boot-starter-aspectj`(Boot 4에서 `-aop`→`-aspectj` 개명)를 추가해야만 활성**(`@ConditionalOnClass(ProceedingJoinPoint)`). 토글 `mutuus.common.method-logging.*`. 문서 [260701.005.METHOD_LOGGING.md](docs/260701.005.METHOD_LOGGING.md).

> 세 "진입" 관측의 깊이 차이가 핵심: 필터=바이트 본문 / 인터셉터=메서드 식별(인자 결정 전) / AOP=역직렬화된 값(인자 결정 후). 그래서 @Valid·JSON 파싱 실패는 `controller.entry`까지만 남고 `method.enter`는 안 남는다. **요청 유입 시 전체 로깅 구간의 케이스별 실측 순서**는 [260701.004.LOGGING_CASES.md](docs/260701.004.LOGGING_CASES.md)(회귀 가드 `PayloadAndEntryLoggingIntegrationTest` + `LoggingCaseMatrixIntegrationTest`).

**로그 민감정보 마스킹(opt-in, `mutuus.common.logging.masking.enabled=true`)**: `SensitiveDataMasker`(`core`, 정규식 매칭부를 마지막 4자만 남기고 마스킹)가 payload(`requestBody`/`responseBody`)·method(`args`/`return`) 로그에 남기 전에 카드번호·주민등록번호 등 PII 를 가린다. 기본 패턴(카드 13~16자리·주민번호)은 `CommonMaskingAutoConfiguration` 제공, 추가 패턴은 `mutuus.common.logging.masking.patterns`. **마스킹은 로깅 한정** — 실제 응답 본문은 원문 그대로다(마스킹 대상은 로그뿐). 기본 OFF, 소비자 자체 `SensitiveDataMasker` 빈으로 대체 가능. 회귀 가드 `SensitiveDataMaskerTest`·`MaskingIntegrationTest`.

### 8. 영속성 감사 (persistence 패키지, optional)

`spring-data-jpa`가 classpath에 있을 때만 활성화되는 optional 통합이다. `BaseEntity`(`@MappedSuperclass`)를 상속하면 `created_at`/`updated_at`(`Instant`)·`created_by`/`updated_by`가 JPA Auditing으로 자동 기록된다. **감사 주체(created_by 등)는 `TraceContextAuditorAware`가 `TraceContext`의 인증 사용자(`X-User-Id`)에서 가져온다** — 즉 보안 체인이 확정한 신뢰 사용자가 그대로 감사 컬럼에 박힌다(별도 코드 불필요).

- 식별자(@Id) 전략은 도메인마다 달라 `BaseEntity`가 **강제하지 않는다**(엔티티가 직접 정의).
- 감사 시각 필드는 `Instant`다 — Spring Data Auditing 기본 `DateTimeProvider`가 `OffsetDateTime`을 변환하지 못하므로 `Instant`(UTC)를 쓴다. **이 타입을 `OffsetDateTime` 등으로 바꾸지 말 것**(런타임 변환 예외).
- 자동구성은 둘로 나뉜다: `CommonPersistenceAutoConfiguration`(주체 제공자 `AuditorAware` 빈) + `JpaAuditingAutoConfiguration`(`@EnableJpaAuditing`, `HibernateJpaAutoConfiguration` 이후 정렬, `jpaAuditingHandler` 부재 시에만 활성화). 슬라이스 테스트가 실제 JPA 없이도 깨지지 않도록 **감사 활성화(@EnableJpaAuditing)는 주체 빈 등록과 분리**돼 있다.
- 토글: `mutuus.common.persistence.auditing-enabled=false`. 회귀 가드 셋: `PersistenceAuditingIntegrationTest`(H2, 항상 실행) + `PostgresAuditingIntegrationTest`(Testcontainers, Docker 없으면 skip) + `PostgresLiveAuditingIntegrationTest`(이미 떠 있는 로컬 실 PostgreSQL 대상, 접속·인증 가능할 때만 실행 — `support/LiveInfra#postgresReachable`). 후자는 생성에 더해 **수정 경로**(`updated_*` 갱신 + `created_*` 불변)까지 본다.
  - **시각 정밀도 주의**: Postgres `timestamptz` 는 마이크로초로 반올림하므로 JVM `Instant`(nanos)와 `isEqualTo` 로 비교하면 실 DB 왕복 후 깨진다 — 실 DB 테스트는 `isCloseTo(..., within(1, MILLIS))` 로 비교한다.

> **로컬 실 인프라 live 테스트 패턴**: Testcontainers 가드는 Docker 없는 환경에서 항상 skip 되므로, 이미 떠 있는 로컬 실 DB/Redis 를 대상으로 도는 `*LiveIntegrationTest` 를 별도로 둔다. 게이트(`samples/sample-api/.../support/LiveInfra`)는 포트 개방만이 아니라 **자격증명 인증까지** 성공해야 `@EnabledIf` 가 통과시킨다 → 인프라가 없거나 계정이 사라진 환경에선 에러가 아니라 **skip**. 좌표/자격은 `LiveInfra` 상수(기본값은 로컬 인프라, `-Dlive.*` 로 override)를 `@DynamicPropertySource` 로 컨텍스트에 주입해 단일 출처를 유지한다.

### 9. 분산 세션 컨벤션 (session 패키지, optional)

`spring-session-data-redis`가 classpath에 있을 때만 활성화되는 optional 통합이다. `CommonSessionAutoConfiguration`이 두 가지를 한다: (1) **저장소 활성화** — Redis 연결(`RedisConnectionFactory`) 자체는 Boot 의 redis 자동구성에 위임하되, **세션 저장소(`RedisSessionRepository`)는 라이브러리가 직접 켠다**(중첩 `RedisHttpSessionEnablement`가 `@Import(RedisHttpSessionConfiguration)`). (2) **컨벤션** — `SessionRepositoryCustomizer<RedisSessionRepository>`로 키 네임스페이스를 `<service-name>:session`(또는 `mutuus.common.session.namespace`), 기본 타임아웃을 `mutuus.common.session.timeout`(기본 30m)으로 잡아 여러 서비스가 같은 Redis 를 공유해도 키 충돌이 없게 한다.

> **Boot 4 주의(중요)**: Boot 4 는 스토어별 세션 자동구성을 **제거**했다(Boot 3 의 `RedisSessionConfiguration` 삭제, Spring Session 도 Boot 자동구성 미제공). 그래서 의존성만 얹어선 `RedisSessionRepository` 가 자동 생성되지 않는다 — **저장소 활성화를 라이브러리가 떠안는 이유**다(소비 서비스는 redis 세션 스타터만 추가하면 됨, 별도 `@EnableRedisHttpSession` 불필요). 소비 서비스가 자체 `SessionRepository` 를 정의하면 `@ConditionalOnMissingBean(SessionRepository.class)` 로 라이브러리 활성화가 비켜선다.

토글: `mutuus.common.session.enabled=false`, 사용자 커스터마이저 우선(`@ConditionalOnMissingBean(name="commonRedisSessionCustomizer")`). 회귀 가드 둘: `RedisSessionIntegrationTest`(Testcontainers, Docker 없으면 skip) + `RedisLiveSessionIntegrationTest`(이미 떠 있는 로컬 실 Redis 대상, 접속·인증 가능할 때만 실행 — `support/LiveInfra#redisReachable`). 후자는 네임스페이스에 더해 **타임아웃 컨벤션**까지 검증한다.

### 10. 멱등성 (idempotency 패키지, optional·opt-in)

같은 요청이 재시도로 중복 도착해도 서버 상태를 한 번만 바꾸게 하는 `Idempotency-Key` 컨벤션이다. `CommonIdempotencyAutoConfiguration`(`@ConditionalOnClass(DispatcherServlet)` + `@ConditionalOnProperty(mutuus.common.idempotency.enabled=true)`, **기본 OFF**)이 `IdempotencyFilter`를 `HIGHEST_PRECEDENCE + 15`(= `AccessLogFilter`(+10) 뒤, payload 필터(+20) 앞)에 등록한다.

- **동작 조건**: 요청 메서드가 대상 목록(`mutuus.common.idempotency.methods`, 기본 POST/PUT/PATCH)에 있고 **`Idempotency-Key` 헤더가 있을 때만** 개입한다 — 그 외에는 무개입 통과(GET·헤더 없는 요청은 영향 없음).
- **흐름**: 첫 요청은 `store.reserve`로 in-progress 마커를 원자 등록 후 처리하고, 응답을 `ContentCachingResponseWrapper`로 캡처해 `store.complete`로 저장. 같은 키의 이후 요청은 저장된 첫 응답을 **재처리 없이 재방**(상태/Content-Type/본문 그대로 + `Idempotent-Replayed: true` 헤더). 아직 처리 중인 같은 키의 동시 중복은 **409**(`Idempotent-Replayed: in-progress`).
- **저장소 추상화**: `IdempotencyStore` 인터페이스(`reserve`/`find`/`complete`) + 기본 `InMemoryIdempotencyStore`(단일 인스턴스, TTL 관리). **분산(다중 인스턴스) 환경에서는 소비 서비스가 Redis 등 공유 저장소 구현을 빈으로 제공**하면 `@ConditionalOnMissingBean`으로 대체된다(인메모리는 인스턴스 간 공유 안 됨).
- 토글·튜닝: `mutuus.common.idempotency.*`(`enabled`, `header-name`, `ttl`(기본 24h), `methods`). 회귀 가드: `InMemoryIdempotencyStoreTest`(단위) + `samples/sample-api/IdempotencyIntegrationTest`(RANDOM_PORT — 같은 키→재방, 다른 키→새 응답, 키 없음→미적용). 상세(키 생성 책임·더블클릭 스코프·서버 발급 토큰 강화 패턴) → [260702.003.IDEMPOTENCY.md](docs/260702.003.IDEMPOTENCY.md).

### 11. 캐시 추상화 (cache 패키지, optional·opt-in)

`@Cacheable` 등 Spring Cache 추상화에 **Redis `CacheManager` 컨벤션**을 얹는 optional 통합이다. `CommonCacheAutoConfiguration`(`@ConditionalOnClass`로 spring-data-redis의 `RedisCacheManager` + Boot 캐시 자동구성 `org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration` + `@ConditionalOnProperty(mutuus.common.cache.enabled=true)`, **기본 OFF**)이 두 가지를 한다:

- **캐싱 활성화** — `@EnableCaching`. Boot 4는 캐싱을 기본으로 켜지 않으므로(라이브러리가 대신 켠다), 소비 서비스는 `spring-boot-starter-cache` + `spring-boot-starter-data-redis`만 추가하고 프로퍼티로 켜면 된다(별도 `@EnableCaching` 불필요).
- **컨벤션 주입** — 사용자 정의 `org.springframework.data.redis.cache.RedisCacheConfiguration` 빈으로 기본 TTL(`mutuus.common.cache.ttl`, 기본 10m)·**키 프리픽스 `<service-name>:cache:`**(서비스 간 키 충돌 방지)·JSON 값 직렬화(`RedisSerializer.json()`)·null 캐싱 비활성을 잡는다. Boot의 Redis 캐시 자동구성이 이 빈을 `ObjectProvider`로 받아 `cacheDefaults`로 채택해 `RedisCacheManager`를 만든다.

- **Boot 4 주의**: 캐시 자동구성이 별도 모듈(`spring-boot-cache`, 패키지 `org.springframework.boot.cache.autoconfigure.*`)로 분리됐다(restclient처럼). 그래서 `@ConditionalOnClass`는 Boot 캐시 자동구성 클래스도 함께 요구해, 소비자가 캐시 스타터를 추가한 경우에만 활성화된다. `@EnableCaching`은 시작 시 Redis에 연결하지 않고(연결은 lazy), 실제 `@Cacheable` 호출 시에만 연결한다 — Redis 미가동이어도 컨텍스트 기동은 성공한다.
- 소비자가 자체 `RedisCacheConfiguration` 또는 `CacheManager` 빈을 정의하면 그 값이 우선한다(`@ConditionalOnMissingBean`). 토글: `mutuus.common.cache.*`(`enabled`, `ttl`, `key-prefix`, `cache-null-values`). 회귀 가드: `CommonCacheConfigurationTest`(Redis 없이 컨벤션 조립 검증, 단위) + `RedisCacheIntegrationTest`(Testcontainers, Docker 없으면 skip) + `RedisCacheLiveIntegrationTest`(로컬 실 Redis, 접속 가능할 때만) + 데모 `samples/sample-api/.../CacheDemoService`(`/demo/cache`).

### 12. 도메인 이벤트 (event 패키지, broker-agnostic 코어)

프로세스 내부/외부로 도메인 이벤트를 발행하는 **브로커 비의존 컨벤션**이다. 통합 스타터 없이 항상 사용 가능하며(`core`(TraceContext)에만 의존), `CommonEventAutoConfiguration`(`@ConditionalOnProperty(mutuus.common.event.enabled, matchIfMissing=true)`, 즉 **기본 ON**)이 in-process 기본 발행자를 등록한다.

- **봉투 `DomainEvent<T>`**(순수 record): `eventId`·`type`·`occurredAt`·`traceId`·`userId`·`payload`. `DomainEvent.of(type, payload)` 팩토리가 **발행 시점의 `TraceContext`(traceId/userId)를 봉투에 자동 주입**하고 eventId/occurredAt 을 발급한다 — 이벤트가 스레드·프로세스 경계를 넘어도 어느 요청에서 비롯됐는지 잃지 않는다(로그 상관·감사 일관성).
- **발행자 `EventPublisher`**: `publish(DomainEvent<?>)` + 편의 `publish(type, payload)`(봉투 자동 생성). 기본 구현 `ApplicationEventPublisherAdapter`는 Spring `ApplicationEventPublisher`로 위임해 같은 컨텍스트의 `@EventListener`/`@TransactionalEventListener`에 전달(브로커 불필요).
- **브로커 확장점**: 외부 브로커(Kafka/Rabbit 등)로 내보내려면 **소비 서비스가 `EventPublisher`를 구현한 빈으로 대체**한다(`@ConditionalOnMissingBean`) — 봉투 규약은 그대로 유지. 라이브러리는 코어(봉투+in-process 발행)만 제공하고, **브로커별 어댑터는 인프라 선택이 확정되면 얹는다**(현재 보류).
- 토글: `mutuus.common.event.enabled=false`. 회귀 가드: `DomainEventTest`(봉투 TraceContext 자동주입, 단위) + `EventPublishingIntegrationTest`(발행→in-process 리스너 수신) + 데모 `samples/sample-api/.../DemoEventRecorder`(`/demo/event`).

### 13. 프로퍼티 기반 다중 DataSource + 읽기/쓰기 라우팅 (datasource 패키지, optional·opt-in)

여러 논리 DB를 **YAML만으로** 정의하고 각 DB의 읽기/쓰기를 라우팅하는 **"메커니즘"만** 제공하는 optional 통합이다(정책=어느 DB·토폴로지·풀 사이징·JPA 매핑·트랜잭션 경계·XA는 서비스 몫). DB 계층 가이드([260702.002.DB_LAYER_GUIDE.md](docs/260702.002.DB_LAYER_GUIDE.md)) 3~4장의 ① 구조를 승격한 것 — ②(DS별 TxManager)·③(JTA/XA)는 도메인·정책 종속이라 수용하지 않는다(서비스 레벨).

`CommonDataSourceRoutingAutoConfiguration`(`@ConditionalOnClass(AbstractRoutingDataSource)` + `@ConditionalOnProperty(mutuus.common.datasource.enabled=true)`, **기본 OFF**, `beforeName` Boot `DataSourceAutoConfiguration`)이 `@Import(MultiDataSourceRegistrar)` 로 프로퍼티(`mutuus.common.datasource.groups.*`)를 읽어 **그룹마다 라우팅 DataSource(`<group>DataSource`)를 등록**한다. 각 그룹은 논리 DB 하나이며 `write`/`read` 커넥션을 갖고(HikariCP), 라이브러리가 `ReadWriteRoutingDataSource` + **필수 `LazyConnectionDataSourceProxy` 래핑**으로 조립한다. `primary` 그룹의 DataSource 가 `@Primary`(JPA/기본 주입 대상, Boot 단일 DataSource 자동구성은 물러남), 나머지는 `@Qualifier("<group>DataSource")` 로 주입. `read` 생략 시 write 폴백.

```yaml
mutuus.common.datasource:
  enabled: true
  primary: app                    # 이 그룹이 @Primary(JPA 기본)
  groups:
    app:  { write: { url: ..., username: ..., password: ..., driver-class-name: ..., pool-max-size: 10 } }  # read 생략 → write 폴백
    db1:  { write: { url: ... }, read: { url: ... } }   # 각 DB read/write 분리
    db2:  { write: { url: ... }, read: { url: ... } }
```

- **라우팅 판별(AOP 불필요)**: 커넥션 획득 시점에 `determineCurrentLookupKey()` 가 (1) `RoutingContext`(ThreadLocal) 명시 지정 우선, (2) 없으면 `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` — `@Transactional(readOnly=true)`→READ, 그 외 WRITE. **`LazyConnectionDataSourceProxy` 필수**(트랜잭션 readOnly 설정 *이후*로 커넥션 획득 지연 → 라우팅 정확; 없으면 무력화 = 가이드 6장 대표 버그).
- **정리 규율**: `RoutingContext.set()` 명시 지정 시 `finally`에서 `clear()`(ThreadLocal 누수 방지 — `TraceContext` 와 동일).
- 회귀 가드: `CommonDataSourceRoutingAutoConfigurationTest`(ApplicationContextRunner — 그룹별 DataSource 등록·primary 선택·readOnly→READ/쓰기→WRITE·OFF 시 미등록). **실증 데모**: 하나의 API 가 3개 DB(db1/db2/db3) 각 read/write 로 접속하는 `/demo/db/lab/multidb/*`(`DbMultiSupport`) + DB 3구조 개요 `/demo/db/{routing,multitx,xa,guide}`(`DbDemoSupport`) + 랩 페이지 `/demo/db.html`. ②(개별 TxManager)·③(Atomikos XA)는 서비스 레벨 프로그램적 시연(앱 기본 DataSource 와 분리).

### 13. 암호화 시크릿 부트스트랩 (secret 패키지, opt-in)

Offline Secret Manager v2의 `secret:v2:inline:` 암호문을 소비 서비스가 시작할 때 복호화하는 기능이다. 별도 Maven 모듈이나 외부 secret-manager SDK를 추가하지 않고 기존 단일 `common-platform` JAR의 `ai.mutuus.common.secret` 패키지에 둔다.

- `EncryptedSecretEnvironmentPostProcessor`는 Boot Config Data 로딩 뒤, DataSource/Flyway/Hikari 자동구성 전에 실행되며 `META-INF/spring.factories`에 등록돼 있다.
- 기본 OFF다. `mutuus.common.secret.enabled=true`일 때만 동작하며 초기 허용 대상은 `spring.datasource.password` ↔ `database.password` 한 개다.
- v2·unpadded Base64URL·16 KiB 제한·필드 집합·RSA-OAEP(SHA-256/MGF1-SHA256)·AES-256-GCM·AAD·program/environment/configKey/keyId를 모두 검증한다. v1, unknown/duplicate 필드, 변조, 빈 평문은 시작 실패다.
- 개인키 PKCS#12와 `keystore-password-file`은 파일시스템에서 서로 독립 공급한다. classpath/http locator와 평문+암호문 다중 source는 fail-closed하며 다른 DB나 평문으로 fallback하지 않는다.
- 성공한 모든 대상만 `mutuusDecryptedSecrets` 최우선 PropertySource에 원자 등록한다. 로그에는 target/keyId/formatVersion/결과만 남기고 암호문·평문·키 비밀번호는 남기지 않는다.
- sample 실증 프로파일은 `samples/sample-api/src/main/resources/application-secret-local.yml`; 실제 Golmok DB live 테스트는 `EncryptedSecretGolmokPostgresLiveIntegrationTest`이며 명시적 `-Dlive.golmok.secret.enabled=true`에서만 실행한다. Flyway/Hibernate DDL은 꺼서 `SELECT 1`만 수행한다.
- 정본과 운영 경계는 [docs/260707.002.SECRET_MANAGEMENT_STANDARD.md](docs/260707.002.SECRET_MANAGEMENT_STANDARD.md), 구현/검증 기록은 [docs/260827.002.ENCRYPTED_SECRET_LOADING_IMPLEMENTATION.md](docs/260827.002.ENCRYPTED_SECRET_LOADING_IMPLEMENTATION.md)다.

## 작업 시 규칙

- **코드 품질 기준(최우선 원칙)**: 이 프로젝트(`common-platform` 라이브러리 + `samples/*`)는 **상용(운영) 품질 AI 코딩의 모범사례이자 원천 소스(source of truth)** 다. 여기의 모든 코드·설정·문서는 AI 코딩 어시스턴트가 참조·재사용·변경관리하는 기준점이므로, **항상 상용 수준으로 작성**한다 — 임시방편·데모용 축약·"일단 동작" 코드를 남기지 않는다. 구체적으로:
  - 계층 분리(얇은 Controller → 트랜잭션 Service → Repository/DAO), 도메인 DTO(record) 노출·MapStruct 매핑·Bean Validation, 표준 응답(`ApiResponse`)·페이징(`PageResponse`)·에러(`ErrorCode`+`BusinessException`), optional+조건부 자동구성 패턴을 지킨다.
  - 새 기능은 참조 샘플([docs/260702.004.BOARD_SAMPLE_GUIDE.md](docs/260702.004.BOARD_SAMPLE_GUIDE.md) 등)의 구조·관습을 따르고, 회귀 가드(테스트)와 한국어 문서(설명·근거·AI 변경관리 지침)를 함께 남긴다.
  - 데모/샘플이라도 눈속임(mock 값 반환 등)이 아니라 **실제로 동작하는 상용급 구현**을 원칙으로 한다(불가피한 시연용 축약은 주석/문서로 명시). 운영 관점 권고(예: 스키마는 Flyway + `ddl-auto=validate`)가 데모 편의 구현과 다르면 그 차이를 문서에 남긴다.
- **테스트 위치(중요)**: 모든 테스트는 **소비 서비스 샘플(`samples/*`)에서 수행**한다. `lib/` 라이브러리 모듈에는 `lib/src/test`를 두지 않는다(현재 0개). 이 라이브러리는 "소비 서비스에 흡수되는" 산출물이므로, 단위 테스트까지 포함해 **실제 소비자가 의존성 하나로 끌어다 쓰는 관점**에서 검증한다.
  - **`samples/sample-api`**(웹 소비자): 대부분의 테스트. 라이브러리 내부 클래스의 단위/슬라이스 테스트는 라이브러리와 **같은 패키지명**(`ai.mutuus.common.*`)으로 둔다(package-private 멤버 접근은 classpath split-package로 동작 — 이 프로젝트는 JPMS 모듈이 아니다). 소비자 시나리오 통합 테스트는 `ai.mutuus.sample.*`에 둔다.
  - **`samples/sample-batch`**(비웹 소비자): web/security starter 미의존. optional 자동구성이 **비웹 서비스로 전이/활성화되지 않음**을 실제 컨텍스트로 검증한다. 비웹에선 spring-web/security 클래스가 classpath에 없어 타입 참조조차 불가하므로 web 빈 부재는 **빈 이름**으로 확인한다.
  - Docker가 필요한 실연동 테스트(Testcontainers)는 `@Testcontainers(disabledWithoutDocker = true)`로 가드해 무도커 환경에서 자동 skip되게 한다.
- **국제화**: 사용자에게 노출되는 메시지는 하드코딩하지 말고 `messages/messages*.properties`(기본/`_ko`/`_en`) + `MessageResolver`를 거친다. 키는 `ErrorCode.messageKey()` 컨벤션(`error.*`)을 따른다.
- **패키지 경계**: 패키지별 책임이 명확히 갈린다(`core` 무의존 유틸 → `config`/`i18n`/`exception` → `web`/`security`/`observability`/`async`/`persistence`). 하위 패키지(예: `core`)가 상위 통합 패키지(`web` 등)에 의존하지 않도록 한다. `async`/`persistence`는 `core`(TraceContext)에만 의존하는 optional 통합 패키지다.
- 주석·문서는 한국어로 작성돼 있다. 일관성을 위해 새 주석/문서도 한국어를 따른다.
- **데이터 접근 CRUD 모범사례(중요)**: 새 CRUD/도메인 기능을 만들 때는 게시판 3-스택 샘플([docs/260702.004.BOARD_SAMPLE_GUIDE.md](docs/260702.004.BOARD_SAMPLE_GUIDE.md), `samples/sample-api/.../board`)의 구조·관습을 참조한다 — 계층형(Controller 얇게→Service 트랜잭션→Repository/DAO), Record DTO + MapStruct + Bean Validation, 응답 `ApiResponse`·목록 `PageResponse`·에러 `ErrorCode`+`BusinessException`. Spring Data JPA / jOOQ / Spring Data JDBC 세 방식의 관용구와 선택 기준, AI 변경관리 지침을 담고 있다. 스키마는 **Flyway + `ddl-auto=validate`**(엔티티 자동 DDL 금지), 낙관적 락(`@Version`), **jOOQ 코드젠**(빌드타임 생성 테이블) 및 **동적 쿼리**(동적 SELECT/WHERE — LIKE·IN·from~to, jOOQ 특화)도 이 샘플에 포함된다(가이드 §4.5). 개선 이력은 [docs/260702.005.BOARD_SAMPLE_IMPROVEMENTS.md](docs/260702.005.BOARD_SAMPLE_IMPROVEMENTS.md).
- **소비 서비스 온보딩·설계 지침**: 라이브러리를 새 서비스에 적용하는 5분 온보딩(스타터+BOM·즉시기능·opt-in 카탈로그·함정)은 [docs/260702.009.ONBOARDING.md](docs/260702.009.ONBOARDING.md). 모듈러 모놀리스(Modulith) 구성·이벤트 아웃박스·async 전파는 [docs/260702.010.MODULITH_INTEGRATION.md](docs/260702.010.MODULITH_INTEGRATION.md). 도메인(바우처·서비스교환) MSA 설계는 DDD **전략** [docs/260702.007.PLATFORM_DDD_STRATEGY.md](docs/260702.007.PLATFORM_DDD_STRATEGY.md)·**전술** [docs/260702.008.VOUCHER_LEDGER_TACTICAL.md](docs/260702.008.VOUCHER_LEDGER_TACTICAL.md).
- **문서 파일 명명 규칙(중요)**: `docs/` 에 새 문서 파일을 만들 때 파일명은 **`yymmdd.NNN.<문서핵심제목>.확장자`** 구조를 따른다.
  - `yymmdd` = 생성일(예: 2026-07-02 → `260702`), `NNN` = **같은 날 생성 순번 3자리**(그날 첫 문서 `001`부터 증가), `<문서핵심제목>` = 문서 핵심 제목(예: `IDEMPOTENCY`), 마지막에 확장자(`.md` 등).
  - 예: `docs/260702.003.IDEMPOTENCY.md`. 구분자는 모두 `.`(점)이다.
  - 문서를 rename/추가하면 그 문서를 링크하는 **모든 참조(README·Codex·문서 간 링크)를 함께 갱신**한다(깨진 링크 금지). 기존 `docs/*` 문서는 각자 최초 생성일 기준으로 이 규칙이 소급 적용돼 있다.
- **기본 응답 언어는 한국어다.** 사용자에게 보고·설명·요약하는 모든 응답은 별도 요청이 없는 한 한국어로 작성한다.
