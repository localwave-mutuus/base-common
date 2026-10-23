# API 응답

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

성공·오류 응답을 공통 봉투로 제공할 때 쓴다. HTTP 상태와 본문 코드를 함께 판단한다.

## 의존 아티팩트

starter-web의 ApiResponse, BusinessException, GlobalExceptionHandler.

## 설정 키

mutuus.common.response-wrapper.enabled는 opt-in. 수동 ApiResponse 반환에는 자동 래핑이 필요 없다.

## 최소 예제

```java
return ApiResponse.ok(Map.of("message", "hello"));
// 오류 경로
// throw new BusinessException(CommonErrorCode.NOT_FOUND);
```

## 확인 방법

제품의 허용된 엔드포인트에서 성공 code=OK/data, 오류 HTTP 상태/error.code, traceId를 확인한다. 자동 래핑은 기존 봉투·String·비JSON 등을 제외하므로 모든 반환형에 강제된다고 가정하지 않는다.

## 원본 정본

- [260629.002.API_RESPONSE.md](../../260629.002.API_RESPONSE.md)
- [260702.009.ONBOARDING.md](../../260702.009.ONBOARDING.md)

[시작 안내](../README.md)
