# 오류 코드·업무 error.code

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

API 업무 오류와 관측용 오류 분류를 구분할 때 쓴다. API error.code와 로그 error.code가 항상 같다고 가정하지 않는다.

## 의존 아티팩트

starter-web의 ErrorCode/BusinessException과 공통 로그 encoder. 소비 도메인 enum은 ErrorCode를 구현한다.

## 설정 키

spring.messages.basename으로 소비 메시지 번들을 추가한다. 임의 문자열을 로그 error.code에 허용하는 범용 설정은 안내하지 않는다.

## 최소 예제

```java
throw new BusinessException(CommonErrorCode.NOT_FOUND);
// 도메인 코드는 소비 서비스의 ErrorCode 구현 enum을 사용한다.
// 예: throw new BusinessException(OrderErrorCode.ORDER_ALREADY_SHIPPED);
```

## 확인 방법

HTTP 404와 API error.code=NOT_FOUND를 확인한다. 로그는 최종 JSON까지 확인한다. 인증 분류 AUTH_TOKEN_MISSING/INVALID/EXPIRED, AUTHN_FAILED, AUTH_PROPAGATION_BLOCKED는 명시 허용된다. 0.6.8부터 BusinessException의 ErrorCode.code()는 공통 처리 경로에서 API 응답·error.business·http.server.complete에 보존된다. 로그 허용 형식은 `[A-Za-z0-9_.:-]{1,128}`이며, 임의 KV에 같은 문자열을 넣는 것으로 이 경로를 대신할 수 없다. 업무 코드 없는 일반 예외는 오류 로그 OPERATION_FAILED, HTTP 완료 로그 HTTP_500 등 기존 분류를 유지한다.

## 원본 정본

0.6.8 업무 코드 근거는 릴리스 `10bd67ddf`와 아래 구현·기존 시험이다. 시험은 재실행하지 않았다.

- [HandledExceptionMetadata](../../../lib/src/main/java/ai/mutuus/common/logging/HandledExceptionMetadata.java)
- [AccessLogger](../../../lib/src/main/java/ai/mutuus/common/logging/AccessLogger.java)
- [CanonicalLogSnapshot](../../../lib/src/main/java/ai/mutuus/common/logging/CanonicalLogSnapshot.java)
- [BusinessErrorCodeTest](../../../lib/src/test/java/ai/mutuus/common/logging/BusinessErrorCodeTest.java)

- [260629.002.API_RESPONSE.md](../../260629.002.API_RESPONSE.md)
- [auth-error-code.md](../../auth-error-code.md)
- [reports/g-common-c3-step2.md](../../reports/g-common-c3-step2.md)

[시작 안내](../README.md)
