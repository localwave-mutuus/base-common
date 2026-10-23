# 추적 상관키

기준: common-platform 0.6.9. 이 문서는 적용 요약이며 실제 제품 시험 통과 보고가 아니다.

## 언제 쓰나

서비스 간 요청과 로그를 같은 실제 trace/span으로 연결할 때 쓴다. 0.3.0의 X-* 중심 안내는 과거 동작이다.

## 의존 아티팩트

common-platform과 제품이 승인한 OTel Java agent/extension 구성. TraceHeaders.apply 호출만으로 HTTP 전파를 보장하지 않는다.

## 설정 키

OTEL_SERVICE_NAME, OTEL_PROPAGATORS, OTEL_TRACES_SAMPLER, OTEL_TRACES_SAMPLER_ARG. agent 설치·목적지 정책은 제품 담당 구성과 함께 확인한다.

## 최소 예제

```sh
export OTEL_SERVICE_NAME=example-service
export OTEL_PROPAGATORS=tracecontext,baggage
```

## 확인 방법

승인된 상하위 호출 로그의 trace.id와 실제 부모 parent.id/span.id를 대조한다. root·lineage 미연결 시 parent를 추정하지 않는다. agent 소유 HTTP span을 별도로 중복 생성하지 않는다. exporter/wire·redirect/재시도와 실제 앱 적용 인수는 구현 보고서 잔여 범위를 확인한다. 환경변수 두 개만으로 설치·전파 성공을 판정하지 않는다.

## 원본 정본

- [261017.001.TRACE_CORRELATION.md](../../261017.001.TRACE_CORRELATION.md)
- [contracts/g-common-trace-log-contract.md](../../contracts/g-common-trace-log-contract.md)
- [reports/g-common-c3-step1.md](../../reports/g-common-c3-step1.md)

[시작 안내](../README.md)
