# BO 코드 정본과 생성기

정본은 `ui-catalog/catalog.json` 한 벌이다. JSON은 Python 3 표준 라이브러리로 읽는다. Maven 의존성·빌드 설정·registry v4·제품 런타임은 바꾸지 않는다. 이 목록과 `review.md`는 사용자 검토 전 제안이다. GS-BO 반입과 계측은 B2, DB 계약 검토·실행은 B3다.

## 생성과 검증

저장소 루트에서 실행한다. 외부 접속·패키지 설치가 없다.

```sh
python3 tools/ui-catalog/generate.py
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests/ui-catalog -p 'test_*.py' -v
```

SQL 계약만 변경할 때는 `python3 tools/ui-catalog/generate.py --sql-only`로 SQL만 출력한다. 이 경로는 다른 출력의 생성·진단을 반복하지 않으며, 정본 변경에는 전체 생성 경로를 사용한다. SQL 계약 최소 시험만 실행하려면 `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests/ui-catalog -p test_sql_contract.py -v`를 쓴다.

생성기는 정본을 검사하고 TS·Java·SQL, 진단 JSON, 사용자 검토 목록을 함께 만든다. 오류가 있으면 종료 코드 1이며 파일을 쓰지 않는다. 알려진 `AMBIGUOUS`는 보고하고 보존하며, 유일 대상처럼 변환하지 않는다. 단위시험은 합성 fixture만 사용한다. 실제 정본을 재생성하지 않는다.

- `generated/ui-catalog/bo-catalog.ts`: 화면·메뉴·별칭 상수, ScreenCode/MenuCode/Alias 타입. 별칭은 target=null과 AMBIGUOUS 상태까지 전달한다. B2 소비자는 system/kind/value/version 전체와 RESOLVED 상태를 확인해야 한다.
- `generated/ui-catalog/UiCodeAliases.java`: Java 21용 별칭표. `resolve`는 네 키가 일치하는 유일 RESOLVED 행만 반환한다. 시스템 대응표는 `BACKOFFICE→BO`, `MEMBER→APP` 등을 제공하지만 자동 추측 변환은 하지 않는다.
- `generated/ui-catalog/catalog.sql`: PostgreSQL 스키마 **계약 제안과 스냅샷 INSERT 텍스트**. 기존 DB에서 실행하는 마이그레이션이 아니다. B3가 실제 스키마·버전 보존·migration 순서에 맞추어 반입한다.
- `generated/ui-catalog/diagnostics.json`, `docs/ui-catalog/review.md`: 같은 정본에서 생성한 수치·목록·미확정 항목.

모든 산출물에 catalogVersion, 정본 digest, 직접 수정 금지 표시가 있다. digest는 시스템/영역/화면/메뉴/별칭 레코드를 키로 정렬한 정본 전체를 UTF-8 JSON(sort_keys, 공백 없음, ensure_ascii=False)으로 직렬화한 SHA-256이다. 원본 JSON 파일 바이트의 SHA-256과는 다르다. 원자료 바이트 hash와 정확한 revision은 정본 sources에 있다. 시각·절대 작업 경로를 생성물에 넣지 않는다. 출력 순서와 문자열 escaping은 결정적이다. 생성기 자체는 정본 digest 대상이 아니므로 생성기 수정도 별도 diff 검토한다.

## 정본 필드와 상태

- `systems`: BO만 ACTIVE. APP(회원 포함), BAT는 RESERVED이며 화면·메뉴·별칭을 채우지 않는다. `systemMapping`은 이슈 DB의 기존 시스템 값을 유지하는 대응표다.
- `domains`: system, code, name, legacyMenuKey. 12개 메뉴 영역과 SHL 공용 영역. 별도 `{system}-SHELL` 공용 컨테이너 개념과 실제 SHL 화면번호를 구분한다.
- `screens`: code/system/domain/name/kind/route/businessNumber/implementation/owner/status/replacement/introducedVersion/retiredVersion. null은 주소 없음·미구현·대체 없음 등을 표현하며 근거 없는 경로를 만들지 않는다. IMPLEMENTED는 registry 등록 또는 App/SetupGate 구현 확인을 의미하며 E2E 성공을 뜻하지 않는다.
- `menus`: code/system/domain/parent/name/order/target/status. 현행 menu.ts는 평면 영역 목록이므로 parent=null, 영역은 domain FK로 표현한다. guide는 App.tsx의 별도 주소에 대응하는 보조 메뉴 제안이다. 메뉴 코드는 `BO-MENU-{영역}-{의미 항목}`이며 설계의 `BO-MENU-ISS-STATUS` 패턴을 따른다. 다른 항목 영문은 신규 정본 제안이므로 사용자 검토가 필요하다.
- `aliases`: system/kind/value/version/target/status/candidates/reason. 복합키는 system·kind·value·version이다. RESOLVED만 target이 있고 AMBIGUOUS는 null target과 사유가 있다. 후보는 증거 설명이며 선택 순서가 아니다.
- `inventory`: 소스에서 추출한 registry/menu/IssueCatalog ID와 shell 목록, 상위 기대값. 별도 검토용 정본을 두지 않고 여기에서 coverage 분모와 검토 목록을 만든다.

표준 화면번호는 영역 안 **메뉴 순서** 010부터 10 간격이다. R-10은 TRD, W-*는 WORK→WRK다. 로그인·guide·SetupGate는 BO-SHL-010/020/030이다. X-30/K-10/Z-02/Y-30은 PLANNED다. K-10의 대체 코드는 menu.ts의 supersededBy=K-01 근거로 BO-POL-010이다. SetupGate는 별도 주소가 없고 인증된 라우트 안에서 비밀번호 변경을 조건부 표시한다. `/setup` 같은 주소를 만들지 않는다.

## 수치와 미매핑

메뉴 실측 12영역·72항목, guide 포함 73항목이다. registry는 69개이며 I-10 1개가 메뉴에 없다. 메뉴에는 registry에 없는 4개가 있다. 따라서 registry∪menu=73, shell 3개 포함 원천 식별자는 **76**이다. I-10에 표준번호를 주지 않는 결정에 따라 표준 화면은 **75**다. 상위 `69+4+3=75` 해석은 산술상 맞지 않으며 원천 합집합과 표준화면을 구분해야 한다.

I-10은 IssueCatalog에도 있지만 menu에는 없다. registry의 issues 구현을 I-01/I-20과 공유하므로 LEGACY로 보존하고 두 후보를 보고한다. ScreenHost는 menu 미등록 ID에 404를 표시하므로 `/i-10`을 지원 라우트 별칭으로 발명하지 않는다.

별칭 분모에는 원천 BO ID 전체(I-10 포함), 메뉴별 기존 route, shell 내부 ID와 확인된 shell route를 넣는다. **보존 완전성 100%와 유일 해석률은 다른 수치**다. 모호한 I-10도 보존 분자와 전체 분모에 포함하고, uniqueResolved에서는 빠진다. 수치는 diagnostics.json에서 확인한다. 미구현 4개는 주소와 BO_ID 별칭이 모두 존재한다. 과거 이슈 backfill은 유일 해석만 허용하고 원래 이름을 보존해야 하며 이 작업은 SQL backfill을 생성하지 않는다.

## SQL 계약과 B3 경계

`tools/ui-catalog/schema.sql`은 생성기의 정적 계약 부분이다. system/domain 참조, 화면/메뉴/요소 PK, 메뉴→화면·요소→화면 FK, 대체 코드 FK, system·kind·value·catalog_version 별칭 UNIQUE, 조회 인덱스를 포함한다. P00 결정 `note01M59ESZRF662BPHFTPRQ680R5`에 따라 screen/menu/element는 각각 **screen_code / menu_code / element_code 단일 PK**다. 표준코드 정체성은 재사용하지 않으며 버전 수명은 introduced_version·retired_version 열로 기록한다. 화면의 도입·폐기 버전은 정본 값을 사용한다. 현재 메뉴는 최초 도입 목록이므로 도입 버전은 catalogVersion, 폐기 버전은 NULL로 출력한다. 이후 메뉴 수명 변경 시 정본 필드를 먼저 확장해야 한다. 버전별 화면·메뉴·요소 스냅샷 행을 복합 PK로 중복 보관하지 않는다. 별칭·메뉴의 system이 대상 화면 system과 같은지도 FK로 강제한다.

환경별 활성 버전은 ui_catalog_activation(environment PK, catalog_version FK)로 분리한 제안이며 행은 생성하지 않는다. 적용시각도 채우지 않는다. 기존 admin.issue_codes·issue 표는 건드리지 않는다. ui_element는 요소 역할표 계약의 locator/evidence_json을 보존하며 현재 TAX 확정 locator 역할 28건을 등록했다(15건 locator 확인 대기). 요소명·클릭 코드는 추정하지 않는다. 동작 5종 CHECK는 실제 action 열이 도입되는 B2 이후에 적용한다. 애플리케이션 계정 SELECT 전용 원칙만 명시하며 실제 계정·GRANT·권한은 B3가 확인한다. 이 텍스트는 DB에 접속하거나 적용한 적이 없다.

## 갱신과 되돌리기

1. 원자료 revision/hash를 고정하고 변경 ID·주소·구현 위치·파일 소유 대장을 확인한다. 소스가 바뀌면 sources와 inventory도 같이 갱신한다.
2. 기존 코드의 정체성은 유지한다. 이름·주소 변경으로 재번호를 주지 않는다. 새 항목은 사용하지 않은 번호를 배정한다. 현재 정렬은 최초 배정 근거일 뿐, 메뉴 재정렬 때 기존 코드를 다시 계산하면 안 된다.
3. 폐기는 삭제·재사용 대신 RETIRED, retiredVersion, replacement로 남긴다. 별칭도 과거 버전에서 제거하지 않는다. 모호값은 근거가 생기기 전 단일화하지 않는다. catalogVersion을 올리고 생성물 diff·diagnostics·사용자 목록을 함께 검토한다.
4. B6가 후속으로 버전 변경/과거 코드 재사용/생성물 diff CI를 맡는다. 이 생성기는 이전 Git revision을 자동 조회하거나 버전 상승을 판정하지 않는다.
5. 이 단계 되돌리기는 정본·생성기·생성물·문서를 같은 승인된 이전 revision으로 되돌리는 patch다. B2 소비본은 승인된 이전 버전에 고정한다. 향후 DB 되돌리기는 B3가 활성 버전을 이전 값으로 돌리며 과거 코드와 도입·폐기 기록을 삭제하지 않는다. 여기에서는 Git 커밋·제품 반입·DB 실행을 하지 않는다.

## 요소 입력

확정 역할표 importer·정규화 거절·후속 반입 경계는 [element-input.md](element-input.md)를 따른다. 현재 정본은 1.0.0-bo.3이며 TAX 요소 28건을 등록했다. 화면별 분모·미등록 사유·원문 대조는 [tax-role-map.md](tax-role-map.md)를 따른다.
