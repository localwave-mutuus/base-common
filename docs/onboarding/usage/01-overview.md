# 전체 개요·기존 온보딩

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

새 API·배치 서비스에 공통 기능을 처음 적용할 때 쓴다. common-platform은 독립 실행 서버가 아니라 소비 서비스 안에서 동작한다.

## 의존 아티팩트

common-platform-bom + starter-web 또는 starter-batch. lib의 optional 기능은 필요한 classpath와 설정이 갖춰져야 동작한다.

## 설정 키

spring.application.name, mutuus.common.service-name, mutuus.common.app-code.

## 최소 예제

```yaml
spring.application.name: example-service
mutuus.common.service-name: example-service
mutuus.common.app-code: EXMP
```

## 확인 방법

의존성 트리에서 0.6.9를 확인하고 공개된 제품 시험 경로에서 응답 봉투·JSON 로그를 확인한다. 보안·DB·외부 서비스 준비는 제품별 승인 설정을 따른다.

## 원본 정본

- [260704.001.PLATFORM_FINAL_OVERVIEW.md](../../260704.001.PLATFORM_FINAL_OVERVIEW.md)
- [260702.009.ONBOARDING.md](../../260702.009.ONBOARDING.md)

[시작 안내](../README.md)
