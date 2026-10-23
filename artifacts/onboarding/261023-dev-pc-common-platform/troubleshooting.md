# 오류 대처

REQ-261023-02 · AC-23-T1-J1

확인 스크립트는 비밀 보호를 위해 Maven 원문 로그를 출력하지 않는다. 실패하면 아래 순서로 확인한다. PM에게는 오류 종류, 시각, OS/JDK/Maven 판, 대상 좌표와 설정 파일의 server/repository ID만 전달한다. 자격·환경변수 전체·settings 원문은 첨부하지 않는다.

| 증상 | 확인과 조치 |
|---|---|
| 환경변수 설정 필요 | README의 숨김 입력으로 현재 세션에 사용자/읽기 자격을 설정한다. IDE에서 실행한다면 해당 프로세스에도 환경변수가 전달되어야 한다. |
| Maven 또는 Java를 찾지 못함 | PATH와 JAVA_HOME, IDE JDK를 확인한다. Java 21 기준이다. |
| 401 / 인증 실패 | 자격 만료·사용자 불일치·변수 전달·server ID를 확인한다. `github-common-platform`이 repository ID와 일치해야 한다. 값을 출력하지 않는다. |
| 403 / 권한 거절 | 패키지/저장소 읽기, PAT(classic)의 `read:packages`, 조직 SSO 승인 여부를 담당자에게 확인 요청한다. 토큰을 게시하거나 임의로 권한을 확대하지 않는다. |
| 404 / artifact not found | 인증 실패도 존재 여부를 숨길 수 있다. 권한 확인 후 owner/repository와 `ai.mutuus.common:common-platform:0.6.9:pom` 좌표, 게시 완료 여부를 확인한다. |
| PKIX / TLS / 접속 시간 초과 | 승인된 네트워크·proxy·JDK 신뢰 저장소를 확인한다. TLS 검증을 끄지 않는다. Maven Central과 GitHub Packages 모두 필요하다. |
| POM 받기는 성공하나 제품 빌드 실패 | 확인 스크립트는 POM만 받는다. parent/BOM/JAR/전이 의존성, 제품 Java 및 Boot 기준을 확인한다. 제품 빌드에서 JAR와 전이 의존성을 받는다. |
| 이전 실패가 캐시됨 | 제품 빌드에 `-U`로 다시 조회한다. 필요 시 해당 좌표/버전 디렉터리만 별도 백업 후 정리한다. 전체 Maven 캐시는 삭제하지 않는다. 스크립트는 매번 새 캐시를 사용한다. |
| 다른 버전이 선택됨 | dependency tree에서 direct dependency의 명시 버전, 다른 BOM의 우선순위, parent/property/profile을 확인한다. 기대 판은 upgrade 문서 참조. |
| mirror가 패키지 저장소를 가로챔 | 사용자/전역 settings의 mirrorOf와 proxy 정책을 담당자에게 확인한다. 기존 설정을 덮어쓰지 말고 승인된 mirror 경로를 사용한다. |
| PowerShell 실행 차단 | 조직 승인 실행 방법을 문의한다. 시스템 실행 정책 변경을 안내하지 않는다. |
| 서버 시험 요청 | gitlab-ci-server-test.md를 따른다. local·웹 Run pipeline 경로는 없으며 dev push는 자동 배포를 수반한다. A06에 승격을 요청한다. |

서버 잡의 시험 범위·로그 경로는 gitlab-ci-server-test.md를 참고한다. 서비스별 성공 조건과 장애 재시도는 담당자에게 확인한다. 업그레이드 실패 시 [upgrade.md](upgrade.md)의 되돌리기를 따른다.
