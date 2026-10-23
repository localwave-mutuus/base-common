# 개발자 PC common-platform 0.6.9 온보딩

REQ-261023-02 · AC-23-T1-J1 · 기준 base-common `7325911`.
이 묶음은 패키지 받기·로컬 빌드/시험·업그레이드 안내다. GitLab 시험과 권한·결과 경로는 A06 회신을 반영한 [gitlab-ci-server-test.md](gitlab-ci-server-test.md)를 따른다. 현재 전달본은 [common-platform-onboarding-0.6.9.zip](dist/common-platform-onboarding-0.6.9.zip)이다. dist를 제외한 onboarding 전체를 포함한다.

## 시작 순서

1. 계정·토큰: 아래 준비와 권한을 확인한다.
2. settings: PC별 설정의 환경변수와 [템플릿](settings.xml.template)을 준비한다.
3. 0.6.9 받기 확인: 아래 OS별 스크립트로 POM 수신을 확인한다.
4. 공통모듈 적용: [스타터·BOM](usage/02-starters-bom.md)부터 적용한다.
5. 로컬·GitLab 서버 시험: 아래 제품 빌드와 [서버 시험 안내](gitlab-ci-server-test.md)를 따른다.
6. 이후 업그레이드: [판 변경·캐시·되돌리기](upgrade.md)를 따른다.
7. 문제 해결: [오류 대처](troubleshooting.md)를 따른다.

## 사용법 요약

각 문서는 언제 쓰나·의존 아티팩트·설정 키·최소 예제·확인 방법과 정본 링크를 담는다.

- [전체 개요·기존 온보딩](usage/01-overview.md)
- [스타터·BOM](usage/02-starters-bom.md)
- [API 응답](usage/03-api-response.md)
- [로그 형식·로깅](usage/04-logging.md)
- [추적 상관키](usage/05-tracing.md)
- [오류 코드·업무 error.code](usage/06-error-codes.md)
- [화면·요소 코드·ui-catalog](usage/07-ui-catalog.md)
- [멱등성](usage/08-idempotency.md)
- [GitHub Packages 소비](usage/09-github-packages.md)

요약의 정본 링크는 저장소의 docs 기준 상대 경로다. zip 단독으로 받기·설정·시험 안내와 요약을 읽을 수 있으나 정본 전문은 같은 revision의 저장소를 함께 받아 docs/onboarding 위치에서 열어야 한다. 정본의 과거 판·게시처·로그 예시는 당시 기록이며 이번 받기 기준은 0.6.9와 GitHub Packages다. [J1 확인 기록](verification.md)은 과거 결과이며 이번 재실행 결과가 아니다.

## 준비와 권한

- Git, JDK 21, Maven 및 Bash(Linux/macOS) 또는 PowerShell(Windows)을 준비한다. Java 기준 근거: base-common `parent/pom.xml:28-29`. 터미널과 IDE의 JAVA_HOME/JDK도 맞춘다.
- GitHub 계정에 `localwave-mutuus/base-common` 및 패키지 읽기 권한이 필요하다. PAT(classic)의 최소 패키지 읽기 범위는 `read:packages`다. 조직 SSO를 쓰면 해당 토큰의 조직 사용 승인도 확인한다. 권한 변경은 이 묶음에서 수행하지 않는다.
- GitLab 소스 읽기/MR 권한과 GitHub 패키지 권한은 각각 필요하다. GitLab Developer 역할은 P00을 통해 그룹 관리자에게 요청한다. 서버 승격은 A06에 요청한다. 외부 개발자가 CI 변수를 설정하지 않는다.
- 토큰은 승인된 비밀 전달 경로로 받는다. 파일, 명령 인자, 셸 이력, MR, 로그에 값을 쓰지 않는다. 설정 템플릿에는 환경변수 참조만 있다. 디버그 옵션 `-X`와 환경변수 전체 출력은 피한다.
- 공식 인증 안내: [GitHub Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry). 서버와 저장소 ID는 모두 `github-common-platform`으로 일치해야 한다.

## PC별 설정

이 디렉터리를 PC에 복사한다. 템플릿을 `-s`로 직접 지정하므로 기존 사용자 설정은 덮어쓰지 않는다. 회사 mirror/proxy 설정이 필요하면 기존 설정의 관련 항목과 템플릿 프로필/서버를 개인 로컬 파일에서 병합하고 그 경로를 사용한다. 자격을 하드코딩하지 않는다.

Linux/macOS(Bash), 이 묶음 디렉터리에서:

```bash
read -r -p 'GitHub username: ' GS_MAVEN_READ_USER
read -r -s -p 'Package read credential: ' GS_MAVEN_READ_TOKEN; printf '\n'
export GS_MAVEN_READ_USER GS_MAVEN_READ_TOKEN
bash ./check-common-platform.sh ./settings.xml.template
```

Windows PowerShell, 이 묶음 디렉터리에서:

```powershell
$env:GS_MAVEN_READ_USER = Read-Host 'GitHub username'
$secret = Read-Host 'Package read credential' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try { $env:GS_MAVEN_READ_TOKEN = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
.\check-common-platform.ps1 -Settings .\settings.xml.template
```

PowerShell 스크립트 실행 정책이 제한되면 조직 승인 방법을 사용한다. 시스템 정책을 변경하지 않는다. 환경변수는 현재 프로세스와 자식 Maven에만 전달하며 세션 종료 시 제거한다. Bash는 `unset GS_MAVEN_READ_USER GS_MAVEN_READ_TOKEN`, PowerShell은 `Remove-Item Env:GS_MAVEN_READ_USER, Env:GS_MAVEN_READ_TOKEN`을 사용한다.

두 확인 스크립트는 동일한 Maven 좌표 `ai.mutuus.common:common-platform:0.6.9:pom`을 새 임시 저장소에 받는다. 기존 캐시가 성공처럼 보이는 것을 방지하며 종료 시 임시 저장소와 비출력 로그를 삭제한다. Maven 플러그인 받기를 위해 Maven Central 접속도 필요하다. PASS는 POM 수신만 뜻하며 JAR·전이 의존성·빌드·서버 동작을 보장하지 않는다.

## 공통모듈 적용

[스타터·BOM 요약](usage/02-starters-bom.md)의 BOM import와 용도에 맞는 스타터를 소비 서비스에 추가한다. 적용 후 API·로그·추적 등 필요한 주제 요약을 순서대로 확인한다.

## 제품 빌드와 로컬 시험

각 GS의 실제 BOM 위치와 버전 변경은 [upgrade.md](upgrade.md)를 따른다. 읽기 사본을 직접 수정하지 말고 개발자 개인 체크아웃에서 진행한다. `SETTINGS`는 위 템플릿 또는 병합한 설정의 **절대 경로**다. 제품 Maven 루트(pom.xml 위치)에서 Linux/macOS는 다음 순서로 실행한다.

```bash
./mvnw -s "$SETTINGS" -U -DskipTests package
./mvnw -s "$SETTINGS" test
./mvnw -s "$SETTINGS" dependency:tree -Dincludes=ai.mutuus.common
```

Windows에서는 동일 인자를 `mvnw.cmd`에 전달한다. wrapper가 없는 제품은 설치된 `mvn`으로 대체한다. 첫 명령은 JAR와 전이 의존성 받기 및 컴파일/패키징, 두 번째는 해당 제품 로컬 시험이다. 외부 서비스가 필요한 시험은 제품의 승인된 개발 환경 설정을 먼저 준비하고 운영 환경에 연결하지 않는다. 기동 프로필·로컬 서비스·스모크 경로는 제품별로 확인해야 하며 제공 입력만으로 확정하지 않았다. 제품 빌드/시험은 이번 산출물 작성에서 실행하지 않았다.

## 검증 범위

토큰 없는 effective-settings 성공은 이전 시도 결과를 재사용했다. 이번 POM 받기 결과는 verification.md를 참고한다. Windows 실행, 제품 빌드·시험, 서버 시험·배포는 수행하지 않았다. 실제 실행할 제품별 최소 시험은 [GitLab 안내](gitlab-ci-server-test.md)에 있다.
