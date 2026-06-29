# common-platform

AI 백엔드 **공통 라이브러리(단일 jar)**. 타 API 서비스가 Maven 의존성 **하나**로 재사용한다.
기능은 내부 패키지로만 구분되며, 자동 구성(Auto-Configuration)으로 무설정 활성화된다.

```
ai.mutuus.common:common-platform:0.1.0-SNAPSHOT   ← 산출물 1개
```

## 기술스택

- Java 21 (LTS)
- Spring Boot 4.1.0 / Spring Framework 7
- Spring Modulith 2.1.0 (관측)
- Micrometer + OpenTelemetry, Logback(JSON), Spring Security OAuth2 Resource Server
- 빌드: Maven (단일 모듈, 라이브러리 jar)

## 내부 패키지 구성

| 패키지 | 역할 | 충족 요건 |
|--------|------|----------|
| `core` | 공통 상수(헤더), 추적 컨텍스트, ID 생성기, 기본 유틸 | 글로벌 UUID, 기본 유틸 |
| `config` | 부팅 전 설정 주입(`EnvironmentPostProcessor`), 프로퍼티 | 프로퍼티 로딩, Boot 로딩 전 설정 주입 |
| `api` | 표준 응답 봉투 `ApiResponse`/`ApiError` | 응답 표준화 → [API_RESPONSE.md](docs/API_RESPONSE.md) |
| `exception` | 표준 `ApiResponse` 봉투 전역 예외 처리(+ i18n·추적ID) | 응답/오류 표준화 |
| `i18n` | MessageSource 기반 다국어 | 다국어 |
| `logging` | API 생명주기 구조화 JSON 로깅(`AccessLogger`) | logging → [LOG_FORMAT.md](docs/LOG_FORMAT.md) |
| `observability` | Micrometer/OTel 추적, 공통 `service.name` 태그, Modulith 관측 | telemetry |
| `web` | e2e 추적 필터, MDC 적재, API 간 헤더 자동 전파, 로케일 | e2e 정보계층, 헤더 자동추가 |
| `security` | OAuth2 Resource Server(JWT), 인증/인가 MSA 연계, 인증 주체 추적 | 인증/인가 연계 |
| `async` | `@Async`/가상스레드 추적 컨텍스트 전파 | 비동기 추적 연속성 |
| `persistence` | JPA 공통 베이스 엔티티 + 감사(생성/수정 시각·주체) | 감사 컬럼 자동화 |
| `session` | 분산 세션(Redis) 컨벤션(네임스페이스·타임아웃) | 세션 |

## 설계 원칙: optional 의존성 + 조건부 자동구성

라이브러리 jar 에는 **모든 기능 코드가 포함**되지만, 통합 의존성(web/security/actuator/otel/redis 등)은
`optional` 이라 소비 서비스로 강제 전이되지 않는다. 각 자동구성은 `@ConditionalOnClass` 로 보호되어
**소비 서비스의 classpath 에 해당 starter 가 있을 때만** 활성화된다.

- 웹 API 서비스(대부분) → `spring-boot-starter-web`/`security` 를 이미 보유 → 추적·헤더전파·예외·인증 자동 활성
- 비(非)웹 배치 서비스 → web/security 자동구성은 비활성, `core`/`config`/`i18n` 유틸만 사용 (검증: `samples/sample-batch`)

## 소비 측(각 API) 사용법

### 1) 의존성 추가 (단 하나)

```xml
<dependency>
  <groupId>ai.mutuus.common</groupId>
  <artifactId>common-platform</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

> 소비 서비스가 web/security 기능을 쓰려면 해당 Spring Boot starter 가 classpath 에 있어야 한다
> (일반 API 서비스는 이미 `spring-boot-starter-web`, `-security` 를 사용하므로 별도 작업 불필요).

### 2) 설정 (application.yml)

```yaml
mutuus:
  common:
    service-name: order-api
    default-locale: ko-KR
    security:
      permit-all:
        - /actuator/health/**
        - /api/public/**

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.mutuus.ai/realms/mutuus   # 별도 인증 MSA

management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

### 3) 로깅 (logback-spring.xml)

```xml
<configuration>
  <include resource="logback-common.xml"/>
</configuration>
```

## 커스텀 헤더(e2e 정보 계층)

| 헤더 | 의미 |
|------|------|
| `X-Trace-Id` | 트랜잭션 글로벌 추적 ID(UUID) |
| `X-Span-Id` | 호출 구간 ID |
| `X-Screen-Id` | 화면 ID (메뉴/화면 식별) |
| `X-Event-Id` | 화면 내 이벤트(버튼/액션) 식별 |
| `X-Device-Level` | 단말 수준 (WEB/ANDROID/IOS 등) |
| `X-Device-Id` | 단말 고유 식별자 |
| `X-User-Id` | 인증 사용자 ID |
| `X-Locale` | 사용자 로케일(다국어) |

`TraceFilter` 가 인입 시 추출→MDC/TraceContext 적재, `HeaderPropagationInterceptor` 가
아웃바운드 호출(RestClient)에 자동 부착 → API 간 추적 체인 연결.

## 테스트

테스트는 라이브러리 모듈이 아니라 **소비 서비스 `samples/sample-api` 에서 수행**한다(라이브러리는 소비자에 흡수되는 산출물이라, 단위 테스트까지 소비자 관점에서 검증). 라이브러리를 고쳤으면 먼저 재설치한다.

```bash
./mvnw clean install                                # 1) 라이브러리 설치(선행 필수)
cd samples/sample-api   && ../../mvnw clean test    # 2a) 웹 소비자 테스트(대부분)
cd samples/sample-batch && ../../mvnw clean test    # 2b) 비웹 소비자 테스트(web/security 비전이 검증)
```

## 배포(사내 저장소)

```bash
./mvnw -q clean deploy   # Nexus/Artifactory 에 라이브러리 jar 게시
```

> JDK 21 필요. Maven Wrapper(`./mvnw`)가 Maven 3.9.9 로 고정돼 있어 로컬 Maven 설치는 불필요하다.

## 향후 확장(TODO)

- ~~Maven Wrapper(`mvnw`) 추가~~ 완료 — Maven 3.9.9 고정
- ~~자동구성 슬라이스 테스트(`ApplicationContextRunner`)~~ 완료. PostgreSQL·Redis 실연동(Testcontainers, Docker 없으면 자동 skip)도 추가
- ~~분산 세션(Redis) 컨벤션~~ 완료 — `session` 패키지(`<service-name>:session` 네임스페이스·타임아웃 자동 적용)
- ~~`persistence` 패키지(JPA 공통 베이스 엔티티·감사컬럼)~~ 완료 — `BaseEntity`(생성/수정 시각·주체 자동), 주체는 `TraceContext` 인증 사용자에서 채움
- ~~가상스레드/비동기 컨텍스트 전파 유틸(`TraceContext` ↔ `@Async`)~~ 완료 — `async` 패키지(`TraceContextPropagation`/`TraceContextTaskDecorator`)
