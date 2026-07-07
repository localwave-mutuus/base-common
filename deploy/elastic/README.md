# Elasticsearch 적재 아티팩트 (common-platform ECS 로깅 Phase 4)

공통모듈이 내는 ECS 로그(전략·구현: [docs/260707.004.ELASTIC_LOGGING_STRATEGY.md](../../docs/260707.004.ELASTIC_LOGGING_STRATEGY.md))를
Elasticsearch data stream 으로 적재하기 위한 **플랫폼 측 설정**이다.

> **범위·검증 수준(중요)**: 이 디렉터리는 앱(공통모듈) 밖의 배포 아티팩트다. 파일은 **JSON/YAML 문법만 검증**했고,
> 실 Elasticsearch/Filebeat 에 대한 **런타임 검증은 하지 않았다**(모듈 테스트 범위 밖). 운영 반영 전 대상 스택 버전
> (ES 8.x/9.x)·보안(TLS/API key)에 맞춰 조정하고, 스테이징에서 `_ingest/pipeline/_simulate` 등으로 검증할 것.

## 구성

| 파일 | 대상 | 용도 |
|---|---|---|
| `ingest-pipeline-mutuus-common.json` | `PUT _ingest/pipeline/mutuus-common` | legacy→ECS 코어 rename(level/logger_name/stack_trace), 타입 보장(status int·duration long·ip), `data_stream.dataset` fallback, 본문 4096자 절단 |
| `component-template-mutuus-settings.json` | `PUT _component_template/mutuus-logs-settings` | 기본 pipeline·`total_fields.limit`(2000)·압축 |
| `component-template-mutuus-mappings.json` | `PUT _component_template/mutuus-logs-mappings` | ECS 필드 타입(차원=keyword/값=long·ip·date, body=wildcard) + 동적 문자열→keyword |
| `index-template-logs-mutuus.json` | `PUT _index_template/logs-mutuus` | `logs-mutuus.*-*` data stream, 위 component 조합, 기본 ILM |
| `ilm-policies.json` | `PUT _ilm/policy/<키>` (키별) | dataset별 retention(access 90d·error 180d·security 365d·payload/method 7d 등) |
| `filebeat-mutuus.yml` | Filebeat 8.x | NDJSON 수집 → data stream 라우팅(.gz 제외) |

## 적용 순서

ingest pipeline → ILM → component templates → index template 순으로 등록한다(템플릿이 pipeline·ILM 을 참조하므로 먼저 존재해야 함).

```bash
ES=https://localhost:9200
AUTH=(-u elastic:changeme)   # 운영은 API key 사용

# 1) ingest pipeline
curl "${AUTH[@]}" -X PUT "$ES/_ingest/pipeline/mutuus-common" \
  -H 'Content-Type: application/json' --data-binary @ingest-pipeline-mutuus-common.json

# 2) ILM 정책(파일이 여러 정책을 담은 객체 → 키별로 등록). jq 필요.
for name in $(jq -r 'keys[] | select(startswith("_")|not)' ilm-policies.json); do
  jq ".\"$name\"" ilm-policies.json | \
    curl "${AUTH[@]}" -X PUT "$ES/_ilm/policy/$name" -H 'Content-Type: application/json' --data-binary @-
done

# 3) component templates
curl "${AUTH[@]}" -X PUT "$ES/_component_template/mutuus-logs-settings" \
  -H 'Content-Type: application/json' --data-binary @component-template-mutuus-settings.json
curl "${AUTH[@]}" -X PUT "$ES/_component_template/mutuus-logs-mappings" \
  -H 'Content-Type: application/json' --data-binary @component-template-mutuus-mappings.json

# 4) index template
curl "${AUTH[@]}" -X PUT "$ES/_index_template/logs-mutuus" \
  -H 'Content-Type: application/json' --data-binary @index-template-logs-mutuus.json
```

## dataset별 ILM 적용(선택)

기본 템플릿은 모든 `logs-mutuus.*` 에 `mutuus-logs-default`(90d)를 건다. dataset마다 다른 retention을 주려면
**해당 dataset만 매칭하는 상위 우선순위 인덱스 템플릿**을 추가해 `index.lifecycle.name` 을 덮어쓴다. 예(payload 7d):

```bash
curl "${AUTH[@]}" -X PUT "$ES/_index_template/logs-mutuus.payload" -H 'Content-Type: application/json' -d '{
  "index_patterns": ["logs-mutuus.payload-*"],
  "data_stream": {},
  "composed_of": ["mutuus-logs-settings","mutuus-logs-mappings"],
  "priority": 300,
  "template": { "settings": { "index": { "lifecycle": { "name": "mutuus-payload" } } } }
}'
```

## Kibana

`logs-mutuus.*` (또는 dataset별 `logs-mutuus.access-*`) data view 를 만들면 Discover/Lens 에서 필드가 노출된다.
필드 노출·집계 축 조건은 문서 260707.004 의 "Kibana Discover 활용" 섹션 참조.

## 검증 팁

```bash
# 샘플 문서로 pipeline 동작 확인(런타임 검증)
curl "${AUTH[@]}" -X POST "$ES/_ingest/pipeline/mutuus-common/_simulate" -H 'Content-Type: application/json' -d '{
  "docs": [ { "_source": { "@timestamp":"2026-07-07T00:00:00Z", "level":"INFO", "logger_name":"ai.mutuus.common.access",
    "event.dataset":"mutuus.access", "event.duration":"1230000", "http.response.status_code":"200", "client.ip":"10.0.0.1" } } ]
}'
```
