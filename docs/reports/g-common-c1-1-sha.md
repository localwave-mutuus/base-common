# [REQ-261021-02] C1-1 — 0.5.0 배포본 바이트 대조표

판정: 확보한 BO dev CI 공급본은 기존 배포 SHA와 두 jar 모두 일치한다. core는 **16d59fabb82a의 재빌드와 class·리소스가 전부 동일**하고, 390c768aae3b와는 class 2개가 다르다. 공급 영수증의 16d59fa 출처 주장과 실물이 부합한다. 다만 이 내용 일치만으로 유일한 원본 커밋이나 실행 중 BO 이미지의 jar까지 확정하지 않는다. **0.6.1 정식 기준 결정은 유지한다.**

## 재개 전 외부 변경 사전확인 · 제출 오류 정정

확인 시점: 2026-10-20 23:49 UTC. 작업 전 조직 루트 `docs/20-kb/dev-access-catalog.md`를 읽었다. 이번 HEAD는 `575229fac4d9a6a8850e535336fa96420023753b`이며 최초 `git status --short`는 빈 결과였다. C1-2 이후 작업은 수행하지 않았다.

아래 경로의 공통 루트는 `/mnt/disk/00.workspaces/b04.mutuus-operation/`이다. W는 `teams/A13.공통모듈/wt/261021-0817-a13-ex.a01/`이다. 세 시도의 `var/worker/attempts/<attempt>/in/brief.md`, `out/cli.stdout.raw`, `out/last.md`, `journal.json` 내 receipt와 작업 사본 보고서를 직접 읽었다. 0822 a02의 후속 지시는 journal의 envelope·human_inputs에도 기록돼 있다.

| 사전확인 대상 | 실제 기록·관측 | 판정과 한계 |
|---|---|---|
| 261021-0817-a13-ex.a01 | 실행 로그의 완료된 명령 13건: 로컬 입력 읽기·복사·추출, 오프라인 package 2건, SHA/ZIP 비교, 문서 작성, 교훈 조회. 두 build.log는 BUILD SUCCESS, 23:20:16Z 종료. receipt exit=0/end_reason=exited, journal state=interrupted | 기록된 배포·DB·메시지·push 명령 없음. 전달 오류와 구현 종료를 구별한다. plugin 내부·독립 외부 감사까지 증명하지 않음 |
| 261021-0822-a13-ex.a01 / a02 | 실제 명령은 0817 기록·영수증·로컬 탐지 행 읽기, /proc 조사, 사전확인 보고서 작성. 두 receipt exit=0, write_zone_violations=[], flags=[] | 기록된 배포·DB·메시지·push 없음. 원 대조·빌드 재실행 없음. 당시 최종 보고서는 공유 캐시·원격 patch 미확인으로 정정됨 |
| 잔여 프로세스 | 23:49 UTC /proc에서 각 journal PID/PGID(2282359, 2304798, 2319516) 및 각 이전 작업 사본 cwd/하위 cwd 일치 프로세스 0건(현재 조사 프로세스 제외) | 관측 범위에서 잔여 실행 없음. 식별자·cwd·그룹을 모두 바꾼 분리 프로세스까지 부재 확정하지 않음 |
| 공유 캐시 쓰기 | 0817 package 명령에 install/deploy·명시적 repo.local 없음. 현재 `/home/cain/.m2`에서 23:19:37~23:21:25Z mtime 파일 0건 | 당시 Maven/plugin 부수 쓰기와 설정에 따른 다른 캐시 경로는 전후 스냅샷·접근 감사가 없어 미확인. 캐시를 사용하는 새 빌드는 수행하지 않음 |
| 이전 제출 patch | 0817 receipt: patch 7,917,483 bytes, SHA `be3081bbd7ba9dd2dd529e3f2074c3c6c17b0807e7d081b584a9dfe3a44bf9d1`, write_zone_violations 815건, flags secret_suspect/write_zone_violation | 이전 보고서의 “patch에는 문서만 포함”은 사실과 달라 철회한다. 원격 patch는 회수·적용하지 않았고 이전 사본은 수정·삭제하지 않음 |

0822 a02 보고서는 로컬 지정 탐지 8행을 UI 요소 코드 자기 매핑 오탐으로 판단했다. 이는 원격 patch 전체 비밀 부재 증명이 아니다. 원격 객체 전체의 내용은 이번에도 확인하지 않았으며, brief에 따라 회수하지 않고 로컬 보고서·원 증거로 새 문서를 작성했다. 현재 수행할 문서 회수에 남은 구체적인 외부 중복 동작은 관측되지 않았다. 이 판단을 과거 외부 변경의 절대 부재나 전체 재시도 안전성 pass로 확대하지 않는다.

## 증거 재사용과 최소 보강

W의 `.tmp-c1-1/comparison.json`, `bundle-sha.json`, `{390c768aae3b,16d59fabb82a}/build.log`, 보고서와 실제 실행 로그(item_7/8/10/14/15)를 대조했다. `.tmp-c1-1/build.log` 단일 파일이 아니라 각 후보 하위의 build.log가 원본이다. 현재 지정 read-repo 두 사본의 HEAD도 아래 전체 커밋 ID와 일치한다. 이미 성공한 동일 입력 빌드·SHA·내용 비교는 재실행하지 않았다.

검증 이유: 이전 전달 오류와 근거 누락을 막기 위해 기존 JSON의 전체 SHA·파일 차이, 성공 로그, 새 표의 값 및 제출 경로를 대조한다. 기존 비교에 없는 390c768의 ZIP 포장 메타데이터만 원 jar를 읽어 보강했다. 추출·빌드·임시 소스 작성 없이 이름별 ZipInfo를 비교했다.

| 배포본 대비 ZIP 포장 | core (디렉터리 포함 243 엔트리) | web (디렉터리 포함 7 엔트리) |
|---|---|---|
| 390c768 | DOS 시각 차이 243, external_attr 차이 9, 순서 다름 | DOS 시각 차이 7, external_attr 차이 1, 순서 같음 |
| 16d59fa (기존 item_14와 일치) | DOS 시각 차이 243, external_attr 차이 9, 순서 다름, 압축 payload 차이 0 | DOS 시각 차이 7, external_attr 차이 1, 순서 같음, 압축 payload 차이 0 |

내용 표의 시각 차이 207/3은 디렉터리를 제외한 파일 기준이고 위 243/7은 디렉터리를 포함한다. class·리소스 차이와 혼동하지 않는다. 테스트는 기존 실행에서도 `-DskipTests`로 생략됐으며 BUILD SUCCESS를 테스트 pass라고 부르지 않는다.

## 실물·원본 증거

- 입력 루트: `/mnt/disk/00.workspaces/b04.mutuus-operation/teams/A18.공통-CICD/build/bo-common-050-seed/`.
- 실물: `common-0.5.0-0143bf9d1b1d.tgz` 내부 두 jar. 같은 루트 `deps/` 두 jar도 전체 SHA가 각각 같다.
- 기존 전체 SHA 원본: `common-deps-0.5.0.sha256`. 생략 SHA로 동일성을 판정하지 않았다.
- 공급 경위: `RECEIPT.md`, `README-PM.md`, `REPORT.md`. 영수증은 원본을 `16d59fabb82a3e74c93d38ed2e5b23156d755125`, 빌드를 git archive 사본에서의 `mvnw -o -DskipTests -pl parent,bom,lib,starters/starter-web -am package`로 명시한다.
- 기존 동일 입력 pass 재사용: `04-verify.txt`의 세 실행기 × (job Maven 저장소, EFS 시드) `ok=6 bad=0`, `VALIDATE local rc=0`. 설치·validate를 재실행하지 않았다. 이는 당시 설치 증거이며 현재 실행 이미지 증거는 아니다.

## 전체 SHA-256 대조

core = `common-platform-0.5.0.jar`, web = `common-platform-starter-web-0.5.0.jar`.

| 대상 | jar | 전체 SHA-256 | 결과 |
|---|---|---|---|
| 배포 tgz / deps / 기존 SHA 원본 | core | `4b74354d43c8ccca45209f67da8dbc22de63f2cf67d099f0c67ba05538ca4874` | 기존 배포와 일치 |
| 390c768 재빌드 | core | `53eb5abb61fff3fa3c9b662b1e6b4e5899d667d26ba2d4733a4ffac42ce1e112` | jar 전체 바이트 불일치 |
| 16d59fa 재빌드 (0817 증거 재사용) | core | `79b6412df7aa8394786b8d8e12ac6e4d7212fbae9538b86e5757622284b38c58` | jar 전체 바이트 불일치 |
| 배포 tgz / deps / 기존 SHA 원본 | web | `79cc0c3d48c9979e6ecef953b44420cf95960aab73d2b7461d76ca4f31ed9ea2` | 기존 배포와 일치 |
| 390c768 재빌드 | web | `e14f80a5d51af93e08986c2d45f450b24349c763dcd1486becd40685f6e38311` | jar 전체 바이트 불일치 |
| 16d59fa 재빌드 (0817 증거 재사용) | web | `e14f80a5d51af93e08986c2d45f450b24349c763dcd1486becd40685f6e38311` | jar 전체 바이트 불일치 |

## 재빌드 조건·파일 차이

0817 시도는 읽기 전용 지정 사본 `…a01.read/base-common-390c768aae3b`, `…a01.read/base-common-16d59fabb82a`에서 빌드에 필요한 파일을 실행 사본 `.tmp-c1-1/{commit}/`으로 복사했다(target 제외). 고정 커밋 전체 ID는 각각 `390c768aae3b3f02bb481e705c2cfb60ca95350b`, `16d59fabb82a3e74c93d38ed2e5b23156d755125`이다.

0817 시도의 2026-10-20 23:20 UTC, Linux amd64, Ubuntu OpenJDK `21.0.12.1+1-1-24.04.4`, Maven `3.9.9`, compiler release 21. 두 사본에서 `bash mvnw -o -DskipTests -pl parent,bom,lib,starters/starter-web -am package` 각 1회 BUILD SUCCESS. 기존 로컬 Maven 캐시를 사용했고 install/deploy는 하지 않았다. 로그는 `.tmp-c1-1/{commit}/build.log`, 전체 SHA·엔트리 비교는 `.tmp-c1-1/comparison.json`에 있다(0817 사본에 보존된 원 증거이며 이번 사본으로 복사하지 않음).

검증 이유: brief가 요구한 실물 SHA 대조 1회 및 후보별 class·리소스 바이트 비교로 내용 동일성을 판단하기 위함. ZIP 엔트리명을 키로 디렉터리를 제외한 모든 파일의 압축 해제 바이트를 직접 비교했다.

| 배포본 대비 후보 | core (class 192 / 리소스 15) | web (class 0 / 리소스 3) |
|---|---|---|
| 390c768 | 추가·삭제 0, class 2개 변경, 나머지 class 190개 및 리소스 15개 동일 | 추가·삭제·변경 0 |
| 16d59fa | 추가·삭제·변경 0, 207개 파일 모두 동일 | 추가·삭제·변경 0, 3개 파일 모두 동일 |

390c768의 변경 파일은 `ai/mutuus/common/core/UiCodeResolver.class`, `ai/mutuus/common/uicatalog/UiCodeAliases.class`이다. 후보 사이 UiCodeAliases 소스의 catalogVersion은 390c768 `1.0.0-bo.8`, 16d59fa `1.0.0-bo.7`이다. 변경 class가 있으므로 390c768 core의 불일치를 ZIP 시각 차이로 설명할 수 없다.

ZIP 차이는 내용 차이와 분리했다. 배포본 ↔ 16d59fa 재빌드는 이름별 압축 payload도 모두 동일하지만, 디렉터리 포함 DOS 시각은 core 243개·web 7개 엔트리에서 다르고 external_attr는 core 9개·web 1개에서 다르다. core는 엔트리 순서도 다르다(web 순서는 같다). 따라서 전체 jar SHA 불일치는 **시각만의 차이라고 주장하지 않으며**, 시각·속성·순서 등 포장 차이와 class·리소스 내용 동일성을 구분한다.

## 출처 판정 범위·확인하지 못함

- 확보한 공급본의 core 내용은 두 후보 중 16d59fa와 일치하고 390c768과 불일치한다. web 내용은 두 후보가 같아 단독으로 출처를 구분할 수 없다. 영수증까지 합치면 **16d59fa 공급본이라는 증거가 부합**하나 내용이 같은 다른 커밋의 가능성까지 배제하는 빌드 attestation은 없다.
- 배포 tgz가 기존 배포 SHA와 일치하므로 brief의 불일치 조건에 따른 dev BO ECR 추출은 수행하지 않았다. 실제 실행 이미지의 jar는 미확인이다.
- brief에 적힌 이전 16d59fa 재빌드 `da81d383…` / `c665ba63…`의 전체 SHA·실물·원본 로그는 지정 입력에서 확보하지 못했다. 해당 생략값과 이번 결과의 동일성은 판정하지 않는다. 0817 재빌드의 전체 SHA와 내용 비교를 독립 증거로 제시한다.
- 과거 빌드의 정확한 JDK 패치·파일 속성·엔트리 순서는 영수증에 없으므로 과거 jar 전체 바이트 재현 조건까지 확정할 수 없다. 전체 회귀·교차 검토 및 배포 변경은 수행하지 않았다.

## 이슈대처 후보 (이전 해결 증거 재사용)

- 증상: 임시 사본의 `mvnw: Permission denied`.
- 원인: 제공 사본에서 복사한 wrapper의 직접 실행 권한 부재.
- 확인 명령: `bash .tmp-c1-1/16d59fabb82a/mvnw -version` → Maven 3.9.9 정상 실행. `team-lesson search "mvnw: Permission denied"`로 관련 증상 검색 후 조치했다.
- 조치·재발 방지: 입력 권한을 변경하지 않고 임시 사본에서 `bash mvnw`로 실행한다.

## 제출 범위

이번 변경은 `docs/reports/g-common-c1-1-sha.md` 한 파일뿐이다. 이전 patch 전체 적용 없이 새 깨끗한 사본에 문서만 작성했다. generated·jar·target·임시 소스·비밀값은 가져오지 않았다. 원격 patch의 실제 전달 결과는 후속 receipt 발행 전이므로 현재 로컬 변경 범위 확인과 구별한다.

교훈 적용: kb://core/ops/lessons/topic-9d939aa20431030e63ab6c76.md — brief에 지정된 증거 일치와 제출 경로 확인만 수행하고 별도 시험을 추가하지 않았다.
