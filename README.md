# common-platform

AI 백엔드 **공통 라이브러리(단일 jar)**. 타 API 서비스가 Maven 의존성으로 재사용한다.
기능은 내부 패키지로만 구분되며, 자동 구성(Auto-Configuration)으로 무설정 활성화된다.
소비 편의를 위해 **BOM + 큐레이션 스타터**(web/batch)를 함께 발행한다.

```
ai.mutuus.common:common-platform                              ← 산출물 라이브러리(핵심)
ai.mutuus.common:common-platform-bom                          ← 버전 관리 BOM
ai.mutuus.common:common-platform-starter-web / -starter-batch ← 큐레이션 스타터(온보딩 1줄)
```

## 기술스택

- Java 21 (LTS)
- Spring Boot 4.1.0 / Spring Framework 7
- Spring Modulith 2.1.0 (관측 + 샘플 모듈러 구조화; BOM 이 `spring-modulith-bom` 노출 → 소비자 버전 없이 사용)
- Micrometer + OpenTelemetry, Logback(JSON), Spring Security OAuth2 Resource Server
- 빌드: Maven **reactor** — 루트 aggregator(`pom.xml`, packaging=pom) 아래 `parent/`(게시모듈 공용부모) + `bom/`(common-platform-bom) + `lib/`(산출물 라이브러리 jar) + `starters/*`(큐레이션 스타터 web/batch) + `samples/*`(소비 검증). 게시 대상은 parent·BOM·라이브러리·스타터.

## 내부 패키지 구성

| 패키지 | 역할 | 충족 요건 |
|--------|------|----------|
| `core` | 공통 상수(헤더), 추적 컨텍스트, ID 생성기, 기본 유틸, 민감정보 마스킹(`SensitiveDataMasker`) | 글로벌 UUID, 기본 유틸, 로그 PII 마스킹 |
| `config` | 부팅 전 설정 주입(`EnvironmentPostProcessor`), 프로퍼티 | 프로퍼티 로딩, Boot 로딩 전 설정 주입 |
| `api` | 표준 응답 봉투 `ApiResponse`/`ApiError`, 페이징 응답 `PageResponse` | 응답 표준화 → [260629.002.API_RESPONSE.md](docs/260629.002.API_RESPONSE.md) |
| `exception` | 표준 `ApiResponse` 봉투 전역 예외 처리(+ i18n·추적ID), `ErrorCode` 인터페이스(소비자 확장) | 응답/오류 표준화 |
| `response` | 성공 응답 자동 래핑(`ResponseBodyAdvice`, opt-in) — 평범한 반환을 표준 봉투로 | 응답 표준화(무코드) |
| `i18n` | MessageSource 기반 다국어 | 다국어 |
| `logging` | API 생명주기 구조화 JSON 로깅(`AccessLogger`) | logging → [260629.001.LOG_FORMAT.md](docs/260629.001.LOG_FORMAT.md) |
| `payload` | 요청/응답 **본문** 로깅(opt-in) | 본문 관측 → [260701.002.PAYLOAD_LOGGING.md](docs/260701.002.PAYLOAD_LOGGING.md) |
| `intercept` | 컨트롤러 **진입** 인터셉트 로깅(opt-in, 인자 역직렬화 전) | 진입 관측 → [260701.001.CONTROLLER_ENTRY.md](docs/260701.001.CONTROLLER_ENTRY.md) |
| `aop` | 컨트롤러 메서드 **인자/리턴** 로깅(AOP, opt-in, 역직렬화 후) | 메서드 관측 → [260701.005.METHOD_LOGGING.md](docs/260701.005.METHOD_LOGGING.md) |
| `observability` | Micrometer/OTel 추적, 공통 `service.name` 태그, Modulith 관측 | telemetry |
| `openapi` | 공통 OpenAPI 설정(springdoc, opt-in) — info + Bearer JWT 보안스킴 | API 문서 표준화 |
| `web` | e2e 추적 필터, MDC 적재, API 간 헤더 자동 전파, 로케일, HTTP 클라 타임아웃/재시도 | e2e 정보계층, 헤더 자동추가 |
| `security` | OAuth2 Resource Server(JWT), 인증/인가 MSA 연계, 인증 주체 추적 + **보안 감사 로깅**(`security/audit`, 401/403·위배 이벤트) + **하드닝**(X-User-Id 신뢰경계·JWT audience·오류 정보 미노출·전파 allowlist·CSRF 조건부·보안 응답 헤더) | 인증/인가 연계·보안 하드닝 → [260702.006.SECURITY_HARDENING.md](docs/260702.006.SECURITY_HARDENING.md) |
| `async` | `@Async`/가상스레드 추적 컨텍스트 전파 | 비동기 추적 연속성 |
| `persistence` | JPA 공통 베이스 엔티티 + 감사(생성/수정 시각·주체) | 감사 컬럼 자동화 |
| `session` | 분산 세션(Redis) 컨벤션(네임스페이스·타임아웃) | 세션 |
| `idempotency` | 멱등성(`Idempotency-Key`) 필터(opt-in) — 중복 요청 첫 응답 재방 | 멱등 처리 |
| `cache` | 캐시 추상화(`@EnableCaching` + Redis `CacheManager` 컨벤션, opt-in) | 캐시 표준화 |
| `event` | 도메인 이벤트 봉투(`DomainEvent`) + 발행자 컨벤션(broker-agnostic) | 이벤트/메시징 |
| `datasource` | **프로퍼티 기반 다중 DataSource + 읽기/쓰기 라우팅**(opt-in) — YAML(`groups.*`)만으로 여러 DB, `@Transactional(readOnly)` 기반 write/read 분리 | DB 라우팅 → [260702.002.DB_LAYER_GUIDE.md](docs/260702.002.DB_LAYER_GUIDE.md) |

## 설계 원칙: optional 의존성 + 조건부 자동구성

라이브러리 jar 에는 **모든 기능 코드가 포함**되지만, 통합 의존성(web/security/actuator/otel/redis 등)은
`optional` 이라 소비 서비스로 강제 전이되지 않는다. 각 자동구성은 `@ConditionalOnClass` 로 보호되어
**소비 서비스의 classpath 에 해당 starter 가 있을 때만** 활성화된다.

- 웹 API 서비스(대부분) → `spring-boot-starter-web`/`security` 를 이미 보유 → 추적·헤더전파·예외·인증 자동 활성
- 비(非)웹 배치 서비스 → web/security 자동구성은 비활성, `core`/`config`/`i18n` 유틸만 사용 (검증: `samples/sample-batch`)

## 소비 측(각 API) 사용법

### 1) 의존성 추가 — 권장: BOM + 큐레이션 스타터

버전은 **BOM** 으로 관리하고, 표준 스택은 **스타터 하나**로 끌어온다.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>ai.mutuus.common</groupId>
      <artifactId>common-platform-bom</artifactId>
      <version>0.2.0</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- 웹 서비스: web/security/oauth2/actuator/validation/restclient/aspectj + JSON 로깅 포함 -->
<dependency>
  <groupId>ai.mutuus.common</groupId>
  <artifactId>common-platform-starter-web</artifactId>   <!-- 버전 생략(BOM 관리) -->
</dependency>

<!-- 비웹(배치) 서비스: core + JSON 로깅 (web/security 없음)
<dependency>
  <groupId>ai.mutuus.common</groupId>
  <artifactId>common-platform-starter-batch</artifactId>
</dependency> -->
```

> 스타터를 쓰면 `logstash-logback-encoder`(JSON 로깅)·`aspectj`(메서드 로깅) 등 **라이브러리가 필요로 하는 의존성이 자동 포함**된다 — 빠뜨려서 기동 실패할 일이 없다.

### (대안) 라이브러리만 직접 참조

스타터 없이 라이브러리만 쓰고 필요한 Spring Boot starter 는 소비자가 직접 조립할 수도 있다.

```xml
<dependency>
  <groupId>ai.mutuus.common</groupId>
  <artifactId>common-platform</artifactId>
  <version>0.2.0</version>
</dependency>
```

> 이 경우 web/security 기능을 쓰려면 `spring-boot-starter-web`/`-security` 등을 직접 넣어야 하고
> (일반 API 서비스는 이미 보유), JSON 로깅을 쓰면 `logstash-logback-encoder` 도 직접 추가한다.

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

`logback-common.xml` 은 **JSON 콘솔 + 파일** appender 를 제공한다.
- 파일명: `<어플리케이션코드>-<인스턴스구분코드>.log` (크기 100MB + 일자 롤링, `${LOG_DIR:-logs}` 디렉터리).
- 모든 로그에 추적키(`X-Trace-Id`)·`appCode`·`instanceCode`·`service` 필드가 포함된다.
- **JSON 인코더는 optional**. 스타터(`common-platform-starter-web`/`-batch`)를 쓰면 자동 포함되고, 라이브러리만 직접 참조하는 경우에만 추가한다:
  ```xml
  <dependency>
    <groupId>net.logstash.logback</groupId><artifactId>logstash-logback-encoder</artifactId>
    <version>8.0</version><scope>runtime</scope>
  </dependency>
  ```

어플리케이션코드(4자리)·인스턴스구분코드(6자리)는 `mutuus.common.app-code`/`instance-code` 로 지정하며,
미지정 시 각각 서비스명에서 도출/구동 시 자동 생성된다. 두 코드는 응답·아웃바운드 헤더(`X-App-Code`/`X-Instance-Id`)와 로그 파일명에 쓰인다.

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
| `X-App-Code` | 어플리케이션코드(숫자/영문 4자리) — 호출 주체 앱 식별 |
| `X-Instance-Id` | 인스턴스구분코드(숫자/영문 6자리) — 호출 주체 인스턴스 식별 |

`TraceFilter` 가 인입 시 추출→MDC/TraceContext 적재, `HeaderPropagationInterceptor` 가
아웃바운드 호출(RestClient)에 자동 부착 → API 간 추적 체인 연결.

## 테스트

테스트는 라이브러리 모듈이 아니라 **소비 서비스 `samples/*` 에서 수행**한다(라이브러리는 소비자에 흡수되는 산출물이라, 단위 테스트까지 소비자 관점에서 검증). 루트에서 reactor 로 돌리면 `lib` 를 먼저 빌드한 뒤 샘플이 그 모듈을 해석하므로 별도 재설치가 필요 없다.

샘플 프로파일 파일 자체도 소비자 설정의 일부로 검증한다. 예를 들어 `AiProfilePropertiesTest`는
`application-ai.yml`의 `mutuus.sample.mock-jwt=true`, `mutuus.common.security.audiences`,
`demo.environment=ai`가 실제 `DemoSecurityConfig`/`JwtDecoder` 동작으로 이어지는지 확인한다.
이 테스트는 MOCK 웹 컨텍스트라 `server.port=8095`를 읽어도 서버 포트를 열지 않는다. 샘플 기본 실행 포트는 `8090`, 수동/백그라운드 테스트 할당 범위는 `8095~8099`다.

```bash
./mvnw clean test                                   # reactor 전체: lib 빌드 후 두 샘플 테스트 일괄
cd samples/sample-api   && ../../mvnw clean test    # 웹 소비자만 (대부분)
cd samples/sample-batch && ../../mvnw clean test    # 비웹 소비자만 (web/security 비전이 검증)
```

> 샘플을 **단독**으로 돌릴 때는 라이브러리를 .m2 설치 jar 로 해석하므로, 라이브러리를 고쳤으면 먼저 `./mvnw -pl lib install` 로 재설치한다.

## 배포(사내 저장소)

```bash
# 테스트 후 배포(한 방): deployAtEnd 라 리액터 전체(샘플 테스트) 성공 후에만 게시 모듈
# (parent·bom·lib·starters) 일괄 업로드. samples·aggregator 는 deploy.skip.
# Nexus self-signed 라 MAVEN_OPTS 로 truststore 지정 필요(자세히: docs/260701.003.HANDOFF.md).
./mvnw -q clean deploy
```

> JDK 21 필요. Maven Wrapper(`./mvnw`)가 Maven 3.9.9 로 고정돼 있어 로컬 Maven 설치는 불필요하다.

## 향후 확장(TODO)

- ~~Maven Wrapper(`mvnw`) 추가~~ 완료 — Maven 3.9.9 고정
- ~~자동구성 슬라이스 테스트(`ApplicationContextRunner`)~~ 완료. PostgreSQL·Redis 실연동(Testcontainers, Docker 없으면 자동 skip)도 추가
- ~~분산 세션(Redis) 컨벤션~~ 완료 — `session` 패키지(`<service-name>:session` 네임스페이스·타임아웃 자동 적용)
- ~~`persistence` 패키지(JPA 공통 베이스 엔티티·감사컬럼)~~ 완료 — `BaseEntity`(생성/수정 시각·주체 자동), 주체는 `TraceContext` 인증 사용자에서 채움
- ~~가상스레드/비동기 컨텍스트 전파 유틸(`TraceContext` ↔ `@Async`)~~ 완료 — `async` 패키지(`TraceContextPropagation`/`TraceContextTaskDecorator`)
- ~~응답 표준화 심화~~ 완료 — `ErrorCode` 인터페이스화(소비자 도메인 코드 확장), 성공 응답 자동 래핑(`response`, opt-in), 페이징 응답 `PageResponse`
- ~~공통 OpenAPI(springdoc) 설정~~ 완료 — `openapi` 패키지(info + Bearer JWT 보안스킴, opt-in). Spring Boot 4 호환 springdoc 도입 완료
- ~~HTTP 클라이언트 하드닝~~ 완료 — 타임아웃 컨벤션 기본값 + 재시도(`HttpRetryInterceptor`, opt-in) + 타임아웃→504 에러디코딩
- ~~관측 심화(본문/진입/메서드 로깅)~~ 완료 — `payload`/`intercept`/`aop` 3계층(opt-in) + 로그 민감정보 마스킹(`SensitiveDataMasker`, opt-in)
- ~~멱등성(`Idempotency-Key`)~~ 완료 — `idempotency` 패키지(opt-in, 중복 요청 첫 응답 재방)
- ~~캐시 추상화~~ 완료 — `cache` 패키지(`@EnableCaching` + Redis `CacheManager` 컨벤션, opt-in)
- ~~도메인 이벤트/메시징 코어~~ 완료 — `event` 패키지(broker-agnostic 봉투 + in-process 발행자). 브로커별 어댑터(Kafka/Rabbit)는 인프라 선택 시 별도 진행. **Spring Modulith 이벤트(`events-jpa` 아웃박스)로 신뢰성 확보 + 외부화 경로** 제공(→ [260702.010](docs/260702.010.MODULITH_INTEGRATION.md))
- ~~보안 하드닝~~ 완료(Phase 0~3) — 보안 감사 로깅(`security/audit`)·인가거부 403 정상화·X-User-Id 신뢰경계·JWT audience 검증·오류 정보 미노출·아웃바운드 전파 allowlist·startup 설정 점검·CSRF 조건부·보안 응답 헤더 (→ [260702.006](docs/260702.006.SECURITY_HARDENING.md))
- ~~데이터 접근 참조 샘플(게시판 3-스택)~~ 완료 — JPA/jOOQ/JDBC 계층형 CRUD + Flyway·`ddl-auto=validate`·낙관적 락(`@Version`)·jOOQ 코드젠·동적 쿼리(→ [260702.004](docs/260702.004.BOARD_SAMPLE_GUIDE.md))

## 문서
- **최종 정리(기술스택 구조·제공 기능·샘플 화면/접속 정보 한 장)**: [260704.001.PLATFORM_FINAL_OVERVIEW.md](docs/260704.001.PLATFORM_FINAL_OVERVIEW.md)
- 온보딩(소비 서비스 적용 5분): [260702.009.ONBOARDING.md](docs/260702.009.ONBOARDING.md)
- Spring Modulith 통합(모듈=바운디드 컨텍스트·이벤트 아웃박스·async 전파): [260702.010.MODULITH_INTEGRATION.md](docs/260702.010.MODULITH_INTEGRATION.md)
- 보안 하드닝(이벤트 카탈로그·토글): [260702.006.SECURITY_HARDENING.md](docs/260702.006.SECURITY_HARDENING.md)
- 데이터 접근: 참조 샘플 [260702.004](docs/260702.004.BOARD_SAMPLE_GUIDE.md) · DB 계층 [260702.002](docs/260702.002.DB_LAYER_GUIDE.md)
- DB 기반 프로퍼티 방안: [260706.001](docs/260706.001.DB_BACKED_CONFIGURATION_PLAN.md)
- H2 제거/PostgreSQL-only 전환 컨텍스트: [260706.002](docs/260706.002.POSTGRES_ONLY_MIGRATION_CONTEXT.md)
- 도메인(바우처·서비스교환) DDD: 전략 [260702.007](docs/260702.007.PLATFORM_DDD_STRATEGY.md) · 전술(voucher/ledger) [260702.008](docs/260702.008.VOUCHER_LEDGER_TACTICAL.md)
- 관측/로깅: 로그 포맷 [260629.001](docs/260629.001.LOG_FORMAT.md) · 케이스 매트릭스 [260701.004](docs/260701.004.LOGGING_CASES.md) · 멱등성 [260702.003](docs/260702.003.IDEMPOTENCY.md)
