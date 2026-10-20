# common-platform 0.6.0 최종통합·발행

REQ-261021-02 / attempt 261021-0722-a13-ex.a01.

0710.a01 누적 patch 111파일을 d2e75a2f4e694440a0926b47e03b3c76ca0bc312에 한 번 적용했다. 계약·계획·step1/2/3 보고서 및 §6 설계를 보존했다. GitHub main은 동일 SHA였다. v0.6.0/v0.6.1 태그와 해당 Publish 실행은 조회 시 없었다. Maven 정식5좌표의 0.6.0 POM 조회는 모두 HTTP 404였다. Packages REST API는 접근되지 않아 목록 기반 부재는 확인하지 못했다.

## 통합 검증

이유: 0.6.0 POM 정렬 뒤 lib·extension·starter 사이 컴파일/패키징 연결을 한 번 확인한다.

`bash ./mvnw -B -pl parent,bom,lib,agent-extension,starters/starter-web,starters/starter-batch -am -DskipTests package`

6모듈 BUILD SUCCESS (2026-10-20T22:23:29Z), 로그 `final-integration-build.log`(TEAM_OUT_DIR). 기존 pass 시험/전체회귀/sample-api 로컬 실행 없음. 관련9개 POM을 0.6.0으로 정렬했다. extension은 기존 deploy.skip=true를 유지하며 정식8파일에 추가하지 않는다.

Publish의 0.6.0/0.6.1 경로는 이 6모듈 패키징/게시만 수행하고 lib 시험을 반복하지 않는다. 발행 후 빈 Maven 소비 저장소의 sample-api OtelRegistryGenerationContractTest 한 개로 v5/94필드와 계약 생성물을 확인한다. 소비 fixture의 이전 v4/90 기대만 v5/94로 정렬했다. 다른 버전 경로는 보존했다.

## 발행 상태

발행 준비 완료. 실제 원격 SHA/태그/run/8SHA는 실행 결과 절에서 확정한다. 이 시점 성공을 주장하지 않는다.

## 확인하지 못함

외부메일DB·MEM/BATCH 실제 transaction/claim/멱등·모바일 실제 UI root·서비스별 opt-in/승인 projection schema·실제 runtime 설치·OpenSearch/C2 실측/C7/C8 실접속은 미확인이다. projection 본문 차단과 HTTP agent 단일 소유·명시전파 금지를 유지한다. AWS 읽기복구 후 A18 실측은 별도후속이다. step1/2/3의 실제 미확인 항목을 이 빌드로 대체하지 않는다. 기존 v0.5.1/v0.5.2 태그 보존, 0.5.x 재발행 없음.

## 이슈대처 후보

- 증상: patch 적용 전 지정 보고서 No such file or directory.
- 원인: 고정 base에는 없고 전달된 누적 patch 안에 있었다.
- 확인 명령: 지정 patch 읽기 및 git apply --check 후 적용.
- 조치: 추측 재구현 없이 누적 patch 한 번 적용 후 보고서 읽기.
- 재발 방지: 누적 patch에 포함된 보고서는 적용 후 확인한다.
- 증상: GitHub Packages REST 조회 HTTP 404/목록 접근 실패.
- 조치: 오류 문자열로 교훈 검색 후 승인 저장소 Maven 실제 좌표를 조회했다(5 POM HTTP 404). REST 목록 접근 성공으로 주장하지 않는다.

교훈 무관: kb://core/ops/lessons/topic-9d939aa20431030e63ab6c76.md — team-task 제출/verify_allow를 사용하지 않았다.
