# 스타터·BOM

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

소비 서비스의 공통 의존성을 정렬할 때 쓴다. 기존 서비스는 직접 버전 덮어쓰기부터 확인한다.

## 의존 아티팩트

ai.mutuus.common:common-platform-bom:0.6.9와 web 또는 batch 스타터. batch는 web/security를 전이하지 않는다. 루트 aggregator는 소비하지 않는다.

## 설정 키

Maven dependencyManagement의 BOM version/type/scope. 기능 설정과 의존성 정렬은 별개다.

## 최소 예제

```xml
<dependencyManagement><dependencies><dependency>
  <groupId>ai.mutuus.common</groupId>
  <artifactId>common-platform-bom</artifactId><version>0.6.9</version>
  <type>pom</type><scope>import</scope>
</dependency></dependencies></dependencyManagement>
<dependencies><dependency>
  <groupId>ai.mutuus.common</groupId>
  <artifactId>common-platform-starter-web</artifactId>
</dependency></dependencies>
```

## 확인 방법

소비 Maven 루트에서 승인 settings로 dependency:tree -Dincludes=ai.mutuus.common을 확인한다. 비웹은 예제의 starter-web을 starter-batch로 바꾼다. 스타터에 별도 버전을 붙이지 않는다.

## 원본 정본

- [BOM POM](../../../bom/pom.xml)
- [웹 스타터 POM](../../../starters/starter-web/pom.xml)

- [260702.009.ONBOARDING.md](../../260702.009.ONBOARDING.md)
- [260827.003.GITHUB_PACKAGES_PUBLISHING.md](../../260827.003.GITHUB_PACKAGES_PUBLISHING.md)

[시작 안내](../README.md)
