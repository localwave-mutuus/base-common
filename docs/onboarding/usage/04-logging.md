# 로그 형식·로깅

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

요청 결과와 실패를 구조화 JSON으로 관측할 때 쓴다. 옛 로그 정본의 request.received/completed 및 raw 본문 예시를 최신 계약과 혼용하지 않는다.

## 의존 아티팩트

starter-web 또는 starter-batch, 공통 logback-common.xml. 최신 정본 encoder는 CommonJsonEncoder/CanonicalLogSnapshot 경로다.

## 설정 키

mutuus.common.service-name, mutuus.common.app-code, mutuus.common.logging.environment. payload-logging과 safe-payload는 별도이며 기본 비활성이다.

## 최소 예제

```xml
<configuration>
  <property name="LOG_DIR" value="logs"/>
  <include resource="logback-common.xml"/>
</configuration>
```

## 확인 방법

요청당 http.server.complete 한 건의 event.outcome/status/event.duration(ns)을 확인한다. trace.id는 유효 문맥이 있을 때만 나온다. raw 본문·query·예외 message/stack 자동 출력을 기대하지 않는다. 안전 projector 결과도 정본 payload 내용 출력이 승인되지 않아 정책/생략 메타데이터만 기록한다.

## 원본 정본

- [260629.001.LOG_FORMAT.md](../../260629.001.LOG_FORMAT.md)
- [260707.005.LOGGING_ONBOARDING.md](../../260707.005.LOGGING_ONBOARDING.md)
- [reports/g-common-c3-step1.md](../../reports/g-common-c3-step1.md)
- [reports/g-common-c3-step2.md](../../reports/g-common-c3-step2.md)

[시작 안내](../README.md)
