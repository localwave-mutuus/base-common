# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

`common-platform`은 **단일 산출물(jar) 공통 라이브러리**다. 타 API 서비스가 Maven 의존성 **하나**(`ai.mutuus.common:common-platform`)로 추적/로깅/예외/i18n/인증/세션 기능을 재사용한다. 실행 가능한 애플리케이션이 아니라 **소비 서비스에 흡수되는 라이브러리**라는 점이 모든 설계 결정의 근간이다.

- Java 21 / Spring Boot 4.1.0 (Spring Framework 7) / Spring Modulith 2.1.0
- 기능은 `ai.mutuus.common` 아래 **패키지로만** 구분된다(별도 Maven 모듈 아님). README 등에서 `common-web`, `common-core`로 부르는 것은 모듈이 아니라 패키지를 가리킨다.

## 빌드 / 테스트 명령

Maven Wrapper는 **아직 없다**(README의 TODO). 로컬에 Maven 3.9+, JDK 21 필요.

```bash
mvn clean install        # 컴파일 + 테스트 + 로컬 .m2 설치
mvn test                 # 전체 테스트
mvn -Dtest=ClassName test                     # 단일 테스트 클래스
mvn -Dtest=ClassName#methodName test          # 단일 테스트 메서드
mvn clean deploy         # 사내 Nexus/Artifactory에 라이브러리 jar 게시
```

`spring-boot-maven-plugin`은 `<skip>true</skip>`로 설정돼 있다 — 라이브러리이므로 **실행 가능 jar로 repackage하지 않는다**. 이 설정을 건드리지 말 것.

## 핵심 아키텍처

### 1. optional 의존성 + 조건부 자동구성 (가장 중요한 원칙)

라이브러리 jar에는 **모든 기능 코드가 포함**되지만, 통합 의존성(web/security/actuator/otel/redis 등)은 `pom.xml`에서 전부 `<optional>true</optional>`이다. 따라서 소비 서비스로 **강제 전이되지 않는다**.

각 자동구성은 `@ConditionalOnClass`로 보호되어, **소비 서비스의 classpath에 해당 starter가 있을 때만** 활성화된다. 예:
- `CommonWebAutoConfiguration` → `@ConditionalOnClass(DispatcherServlet.class)`
- `CommonSecurityAutoConfiguration` → `@ConditionalOnClass(SecurityFilterChain.class)`

**새 통합 기능을 추가할 때는 반드시 이 패턴을 따른다**: 의존성은 optional로, 자동구성은 `@ConditionalOnClass`(필요시 `@ConditionalOnProperty`/`@ConditionalOnMissingBean`)로 보호. 그래야 비(非)웹 배치 서비스가 이 라이브러리를 써도 web/security가 끌려 들어가지 않는다.

### 2. 자동구성 등록 (Spring Boot 3+ 방식)

자동구성 클래스는 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`에 한 줄씩 등록한다. **새 `@AutoConfiguration` 클래스를 만들면 이 파일에도 추가해야** 활성화된다.

별도로 `META-INF/spring.factories`에는 `EnvironmentPostProcessor`만 등록돼 있다(컨텍스트 초기화 *이전*에 동작해야 해서 이 메커니즘이 별도로 필요).

### 3. 설정 기본값 주입 흐름

`CommonEnvironmentPostProcessor`가 Boot 컨텍스트 초기화 **전에** 기본 프로퍼티(`management.tracing.*`, `spring.messages.*`, `mutuus.common.*` 등)를 Environment에 `addLast`(= 최저 우선순위)로 주입한다. 따라서 소비 서비스의 `application.yml`이 같은 키를 정의하면 **항상 애플리케이션 값이 우선**한다. 새 "convention" 기본값은 여기서 추가한다.

### 4. e2e 추적 정보 계층 (web 패키지의 핵심)

`X-Trace-Id`, `X-Span-Id`, `X-Screen-Id`, `X-Event-Id`, `X-Device-Level`, `X-Device-Id`, `X-User-Id`, `X-Locale` 8개 커스텀 헤더로 화면→이벤트→API 체인을 추적한다. 헤더 상수와 전파 목록은 `core/HeaderNames.java` 한 곳에 정의 — **헤더를 추가/변경하면 여기부터 수정**한다.

흐름:
1. **인입**: `TraceFilter`(`@Order(HIGHEST_PRECEDENCE)`, `/*`)가 헤더를 추출해 `TraceContext`(ThreadLocal) + SLF4J `MDC`에 적재. `X-Trace-Id`가 없으면 신규 UUID 생성. 응답에 `X-Trace-Id` 회신. **요청 종료 시 `finally`에서 반드시 `MDC.clear()` + `TraceContext.clear()`** (ThreadLocal 누수 방지 — 이 정리를 빠뜨리지 말 것).
2. **아웃바운드**: `HeaderPropagationInterceptor`(`ClientHttpRequestInterceptor`)가 `TraceContext`의 헤더를 하위 호출에 자동 부착. 단 `X-Span-Id`는 전파하지 않고 호출 구간마다 **새로 발급**한다.

`TraceContext`는 ThreadLocal 기반이므로 **가상 스레드/`@Async` 경계를 넘으면 전파되지 않는다**(컨텍스트 전파 유틸은 TODO 상태).

### 5. 표준 예외 응답

`GlobalExceptionHandler`(`@RestControllerAdvice`)가 모든 예외를 **RFC7807 `ProblemDetail`**로 변환한다. `BusinessException`은 `ErrorCode`(enum: status + i18n messageKey)를 들고 다니며, 응답 메시지는 `MessageResolver`(i18n)로 로케일별 변환, `traceId`/`screenId`/`timestamp`를 부가 프로퍼티로 첨부한다. **새 도메인 에러는 `ErrorCode` enum에 항목 추가 + `messages*.properties`에 해당 키 추가**가 한 세트다.

### 6. 보안 (인증 위임 모델)

이 라이브러리는 인증을 **수행하지 않는다**. 별도 인증/인가 MSA가 발급한 JWT를 검증하는 OAuth2 Resource Server 기본 설정만 제공한다(`CommonSecurityAutoConfiguration`). `SecurityFilterChain`/`JwtAuthenticationConverter`는 `@ConditionalOnMissingBean`이라, **소비 서비스가 자체 빈을 정의하면 즉시 대체**된다. permit-all 경로·roles claim·authority prefix는 `mutuus.common.security.*`로 설정.

## 작업 시 규칙

- **국제화**: 사용자에게 노출되는 메시지는 하드코딩하지 말고 `messages/messages*.properties`(기본/`_ko`/`_en`) + `MessageResolver`를 거친다. 키는 `ErrorCode.messageKey()` 컨벤션(`error.*`)을 따른다.
- **패키지 경계**: 패키지별 책임이 명확히 갈린다(`core` 무의존 유틸 → `config`/`i18n`/`exception` → `web`/`security`/`observability`). 하위 패키지(예: `core`)가 상위 통합 패키지(`web` 등)에 의존하지 않도록 한다.
- 주석·문서는 한국어로 작성돼 있다. 일관성을 위해 새 주석/문서도 한국어를 따른다.
