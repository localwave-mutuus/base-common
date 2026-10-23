# 화면·요소 코드·ui-catalog

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

UI 동작과 서버 관측을 표준 화면 코드로 연결할 때 쓴다. UI 힌트는 신원·권한의 근거가 아니다.

## 의존 아티팩트

0.6.9 런타임 UiCodeAliases와 생성된 bo-catalog.ts 소비본. 라이브러리 판과 카탈로그 판은 별도다.

## 설정 키

요청 헤더 X-Screen-Id, X-Screen-Catalog-Version, X-UI-Element-Id, X-UI-Action, X-Event-Id. 0.6.9 대응 카탈로그는 1.0.0-ui.9다.

## 최소 예제

```http
X-Screen-Id: APP-AUT-010
X-Screen-Catalog-Version: 1.0.0-ui.9
X-UI-Action: view
```

## 확인 방법

golmok.screen.id/catalog_version을 확인한다. 별칭은 system/kind/value/version이 정확히 맞는 유일 RESOLVED만 해석한다. 미등록·모호 입력은 후보로 추측하지 않는다. 요소는 등록된 코드를 소비하고 런타임 문법/화면 prefix 확인을 정본 요소 membership 검증으로 오해하지 않는다. X-Event-Id는 실제 UI root span의 16자리 lowercase hex이며 요청 CLIENT span과 다를 수 있다.

## 원본 정본

- [261005.002.SCREEN_METADATA.md](../../261005.002.SCREEN_METADATA.md): 0.2.6-SNAPSHOT 당시 선택 헤더 기록이다. 현재 카탈로그 판·필드 계약은 아래 ui-catalog를 따른다.

- [ui-catalog/README.md](../../ui-catalog/README.md)
- [ui-catalog/bo-tracing.md](../../ui-catalog/bo-tracing.md)
- [ui-catalog/ci-guard.md](../../ui-catalog/ci-guard.md)

[시작 안내](../README.md)
