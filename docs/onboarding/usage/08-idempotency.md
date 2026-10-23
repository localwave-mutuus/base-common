# 멱등성

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

동일 논리 작업의 중복 제출·네트워크 재시도를 제어할 때 쓴다. 클릭마다 새 키를 만들지 않는다.

## 의존 아티팩트

웹 common-platform IdempotencyFilter/IdempotencyStore. 기본 메모리 저장소는 단일 인스턴스용이며 분산은 소비 서비스의 공유 저장소 빈이 필요하다.

## 설정 키

mutuus.common.idempotency.enabled, header-name, ttl, methods.

## 최소 예제

```yaml
mutuus.common.idempotency:
  enabled: true
  header-name: Idempotency-Key
  ttl: 24h
  methods: [POST, PUT, PATCH]
```

## 확인 방법

승인된 시험 데이터로 동일 키를 재전송해 첫 상태·본문과 Idempotent-Replayed: true를 확인한다. 처리 중 중복은 409/in-progress다. 헤더 없음·비대상 메서드는 통과한다. 사용자 바인딩/발급분만 허용하는 3상태 토큰 저장소는 기본 제공이 아니므로 별도 구현이 필요하다.

## 원본 정본

- [260702.003.IDEMPOTENCY.md](../../260702.003.IDEMPOTENCY.md)

[시작 안내](../README.md)
