# common-platform Docs

이 디렉터리의 정본 문서 기준이다. 새 안내를 추가할 때는 아래 문서 중 하나를 갱신하거나, 새 문서를 추가한 뒤 이 색인에 연결한다.

## 현재 기준

| 주제 | 정본 |
|---|---|
| 현행화 계획(갭 분석·실행 순서) | [260827.001.PLATFORM_ALIGNMENT_PLAN.md](260827.001.PLATFORM_ALIGNMENT_PLAN.md) |
| 암호화 시크릿 로더 구현·검증 | [260827.002.ENCRYPTED_SECRET_LOADING_IMPLEMENTATION.md](260827.002.ENCRYPTED_SECRET_LOADING_IMPLEMENTATION.md) |
| 인수인계 온보딩(유지보수 담당자) | [260729.001.HANDOVER_ONBOARDING.html](260729.001.HANDOVER_ONBOARDING.html) |
| 전체 개요 | [260704.001.PLATFORM_FINAL_OVERVIEW.md](260704.001.PLATFORM_FINAL_OVERVIEW.md) |
| 다음 작업자 handoff | [260701.003.HANDOFF.md](260701.003.HANDOFF.md) |
| 샘플 데모 케이스 | [260630.001.DEMO_CASES.md](260630.001.DEMO_CASES.md) |
| 샘플 포트 정책 | [260707.003.SAMPLE_PORT_POLICY.md](260707.003.SAMPLE_PORT_POLICY.md) |
| Secret 관리 | [260707.002.SECRET_MANAGEMENT_STANDARD.md](260707.002.SECRET_MANAGEMENT_STANDARD.md) |
| Nexus 재등록 | [260707.001.NEXUS_REREGISTRATION.md](260707.001.NEXUS_REREGISTRATION.md) |
| GitHub Packages Maven 배포/소비 | [260827.003.GITHUB_PACKAGES_PUBLISHING.md](260827.003.GITHUB_PACKAGES_PUBLISHING.md) |
| Elasticsearch 로깅 전략 | [260707.004.ELASTIC_LOGGING_STRATEGY.md](260707.004.ELASTIC_LOGGING_STRATEGY.md) |
| PostgreSQL-only 전환 | [260706.002.POSTGRES_ONLY_MIGRATION_CONTEXT.md](260706.002.POSTGRES_ONLY_MIGRATION_CONTEXT.md) |
| DB-backed configuration | [260706.001.DB_BACKED_CONFIGURATION_PLAN.md](260706.001.DB_BACKED_CONFIGURATION_PLAN.md) |

## 작성 규칙

- 평문 비밀번호, token, Nexus/admin credential은 문서에 쓰지 않는다.
- DB 이름은 live/Aiven 기준 `golmok`으로 안내한다.
- 샘플 현재 포트는 8090, 수동/백그라운드 테스트 슬롯은 8095~8099다.
- 새 DB 검증 기준은 PostgreSQL이다. H2는 전환 중 호환 경로로만 언급한다.
- 테스트 명령은 포트와 공유 인프라에 영향을 주지 않는 방식부터 안내한다.
