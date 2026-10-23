# 프로그램 소유 UI 별칭

AC-30-T1-J1 · REQ-261023-09

`common-platform:0.6.11`부터 해석기는 프로그램 클래스패스의
`META-INF/ui-catalog/aliases.json`만 읽는다. 공통 jar의 `UiCodeAliases`는
기존 바이너리 호환용이며 새 해석기는 이를 참조하지 않는다.

```json
[{"system":"BO","kind":"ROUTE","value":"/program","target":"BO-NEW-001"}]
```

프로그램의 `src/main/resources/META-INF/ui-catalog/aliases.json`에 넣고 프로그램만
빌드·배포한다. 화면 추가 때문에 공통모듈을 다시 발행할 필요가 없다.
`system`은 APP/MEM/BO, `kind`는 ROUTE/BO_ID/INTERNAL_ID이며 APP은 LEGACY도
조회한다. `resolve`의 기존 version 인자는 바이너리 호환을 위해 유지하지만
프로그램 자원 조회 조건에는 사용하지 않는다. 버전 헤더는 기존처럼 로그에 남긴다.

자원은 context classloader별로 최초 사용 때 읽어 캐시한다. 변경 반영에는 프로그램
재시작이 필요하다. 여러 jar의 자원은 합치며 같은 키의 서로 다른 target이나 잘못된
JSON/행이 있으면 전체 설정을 무시한다. 진단은 프로세스당 최대 한 번이며 입력값이나
자원 내용은 로그에 쓰지 않는다. 자원 부재는 경고하지 않는다.

미등록 값은 기존 식별자 형식 `[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}`이면 그대로
통과한다. 빈 값·잘못된 헤더 형식은 빈 결과가 되며 요청을 거절하지 않는다.
등록된 route는 target으로 바꾸고 원래 alias를 기존 메타데이터에 남긴다.
미등록 route(`/...`)는 기존 식별자 형식에 맞지 않아 생략한다.
헤더 이름·UI element/action/event 형식과 로그 필드 매핑은 유지한다.

## 독립 생성기

`tools/ui-catalog/program_aliases.py` 파일 하나를 프로그램 저장소로 복사하거나
이 변경을 포함한 base-common 리비전에 고정해 가져온다. Python 3 표준 라이브러리만
사용하므로 공통 저장소·정본·Maven·다른 Python 모듈 없이 실행한다.

```sh
python3 program_aliases.py program-aliases.json src/main/resources
```

입력은 위 배열 또는 `aliases` 배열을 가진 객체다. 기존 정본 형식 입력에서는
RESOLVED 행만 사용하고 version/status를 제거한다. 프로그램별 한 버전을 선택해
입력한다. 충돌한 키는 생성 시 오류로 알려 주며, 이 선택 도구를 프로그램의 필수
파이프라인 게이트로 연결할 필요는 없다. 자원 자체가 없어도 런타임 동작은 유지된다.

## 발행과 소비

기존 GitHub Packages 수동 Maven deploy 절차를 lib 모듈에 한정한다.
parent/BOM/starter는 이미 발행된 0.6.10을 유지하고 lib만 새 버전으로 발행한다.
버전 존재 확인과 실제 발행 응답은 실행 결과 디렉터리에 기록한다.

소비 좌표: `ai.mutuus.common:common-platform:0.6.11`.
기존 GitHub Packages 저장소와 server ID `github` 인증을 사용한다.
BOM/starter 0.6.10 소비자는 이 라이브러리 의존성 버전을 명시적으로 0.6.11로
재정의한다. 되돌릴 때는 해당 재정의를 제거하거나 0.6.10을 지정한다.
발행물을 삭제하거나 기존 버전을 덮어쓰지 않는다.

2026-10-23 발행 완료: 사전 metadata 최신 0.6.10, 새 0.6.11 POM HTTP 404를
확인하고 `mvn -B -s "$TEAM_OUT_DIR/publish-settings.xml" -f lib/pom.xml -Pgithub -DskipTests deploy`를
1회 실행했다(BUILD SUCCESS). 발행 후 0.6.11 POM·JAR 모두 HTTP 200.
지정 시험은 `mvn -B -f lib/pom.xml -Dtest=UiCodeResolverProgramResourceTest test`로
3건 실행, 실패 0·오류 0이다. 발행 단계에서는 같은 시험을 다시 실행하지 않았다.

- POM SHA-256: `141a32ad9f17cbdf7d0f869bc0a1c805045bdb612b89c74768900260cf4138ed`
- JAR SHA-256: `9b965a476642eca394ab635435e7add294d92fc7c06346d7d7f938baec184d3b`
- 되돌릴 게시 버전: `ai.mutuus.common:common-platform:0.6.10`
- 시험·발행 원문 근거: 실행 결과 디렉터리의 `resolver-test.log`, `published-metadata.xml`,
  `publish-preflight.txt`, `publish-command.txt`, `publish.log`, `publish-response.json`.

2026-10-23 후속 검증: 생성기 파일 하나만 저장소 밖 실행 결과 디렉터리로 복사해
`python3 -I program_aliases.py input.json src/main/resources`를 1회 실행했다.
작은 입력 `[{"system":"BO","kind":"ROUTE","value":"/program","target":"BO-NEW-001"}]`에서
`src/main/resources/META-INF/ui-catalog/aliases.json`이 생성되었고, 입력과 같은
4필드 배열 1행임을 확인했다(exit 0, 표준 출력·오류 없음). Python 격리 모드와
저장소 밖 cwd에서 실행하여 공통 저장소 모듈 없이 단독 실행 가능함을 확인했다.
이번 실행 결과 디렉터리의 `generator-verification.json`에 명령·출력·파일 해시를 남겼다.
기존 3사례 시험과 발행은 반복하지 않았다. 전체 시험·소비 프로그램 실행은 이번 범위 밖이다.
