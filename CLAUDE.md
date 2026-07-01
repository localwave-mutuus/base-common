# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

`common-platform`의 **핵심 산출물은 단일 jar 공통 라이브러리**(`ai.mutuus.common:common-platform`)다. 타 API 서비스가 Maven 의존성으로 추적/로깅/예외/i18n/인증/세션 기능을 재사용한다. 실행 가능한 애플리케이션이 아니라 **소비 서비스에 흡수되는 라이브러리**라는 점이 모든 설계 결정의 근간이다. 소비 편의를 위해 **BOM(`common-platform-bom`)과 큐레이션 스타터(`common-platform-starter-web`/`-batch`)를 함께 발행**한다 — 소비자는 스타터 1개로 표준 스택을, BOM 으로 버전을 관리한다.

- **레포 레이아웃은 Maven reactor**다(루트 `pom.xml`은 `packaging=pom` aggregator, 게시 대상 아님). 모듈 셋:
  - `lib/` — **산출물 라이브러리**(`ai.mutuus.common:common-platform`). 라이브러리 소스/리소스는 전부 `lib/src/...`에 있다.
  - `parent/` — **`common-platform-parent`**(`packaging=pom`). 게시 모듈(bom·lib·starters)의 공용 부모 — **게시 좌표(Nexus distributionManagement)·배포 정책(`deployAtEnd`)·공통 버전(java/logstash/springdoc)을 한 곳에서** 관리(자식에 복붙되던 중복 제거). 소비자가 lib/starter pom 을 해석할 때 필요하므로 parent 도 게시 대상.
  - `bom/` — **`common-platform-bom`**(`packaging=pom`). 자사 아티팩트(lib·스타터들) + 라이브러리가 노출하는 optional 3rd-party(logstash·springdoc) 버전을 한 곳에서 고정 → 소비자는 버전 없이 추가, CVE/상향은 BOM 한 곳만 올리면 일괄 롤아웃.
  - `starters/starter-web`, `starters/starter-batch` — **큐레이션 스타터**(코드 없는 의존성 취합 jar, `spring-boot-maven-plugin skip`). web = lib + 표준 웹 스택(web/security/oauth2/actuator/validation/restclient/aspectj) + JSON 로깅(logstash); batch = lib + JSON 로깅(web/security 제외 → 비전이 유지). 소비자 온보딩 1줄.
  - `samples/sample-api`, `samples/sample-batch` — 라이브러리를 끌어쓰는 **소비 검증용 샘플**(아래 "테스트 위치" 규칙). 현재 샘플은 **스타터+BOM 경유**로 소비한다(sample-api→`starter-web`, sample-batch→`starter-batch`, 둘 다 `common-platform-bom` import).
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

### 7. API 생명주기 자동 로깅 (logging 패키지)

소비 서비스는 **별도 코드 없이** 라이브러리가 지정한 컴포넌트를 거치며 4개 지점에서 자동 로깅된다. 중심은 `AccessLogger`(단일 진입점, 로거 이름 `ai.mutuus.common.access`) — SLF4J 2 fluent API(`addKeyValue`)로 구조화 필드를 남기고 logstash 인코더가 JSON으로 렌더한다(추적ID/사용자는 MDC 경유 자동 포함).

`lib/src/main/resources/logback-common.xml`은 **JSON 콘솔 + 롤링 파일** appender를 제공한다(파일명 `<app-code>-<instance-code>.log`, 크기 100MB+일자 롤링, `${LOG_DIR:-logs}`). 소비자가 `logback-spring.xml`에서 `<include resource="logback-common.xml"/>`하면 활성화되며, **logstash 인코더는 optional이라 소비자가 `net.logstash.logback:logstash-logback-encoder`를 직접 추가**해야 한다(미추가 시 `ClassNotFoundException`으로 컨텍스트 기동 실패). 5개 시나리오는 `AccessLogFilter`(정상 유입/응답) + `GlobalExceptionHandler`(잘못된 포맷→`MALFORMED_REQUEST` 400, 비즈니스→`error.business`, 런타임→`error.server` 500, 네트워크/아웃바운드 실패 `ResourceAccessException`→502/504)에서 모두 추적키와 함께 남는다.

- **요청 수신/응답**: `AccessLogFilter`(order `HIGHEST_PRECEDENCE + 10`, 즉 `TraceFilter` 바로 뒤). 보안 체인을 **감싸므로** 응답 로그가 401/403을 포함한 최종 상태코드를 본다. → `request.received` / `request.completed`(5xx·느린 요청은 WARN)
- **인증 체크(실패)**: `CommonSecurityAutoConfiguration`이 `LoggingAuthenticationEntryPoint`(401)/`LoggingAccessDeniedHandler`(403)를 자원 서버 DSL에 주입. 둘 다 **기본 Bearer 핸들러에 위임**하여 `WWW-Authenticate` 등 표준 응답을 보존한다. → `auth.failure` / `auth.denied`
- **오류**: `GlobalExceptionHandler`가 `error.business`(WARN)/`error.server`(ERROR, 스택 포함) 기록.

`AccessLogger` 빈은 항상 제공(보안/예외 모듈이 `ObjectProvider`로 주입, 없으면 무동작 폴백)하고, 요청/응답 필터는 웹 환경에서만 등록한다. 토글·튜닝은 `mutuus.common.logging.*`(`enabled`, `include-query-string`, `exclude-path-prefixes`(기본 `/actuator`), `slow-request-threshold-millis`). 본문(body) 로깅은 PII·비용 문제로 기본 미포함. 회귀 가드: `samples/sample-api/AccessLoggingIntegrationTest`.

**추가 3계층(모두 기본 OFF, opt-in)** — 액세스 로깅이 못 보는 "컨트롤러 안쪽"을 점점 더 깊이 관측한다. 계층별 위치·이벤트·의존성:
- **입출력 본문(`payload` 패키지, 로거 `ai.mutuus.common.payload`)**: `PayloadLoggingFilter`(order `HIGHEST_PRECEDENCE+20`, AccessLogFilter 바로 안쪽)가 요청/응답 본문을 캐싱해 `request.payload`/`response.payload` 기록. 진입/종료 출력은 각각 `RequestPayloadLogger`/`ResponsePayloadLogger` 빈(교체 가능). 토글 `mutuus.common.payload-logging.*`. 문서 [PAYLOAD_LOGGING.md](docs/PAYLOAD_LOGGING.md).
- **컨트롤러 진입(`intercept` 패키지, 로거 `ai.mutuus.common.controller`)**: `ControllerEntryInterceptor`(MVC `preHandle`, 인자 역직렬화 **전**)가 명칭/패키지/URL 로 타게팅한 메서드 진입 시 `controller.entry` 기록. 판별(`ControllerMethodMatcher`)·동작(`ControllerEntryHandler`) 분리. 토글 `mutuus.common.controller-entry.*`. 문서 [CONTROLLER_ENTRY.md](docs/CONTROLLER_ENTRY.md).
- **메서드 인자/리턴(`aop` 패키지, 로거 `ai.mutuus.common.method`)**: `ControllerMethodLoggingAspect`(`@Around @within(@RestController)`, 인자 역직렬화 **후**)가 `method.enter`(인자 객체)/`method.exit`(리턴 객체) 기록. 출력은 `MethodLoggingWriter` 빈(교체 가능). **AOP라 소비자가 `spring-boot-starter-aspectj`(Boot 4에서 `-aop`→`-aspectj` 개명)를 추가해야만 활성**(`@ConditionalOnClass(ProceedingJoinPoint)`). 토글 `mutuus.common.method-logging.*`. 문서 [METHOD_LOGGING.md](docs/METHOD_LOGGING.md).

> 세 "진입" 관측의 깊이 차이가 핵심: 필터=바이트 본문 / 인터셉터=메서드 식별(인자 결정 전) / AOP=역직렬화된 값(인자 결정 후). 그래서 @Valid·JSON 파싱 실패는 `controller.entry`까지만 남고 `method.enter`는 안 남는다. **요청 유입 시 전체 로깅 구간의 케이스별 실측 순서**는 [LOGGING_CASES.md](docs/LOGGING_CASES.md)(회귀 가드 `PayloadAndEntryLoggingIntegrationTest` + `LoggingCaseMatrixIntegrationTest`).

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

## 작업 시 규칙

- **테스트 위치(중요)**: 모든 테스트는 **소비 서비스 샘플(`samples/*`)에서 수행**한다. `lib/` 라이브러리 모듈에는 `lib/src/test`를 두지 않는다(현재 0개). 이 라이브러리는 "소비 서비스에 흡수되는" 산출물이므로, 단위 테스트까지 포함해 **실제 소비자가 의존성 하나로 끌어다 쓰는 관점**에서 검증한다.
  - **`samples/sample-api`**(웹 소비자): 대부분의 테스트. 라이브러리 내부 클래스의 단위/슬라이스 테스트는 라이브러리와 **같은 패키지명**(`ai.mutuus.common.*`)으로 둔다(package-private 멤버 접근은 classpath split-package로 동작 — 이 프로젝트는 JPMS 모듈이 아니다). 소비자 시나리오 통합 테스트는 `ai.mutuus.sample.*`에 둔다.
  - **`samples/sample-batch`**(비웹 소비자): web/security starter 미의존. optional 자동구성이 **비웹 서비스로 전이/활성화되지 않음**을 실제 컨텍스트로 검증한다. 비웹에선 spring-web/security 클래스가 classpath에 없어 타입 참조조차 불가하므로 web 빈 부재는 **빈 이름**으로 확인한다.
  - Docker가 필요한 실연동 테스트(Testcontainers)는 `@Testcontainers(disabledWithoutDocker = true)`로 가드해 무도커 환경에서 자동 skip되게 한다.
- **국제화**: 사용자에게 노출되는 메시지는 하드코딩하지 말고 `messages/messages*.properties`(기본/`_ko`/`_en`) + `MessageResolver`를 거친다. 키는 `ErrorCode.messageKey()` 컨벤션(`error.*`)을 따른다.
- **패키지 경계**: 패키지별 책임이 명확히 갈린다(`core` 무의존 유틸 → `config`/`i18n`/`exception` → `web`/`security`/`observability`/`async`/`persistence`). 하위 패키지(예: `core`)가 상위 통합 패키지(`web` 등)에 의존하지 않도록 한다. `async`/`persistence`는 `core`(TraceContext)에만 의존하는 optional 통합 패키지다.
- 주석·문서는 한국어로 작성돼 있다. 일관성을 위해 새 주석/문서도 한국어를 따른다.
- **기본 응답 언어는 한국어다.** 사용자에게 보고·설명·요약하는 모든 응답은 별도 요청이 없는 한 한국어로 작성한다.
