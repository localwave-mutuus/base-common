# GitHub Packages 소비

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

다른 개발자 PC가 게시된 0.6.9를 받아 소비할 때 쓴다. 게시 작업은 이 안내 범위 밖이다.

## 의존 아티팩트

Maven/JDK 21, GitHub Packages의 parent/BOM/lib/스타터. 샘플과 루트 aggregator는 게시 대상이 아니다.

## 설정 키

GS_MAVEN_READ_USER, GS_MAVEN_READ_TOKEN과 settings server/repository ID github-common-platform. PAT(classic) read:packages 및 필요 시 조직 SSO 승인을 준비한다.

## 최소 예제

```sh
bash ./check-common-platform.sh ./settings.xml.template
```

## 확인 방법

위 명령은 usage가 아닌 onboarding 디렉터리에서 실행한다. 숨김 입력으로 자격을 준비하는 README 절차를 먼저 따른다. PASS는 새 임시 캐시에 common-platform:0.6.9:pom 수신만 뜻한다. JAR/전이 의존성·제품 빌드·서버 시험은 별도다. 정본의 github ID/옛 판 예시와 현재 템플릿 ID를 섞지 않는다. 기존 settings 병합 시 server/repository ID를 일치시킨다.

## 원본 정본

- [260827.003.GITHUB_PACKAGES_PUBLISHING.md](../../260827.003.GITHUB_PACKAGES_PUBLISHING.md)

[시작 안내](../README.md)
