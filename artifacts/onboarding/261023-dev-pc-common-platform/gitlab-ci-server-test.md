# GitLab 서버 시험과 로컬 대체 시험

REQ-261023-02 · AC-23-T1-J1. 근거: PM 첨부 `attach-1-REQ-261023-02-ci-facts.md` §1–7(A06, 2026-10-23 local 브랜치 읽기 조사). 아래 CI 근거 경로·행은 A06 인용이며 GitLab 설정을 재조회한 결과가 아니다.

## 실행 경로와 권한

local 파이프라인은 네 저장소 모두 꺼져 있다(`GS_LOCAL_PIPELINE_ENABLED=0`, A06 인용 CICD `docs/03-current/cicd-environments.md:43`). APP/BO local 잡은 폐기된 CodeDeploy 경로이고 MEM/BATCH에는 local 규칙이 없다. 웹 Run pipeline은 workflow 규칙상 생성되지 않는다(APP `.ci/gs-dev-auto.yml:8-21`, BO `.gitlab/gs-dev-auto.yml:8-20`, BATCH `.gitlab-ci.yml:9-18`). APP prod Java 21 베이스 이미지 발행 예외는 서버 시험과 무관하다.

dev 보호 브랜치 push의 `gs-package-dev`는 `gs-deploy-dev` 자동 실행으로 이어진다. 개발자가 시험 목적으로 push하거나 변수를 바꾸지 않는다. P00을 통해 A06에 같은 커밋의 local→dev 승격과 서버 시험을 요청한다. prod는 dev→prod MR merge 후 수동 잡이며 외부 개발자 범위 밖이다. prod merge는 Maintainer가 수행한다.

소스·잡 로그·artifact 열람용 Developer 역할을 P00→GitLab 그룹 관리자에게 요청한다. 실제 외부 계정 역할은 미확인이다. CI 변수는 Maintainer가 관리하며 외부 개발자가 넣을 변수는 없다. 러너 AWS 권한은 작업별 OIDC다. 빌드 전용 변수 이름만 보고 배포 생략을 가정해 실행하지 않는다.

## 로컬에서 수행할 최소 시험

먼저 README의 settings로 의존성을 온라인으로 받아 둔다. 아래 `-o`는 캐시가 준비된 뒤 사용한다. 최초 받기에 오프라인 옵션을 쓰지 않는다. 예시는 POSIX shell 기준이며 Windows는 Maven wrapper를 `mvnw.cmd`로 대체한다. Bash/Python 보조 스크립트는 승인된 Bash 환경에서 실행한다.

| 저장소 | 위치와 로컬 명령 | CI와의 관계·근거(A06 §2) |
|---|---|---|
| APP | 루트에서 `bash .ci/stub-guard.sh`, 이후 `src/golmok/apps/backend`에서 `./mvnw -o clean package` | CI `.ci/build-app-dev.sh:114`는 `-DskipTests -Dmaven.test.skip=true`로 단위 시험을 생략한다. 로컬 명령은 시험을 포함하므로 CI와 동일한 시험 실행이라고 주장하지 않는다. |
| BO | `apps/backend`에서 `./mvnw -o -q '-Dtest=RecruitmentAdminServiceTest,RecruitmentControllerTest' package` | `scripts/gs-ci-package.sh:31`의 최소 시험과 동일 |
| MEM | `src/member/apps/backend`에서 `./mvnw -q clean package -Dspring.profiles.active=dev -Dtest=MemberBusinessIdentityServiceTest,MemberBusinessIdentityControllerTest,AdminServiceAssertionTransportFilterScopeTest,DeviceLoginServiceTest` | `tools/gs-member-ci-build.sh:22`와 동일. 승인된 개발용 서비스 설정을 먼저 준비한다. |
| BATCH | 루트에서 `python3 .ci/test-batch-logging.py`, 이후 `mvn -B -ntp package` | `.ci/batch-package.sh:13-14`의 시험. 해당 스크립트 자체는 8-11행 CI push 환경 검사 때문에 로컬에서 직접 실행하지 않는다. |

APP CI는 `.gitlab-ci.yml`→`.ci/gs-dev-auto.yml`, BO는 `.gitlab-ci.yml`→`.gitlab/gs-dev-auto.yml`, MEM은 `.gitlab-ci.yml`→`.gitlab/ci/gs-dev-auto.yml`, BATCH는 `.gitlab-ci.yml` 직접 정의다. MEM의 CICD 문서 표기는 `.ci/gs-dev-auto.yml`로 다르며 실제 GitLab 프로젝트 설정 경로는 미확인이다.

## 패키지 캐시와 결과 확인

A06 §4에 따르면 CI에는 패키지 다운로드 변수가 없고 APP/BO는 오프라인 빌드한다. 러너는 A06이 시드한 Maven 캐시에 의존한다. 새 판은 A06이 GitHub Packages에서 받아 SHA 검증 뒤 러너·캐시 시드에 설치한다. 0.6.8·0.6.9 캐시 설치 여부는 미확인이다. PC 다운로드 성공을 CI 준비 완료로 해석하지 않는다. 근거는 A06 인용 CICD `docs/03-current/cicd-environments.md:90`, `docs/gcom/REQ-261021-02-runner-cache-064-install.md`이며 후자는 0.6.4 기록이다. base-common `parent/pom.xml:62-71`의 옛 배포 저장소는 PC 받기 설정으로 사용하지 않는다.

GitLab.com `localwave-group/<프로젝트>` → Build → Pipelines → 해당 커밋 → `gs-package-dev` 잡 로그와 Job artifacts에서 확인한다. 네 저장소 모두 JUnit 리포트는 없으며 시험 결과는 잡 로그로 판정한다. `build/gs-ci/` artifact는 1일 보관(MEM/BATCH는 Developer 접근)이다. MEM dotenv는 `build/gs-ci-producer.env`다. 실패 시 잡·커밋·좌표·비밀을 제거한 오류 종류를 남겨 A06에 전달한다.

배포 결과 artifact는 APP/BO `build/fargate/deployment.json`, MEM `build/gs-ci-transport/`, BATCH `build/batch-deploy/`다. package 성공과 실제 서버 판·서비스 정상 동작은 별도로 확인한다. 이 묶음을 작성하면서 파이프라인·서버 배포·제품 시험은 실행하지 않았다.
