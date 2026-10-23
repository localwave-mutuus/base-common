# common-platform 0.6.9 업그레이드 안내

읽기 revision: base-common `7325911`, GS-BO `da0cc35f1090`, GS-APP `ef61ede4ca1c`, GS-MEM `f2ddaa3ee4f3`, GS-BATCH `3033ce0dfc7a`.

2026-10-23 기준 POM, 0.6.9 관련 실제 소스 및 변경 이력을 읽어 작성했다. 아래 GS 경로는 제공된 읽기 사본 내 저장소 상대 경로다. 실제 개발 브랜치의 판은 적용 직전에 다시 확인한다. 이 문서 작성 중 소비 저장소 수정, 의존성 다운로드, 빌드 및 테스트는 실행하지 않았다.

## 판과 변경 근거

목표 좌표는 `ai.mutuus.common:common-platform-bom:0.6.9`이다. 공통 저장소의 `bom/pom.xml:28-31`이 BOM 좌표와 판, `lib/pom.xml:14-17`이 라이브러리 0.6.9를 선언한다. `pom.xml:20-23`의 aggregator도 0.6.9지만 게시 대상은 아니다(`pom.xml:8-18`, `pom.xml:29-30`). 소비 서비스에 aggregator를 추가하지 않는다.

| 소비 저장소 | BOM import 근거 | 읽기 사본 현재판 → 목표 | 처리 |
| --- | --- | --- | --- |
| GS-BO | `GS-BO/apps/backend/pom.xml:29-34` | 0.6.8 → 0.6.9 | 32행 버전 변경 |
| GS-APP | `GS-APP/src/golmok/apps/backend/pom.xml:30-35` | 0.6.9 → 0.6.9 | 판 유지, 실제 해석·배포 판 검증 |
| GS-MEM | `GS-MEM/src/member/apps/backend/pom.xml:30-35` | 0.6.8 → 0.6.9 | 33행 버전 변경 |
| GS-BATCH | `GS-BATCH/batch-app/pom.xml:14-19` | 0.6.6 → 0.6.9 | 17행 버전 변경 |
| GS-BATCH | `GS-BATCH/batch-member/pom.xml:14-19` | 0.6.6 → 0.6.9 | 17행 버전 변경 |
| GS-BATCH | `GS-BATCH/batch-common/runtime/pom.xml:5-7` | 0.6.6 → 0.6.9 | 6행 버전 변경 |

A06 회신은 10-23 local 브랜치 조사로 APP 0.6.9, BO/MEM 0.6.8, BATCH 0.6.6을 보고했다. 제공 읽기 revision의 위 값과 일치한다. A06 표에는 BATCH batch-member가 생략되어 있으나 읽기 사본에는 import가 존재하므로 세 곳 모두 확인한다. 현재 원격 브랜치나 러너 적용 판을 뜻하지 않는다.

GS-BATCH는 루트 한 곳의 변경으로 끝나지 않는다. 루트 모듈 목록은 `GS-BATCH/pom.xml:7`이고, POM 검색에서 BOM 직접 import가 확인된 곳은 위 세 곳이다. batch-app/member는 web starter를 사용한다(`GS-BATCH/batch-app/pom.xml:36-39`, `GS-BATCH/batch-member/pom.xml:36-39`). runtime은 batch starter를 사용한다(`GS-BATCH/batch-common/runtime/pom.xml:13`). 이번 판 변경과 starter 교체를 섞지 않는다.

0.6.9 변경 이력은 `5cc807f8d`의 제목 “Add APP canonical aliases and publish v0.6.9”로 확인된다. `git diff 5cc807f8d^2 5cc807f8d -- pom.xml bom/pom.xml lib/pom.xml`에서 확인되는 POM 변경은 다음과 같다.

| 대상 | 0.6.8 대비 0.6.9 POM 변경 근거 |
| --- | --- |
| aggregator | `pom.xml:22` 버전 정렬 |
| BOM | `bom/pom.xml:10` parent, `bom/pom.xml:24` 사용 예, `bom/pom.xml:30` BOM 자체 버전 정렬 |
| library | `lib/pom.xml:10` parent, `lib/pom.xml:16` 라이브러리 자체 버전 정렬 |

0.6.9 기능 변경은 `5cc807f8d`의 변경 목록과 실제 소스에서 다음과 같이 확인했다.

- `lib/src/main/java/ai/mutuus/common/core/UiCodeResolver.java:24-27`: `screen`이 ROUTE/BO_ID/INTERNAL_ID로 먼저 조회한 뒤, APP이면 LEGACY 별칭도 조회하도록 추가되었다. 그 다음 기존 safeIdentifier fallback을 수행한다.
- `lib/src/main/java/ai/mutuus/common/uicatalog/UiCodeAliases.java:10`의 카탈로그 판은 `1.0.0-ui.9`다. 같은 파일 `:15`에는 APP INTERNAL_ID `GM-SCR-000001` → `APP-AUT-010`, `:171`에는 APP LEGACY `A.1.a` → `APP-AUT-010`의 RESOLVED 별칭이 등록되어 있다. 이는 등록 예시이며 모든 입력에 대한 변환 보장이 아니다.
- `tools/ui-catalog/generate.py:160-161`: Java 별칭 생성 시 APP은 RESOLVED 항목만 포함하고, 조회는 system/kind/value/version이 모두 일치하는 단일 RESOLVED 항목의 target만 반환한다. 모호한 APP 별칭을 임의의 canonical 코드로 추정하지 않는다.

라이브러리 릴리스 판 `0.6.9`와 UI 카탈로그 판 `1.0.0-ui.9`는 별개다. 위 소스 확인은 소비 서비스의 실제 동작 시험 결과를 뜻하지 않는다. 0.6.6에서 올라가는 GS-BATCH에는 중간 릴리스도 검토 대상이다. POM 변경 이력 제목상 `2b12a759f`는 0.6.7 screen metadata/registry contract, `10bd67ddf`는 0.6.8 T4 authentication/T5 business error codes 릴리스다. 제목은 변경 범위의 단서이며 소비 서비스 호환성 검증 결과가 아니다.

## 호환 기대와 적용 범위

BOM은 common-platform 및 web/batch starter의 판을 `${project.version}`으로 함께 관리한다(`bom/pom.xml:57-71`). 따라서 개별 starter 버전을 추가하지 않고 BOM import를 올리면 0.6.9로 정렬될 것으로 기대한다. 기존 직접 버전 선언이나 다른 BOM과의 우선순위가 있으면 실제 결과가 달라질 수 있으므로 effective POM과 dependency tree로 확인한다.

읽기 사본의 GS-BO·APP·MEM은 Spring Boot 4.1.0(`각 backend/pom.xml:8-12`)과 Java 21(`각 backend/pom.xml:22`)을 선언한다. GS-BATCH도 Boot 4.1.0/Java 21이다(`GS-BATCH/pom.xml:4-6`). 공통 라이브러리는 parent 속성을 상속한다(`lib/pom.xml:21-23`). `parent/pom.xml:28-30`에서 Java 21, compiler release 21, UTF-8을 확인했다. `parent/pom.xml:35-38`의 관리 판은 OpenTelemetry 1.62.0, logstash encoder 8.0, springdoc 3.0.3, Spring Modulith 2.1.0이다. Java 선언은 소비 서비스와 같지만 이것만으로 바이너리/API 호환성을 보장할 수 없다. 공통 라이브러리에는 optional 의존성도 있으므로(`lib/pom.xml:49-89`, `lib/pom.xml:130-150`) BOM 상향만으로 모든 기능이 활성화된다고 가정하지 않는다.

## 받기 → 서버 시험 → MR

1. 소비 저장소의 최신 기준 브랜치를 받고 별도 업그레이드 브랜치를 만든다. 시작 커밋, 실제 BOM 현재판, 직전 정상 배포 산출물 식별자를 남긴다. GS-APP처럼 이미 0.6.9라면 판을 다시 바꾸지 않는다.
2. 위 표의 BOM import 버전만 0.6.9로 맞춘다. `groupId`, `artifactId`, `type=pom`, `scope=import`는 유지한다. 예시는 `bom/pom.xml:21-26`이다. GS-BATCH 세 import를 빠짐없이 확인한다.
3. 승인된 Maven settings와 저장소 구성을 사용해 각 소비 모듈에서 의존성을 받는다. 아래 명령은 실행 예이며, 이 문서 작성 시 실행한 결과가 아니다. 저장소가 제공하는 Maven 실행기를 사용한다.

   ```sh
   SETTINGS="$HOME/.m2/settings.xml"
   test -f "$SETTINGS" || exit 1
   mvn -s "$SETTINGS" -U dependency:resolve
   mvn -s "$SETTINGS" help:effective-pom -Doutput=effective-pom.xml
   mvn -s "$SETTINGS" dependency:tree -Dincludes=ai.mutuus.common
   ```

   `SETTINGS`는 홈 디렉터리에서 확장되는 절대경로다. 승인된 settings가 다른 곳에 있으면 실제 절대경로로 지정한다. 파일 존재 확인 실패 시 해당 실행을 멈춘다.

   GS-BATCH는 루트에서 대상 모듈을 지정하거나 각 POM 기준으로 확인한다. 다운로드 실패는 설정·권한·게시 상태를 구분해 확인한다. 자격 증명이나 settings 내용은 MR 및 공유 로그에 붙이지 않는다. 의존성 해석 성공은 서버 동작 성공과 별도다.
4. [gitlab-ci-server-test.md](gitlab-ci-server-test.md)의 로컬 최소 시험을 수행하고 결과를 남긴다. local 및 웹 Run pipeline 경로는 없다. dev 보호 브랜치 push는 자동 배포로 이어지므로 직접 실행하지 않고 P00을 통해 A06에 서버 승격을 요청한다. A06은 목표 판의 러너 캐시 설치 여부를 먼저 확인해야 한다. 서버 시험 완료 여부는 A06 잡 로그·산출물 및 서비스별 핵심 흐름 결과로 판단한다. 인증/오류 응답, 로그·추적 계약과 BATCH 작업 실행·종료 결과는 서비스 담당자와 확인한다.
5. 서버 시험 결과를 받은 뒤 MR에 기존판→0.6.9, 수정 POM, 해석된 공통 의존성 판, 시험 산출물/결과, 남은 문제, 되돌리기 기준을 기록한다. GS-APP은 이미 목표판임과 검증 결과를 기록한다. 이 안내 작성은 소비 저장소 MR 생성·병합이나 서버 배포 실행을 의미하지 않는다.

## 캐시와 적용 판 확인

- 소스 선언: 실제 브랜치의 위 POM import가 0.6.9인지 확인한다. aggregator 판만 확인해서 소비 판을 판단하지 않는다.
- Maven 해석: effective POM에서 BOM 및 직접 버전 덮어쓰기를 확인하고, dependency tree에서 실제 사용되는 `ai.mutuus.common` 라이브러리·starter가 0.6.9인지 확인한다. 사용하지 않는 starter가 없어도 실패는 아니다.
- 패키지: 새 산출물 내부의 공통 JAR 판과 빌드 커밋을 확인한다. 배포 시 산출물 식별자와 실행 인스턴스의 식별자를 대조해 이전 프로세스나 이전 이미지가 남지 않았는지 확인한다. POM 선언만으로 실행 서버의 적용 판을 확정하지 않는다.
- 다운로드 캐시: 먼저 `-U`로 실패 캐시를 갱신한다. 계속 실패하면 해당 좌표의 실패 기록이나 손상 파일만 원인을 확인해 다시 받는다. 전체 로컬 Maven 저장소 삭제를 기본 절차로 삼지 않는다. 0.6.9는 고정 릴리스 좌표이므로 같은 판의 재게시를 해결책으로 가정하지 않는다.
- 빌드 캐시: dependency tree는 맞지만 패키지에 이전 JAR가 있으면 기존 산출물과 빌드/이미지 캐시를 확인하고 서비스 절차에 따라 새 산출물을 만든다. 애플리케이션의 Redis 등 업무 캐시 삭제는 라이브러리 다운로드 캐시 조치에 포함하지 않는다.

## 되돌리기

서버 시험 실패 또는 적용 후 핵심 흐름 회귀 시 확대 적용을 멈추고 결과를 남긴다. 변경 커밋을 되돌려 원래 BOM으로 재빌드하거나, 확인된 직전 정상 산출물을 서비스 배포 절차로 복구한다. 문서 조사 기준 원래판은 BO/MEM 0.6.8, BATCH 세 곳 0.6.6이며 실제 시작 기록을 우선한다. APP의 현재 0.6.9를 임의로 0.6.8로 내리지 않고 직전 정상 산출물 기록에 따른다.

복구 후에도 dependency tree·패키지·실행 산출물의 판을 다시 대조하고 핵심 흐름을 재확인한다. POM 되돌리기만으로 이미 실행 중인 서버가 바뀌지는 않는다. 데이터 또는 설정의 변경·복구 필요 여부는 POM 조사로 확인하지 못했으므로 A06 및 서비스 담당자의 시험·배포 결과로 판단한다.

## 확인하지 못한 항목

POM 받기는 verification.md를 참고한다. 실제 소비 브랜치 최신판, ABI/API 호환성, 서버 기동·시험·배포 판, 러너 캐시 0.6.8·0.6.9 설치, 외부 계정 역할 및 MEM의 GitLab CI 설정 경로는 확인하지 않았다. 이 문서는 지정 POM·관련 소스와 변경 이력을 근거로 한 적용 안내이며 테스트 통과 보고서가 아니다.
