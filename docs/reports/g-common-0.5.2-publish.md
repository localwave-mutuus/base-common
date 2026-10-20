# common-platform 0.5.2 발행 결과 — 중단

## 결과 및 확정 상태

정식 0.5.2 발행 미완료. 승인된 최소 시험은 성공했으나 실행 담당의 명령 제어 오류로 수정 전 커밋에 v0.5.2 태그를 생성했다. 태그 이동·삭제·강제 push·재발행은 하지 않았다. 후속 판단은 PM에게 요청한다.

- 작업 시작: 390c768aae3b3f02bb481e705c2cfb60ca95350b.
- 사전 GitHub main 및 첨부 증거: 2e666237731f21ea18803b73c10f9b3210bab7df. git fetch 후 fast-forward만 수행했다.
- 사전 v0.5.2 없음. v0.5.1은 위 main SHA를 가리켰다.
- 기존 0524.a01·0530.a01 patch 미통합. 0533.a02의 POM 8개와 시험 5개 수정만 반영했다.
- 현재 로컬 POM: 0.5.2. 원격 main·v0.5.1·v0.5.2: 모두 2e666237731f21ea18803b73c10f9b3210bab7df (POM 0.5.1).
- main push 요청은 변경 없는 HEAD를 보내 실제 main 변경이 없었다. 새 태그 push는 1회였다.

## 원인별 수정

| 시험 | 반영 내용 |
| --- | --- |
| UiCodeResolverTest | bo.8 버전·메서드명 및 고정 digest 5a86338c011c0c2baeb482752354dcfaf3008de7a28128dd170594fb28c3ea1e 정렬. 생성본/런타임 바이트 동일성과 고정 digest assertion 유지 |
| ScreenMetadataLoggingTest | 카탈로그 입력과 출력 기대 버전 bo.8 정렬 |
| TraceFilterTest | 메타데이터 입력 버전 bo.8 정렬, 보존·복원 assertion 유지 |
| AccessLoggerTest | 원문 query 미출력 계약 검증 |
| OtelLoggingContractTest | 등록 화면 BO-ISS-010·16자리 이벤트 ID·bo.8 입력과 내부 화면 ID 미출력 계약 검증 |

생산 코드 수정 없음. 8개 POM의 자체 버전·내부 참조를 0.5.2로 정렬했다.

## 승인된 최소 시험 1회

검증 이유: 이전 실패 이후 수정된 bo.8 digest를 포함하여 지정된 5개 시험 클래스의 성공 여부 확인.

명령:
```sh
bash ./mvnw -B -pl samples/sample-api -am '-Dtest=UiCodeResolverTest,ScreenMetadataLoggingTest,AccessLoggerTest,OtelLoggingContractTest,TraceFilterTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

2026-10-20T20:43:29Z 종료, exit 0, BUILD SUCCESS, 13.167초.
28개 실행·실패 0·오류 0·생략 0. 동일 입력 재시험이나 추가 로컬 전체 회귀 없음. 원본 로그는 결과 출력 디렉터리의 c1-minimum.log에 보존한다.

## 발행 오류 및 workflow 증거

git commit이 작성자 정보 부재로 실패했다. 셸이 실패 후에도 후속 명령을 계속 실행하여 수정 전 HEAD에 v0.5.2 태그가 생성됐다. 변경 파일은 stage 상태로 보존되어 있으며 수정 커밋은 없다. 이는 실행 담당 오류이며 시험 실패가 아니다.

- workflow ID: 343476181.
- run ID: 39494450268.
- URL: https://github.com/localwave-mutuus/base-common/actions/runs/39494450268
- head SHA: 2e666237731f21ea18803b73c10f9b3210bab7df.
- 트리거: v0.5.2 태그 push 1회. dispatch·재실행 없음.
- 잘못된 0.5.1 게시 방지를 위해 cancel API 1회 요청, HTTP 202 수락.
- 취소 직후 조회: in_progress, Verify the reactor before publication 실행 중, Publish verified reactor artifacts 및 게시본 소비 단계 pending. 최종 상태는 아래 후속 증거에 기록한다.

## 정식 8파일

실재 다운로드와 SHA 확정은 수행하지 않았다. 태그/POM 불일치 상태에서 0.5.2 발행 성공을 추정하지 않는다.

| 좌표 (groupId ai.mutuus.common, version 0.5.2) | 파일명 | SHA-256 |
| --- | --- | --- |
| common-platform-parent:pom | common-platform-parent-0.5.2.pom | 미확정 |
| common-platform-bom:pom | common-platform-bom-0.5.2.pom | 미확정 |
| common-platform:pom | common-platform-0.5.2.pom | 미확정 |
| common-platform:jar | common-platform-0.5.2.jar | 미확정 |
| common-platform-starter-web:pom | common-platform-starter-web-0.5.2.pom | 미확정 |
| common-platform-starter-web:jar | common-platform-starter-web-0.5.2.jar | 미확정 |
| common-platform-starter-batch:pom | common-platform-starter-batch-0.5.2.pom | 미확정 |
| common-platform-starter-batch:jar | common-platform-starter-batch-0.5.2.jar | 미확정 |

비밀은 team-secret --org b04 run으로 메모리 환경에 주입하고 스크립트 내부 gh[opsu]_ 정규식으로 추출했다. 토큰·환경·헤더·예외 객체를 출력하거나 파일·remote URL에 저장하지 않았다. 운영·다른 저장소·GS·OpenSearch·C2·C3 변경 없음. 0.5.0 바이트 재대조 없음.

교훈 무관: kb://core/ops/lessons/topic-9d939aa20431030e63ab6c76.md — verify_allow/test-runner 제출 경로를 사용하지 않았다.

## 최종 workflow 조회
{"id": 39494450268, "status": "in_progress", "conclusion": null}
- Verify the reactor before publication: in_progress / None
- Publish verified reactor artifacts: pending / None
- Verify sample-api consumes the published package: pending / None

후속 확정 조회: in_progress / None
- Verify the reactor before publication: in_progress / None
- Publish verified reactor artifacts: pending / None
- Verify sample-api consumes the published package: pending / None
