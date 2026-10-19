# 요소 역할표 입력 계약 v1

현재 TAX 확정 역할 43개 중 locator 근거가 명시된 **28건**을 등록했다. 공용 내부/설명만 있는 locator 15건은 미등록이다. [원문 대조표](tax-role-map.md)를 소유팀이 검토한다. SYS 확정 8화면 130개 역할을 추가 등록했다. [SYS 소유팀 검토표](sys-role-map.md)에 원문·코드·gate·제외 범위를 보존한다. 생성기 준비는 영역 T2 완료가 아니다. `tools/ui-catalog/test_elements.py`의 SYNTHETIC 역할과 locator는 검증 전용이며 정본 반입 대상이 아니다.

영역팀은 UTF-8 JSON으로 다음 필드를 제공한다. 모든 필드는 필수이고 추가 필드는 거절한다. 아래 타입 표는 실제 역할표가 아니다.

| 위치 | 값 |
| --- | --- |
| schemaVersion | 정수 1 |
| status | CONFIRMED (영역팀 확정표만 허용) |
| roles | 비어 있지 않은 행 배열 |
| roles[].screen | 기존 표준 화면코드 정확히 일치 |
| roles[].role | 확정 동작역할, `[A-Z][A-Z0-9]*(?:-[A-Z][A-Z0-9]*)*` |
| roles[].locator | 근거 locator 문자열, 빈값/앞뒤 공백 금지 |
| roles[].evidence | repository, revision(소문자 Git SHA 40자리), path(저장소 상대경로) |

코드는 화면코드 + `-` + 역할명으로 생성한다(최대 64자). 대상 ID, 개인정보, 검색어, 런타임 입력값은 역할명에 넣지 않는다. locator는 근거로만 보존하고 코드 계산에는 쓰지 않는다. 역할명은 trim/대소문자변환/동의어 추측 없이 받는다. 의미가 모호한 역할은 영역팀 확정 전 반입하지 않는다. CONFIRMED는 제출자의 확정 선언이며 생성기가 외부 승인·Git revision 존재·locator 유효성까지 검증하지는 않는다.

같은 화면·동작역할은 대상 ID나 locator 수만큼 나누지 않고 하나의 역할 행으로 제출한다. 중복코드와 같은 화면·역할의 서로 다른 locator/근거는 거절한다. 기존 역할 재입력도 명시 거절하며 갱신은 별도 검토한다. 미등록 화면·잘못된 역할·빈 필드도 거절한다. 이 계약에는 역할 어휘 목록을 임의로 만들지 않았다.

## 입력 → 정본 → 생성 → 반입

```sh
# 1. 영역팀 확정 역할표를 검토한 뒤 새 버전을 명시해 변환한다.
python3 tools/ui-catalog/import_roles.py --roles /path/to/confirmed.json \
  --version 1.0.0-bo.3 --output /path/to/catalog.next.json
# 2. diff 승인 후 catalog.next.json을 ui-catalog/catalog.json으로 등록한다.
# 3. 필요한 한 번만 전체 생성한다.
python3 tools/ui-catalog/generate.py
# 4. P00 통합 뒤 GS-BO 팀의 별도 반입 작업으로 TS를 가져간다.
```

importer는 지정 output에만 쓰고 생성은 하지 않는다. 오류면 output을 쓰지 않는다. 기존 화면·메뉴·별칭 식별자와 I-10/home 모호 계약은 유지한다. 새 버전에 맞춰 별칭 version만 갱신한다. 정본 elements 행은 code/screen/role/locator/evidence/introducedVersion을 보존한다. v2는 rawRole/sourceScreen/actionToken도 보존한다. 기존 요소의 도입 버전은 후속 입력에도 유지한다. digest는 elements까지 code 정렬해 기존 canonical SHA-256 규칙으로 계산한다.

TS는 ELEMENTS, ELEMENT_CODES, 화면별 SCREEN_ELEMENTS 및 ElementCode를 출력한다. Java에는 기존 별칭표와 새 version/digest를 출력한다. SQL에는 ui_element INSERT와 locator/evidence_json 근거를 출력하며 DB 실행은 하지 않는다. SQL 과거 버전 FK 행 보존·migration은 B3 검토 대상이다.

## 후속 반입 필요 목록

- `generated/ui-catalog/bo-catalog.ts`: GS-BO 소비본에 별도 반입. 생성물 고정 hash/version 검사기 및 fixture 기대값 갱신 필요(실제 경로는 이번 쓰기 구역 밖이므로 미확인).
- `generated/ui-catalog/UiCodeAliases.java` → `lib/src/main/java/ai/mutuus/common/uicatalog/UiCodeAliases.java`: 바이트 동일 반입 필요. 이 작업에서는 런타임 Java를 고치지 않는다.
- `generated/ui-catalog/catalog.sql`: B3 계약 검토 입력. 적용·DB 실행 없음.
- `docs/ui-catalog/bo-tracing.md`: 기존 런타임 소비본의 bo.1 설명은 아직 유효하다. 소비본 반입 시 새 version/digest와 요소 목록 지원 범위를 함께 갱신한다.

확인하지 못함: SYS 생성 코드에 대한 소유팀 후속 대조 승인, locator 실동작, GS-BO 고정해시 검사기 경로와 제품 반입. 네트워크·제품 시험·전체 회귀는 수행하지 않는다.

## 명시 원문 대조 계약 v2 (TAX)

`ui-catalog/inputs/tax-confirmed.json`이 최초 실제 입력이다. v1은 그대로 지원한다.
v2는 schemaVersion=2이며 각 행에 rawRole/sourceScreen/actionToken을 반드시 더한다.
예: `<X-01.open-tax-list>` / `X-01` / `open-tax-list` / `OPEN-TAX-LIST` → `BO-TAX-010-OPEN-TAX-LIST`.
괄호와 화면접두는 rawRole에서 정확히 분리해 별도 보존하고, role은 명시적으로 공급한다.
importer는 actionToken이 소문자 ASCII hyphen 문법인지, role이 해당 token의 대문자와 정확히 같은지 검사한다.
다른 표기·공백·동의어·구두점은 변환하지 않고 거절한다. sourceScreen은 정본 businessNumber와 일치해야 한다.
동일 rawRole의 중복/locator 충돌, 동일 화면·suffix 충돌은 거절한다. 실제 대조표에 없는 역할을 자동 추출/등록하지 않는다.

문서 스냅샷 근거는 evidence의 기존 repository/revision/path에 contentSha256/revisionKind를 함께 추가할 수 있다.
revisionKind=WORKTREE_BASE는 문서 작업 사본의 base HEAD이며 통합 revision 확정 주장이 아니다.
contentSha256은 실제 읽은 문서 바이트를 고정한다. 제품 기준 revision과 확정 메시지는 대조표에 별도로 기록했다.
SQL evidence_json에도 원문 대조 필드를 포함해 원문을 보존한다. locator 원문은 실행 CSS로 변환하지 않는다.

```sh
# 최초 bo.2 → bo.3 등록에 사용한 변환 계약 (이미 등록된 정본에 재실행하면 충돌 거절)
python3 tools/ui-catalog/import_roles.py --roles ui-catalog/inputs/tax-confirmed.json \
  --version 1.0.0-bo.3 --output /path/to/catalog.next.json
```

이번 최초 수용 검증은 `tools/ui-catalog/verify_tax.py`로 정상 등록·대조표와 5개 거절 사례·생성 정합을 1회 확인했다.
이 스크립트는 bo.2를 입력으로 하는 일회성 검증이며 bo.3 정본에는 재실행하지 않는다.
생성은 1회, 기존 합성7건/기존B1/SQL/common/web 시험은 0회다.

## SYS 확정 입력 v3 · bo.4

`ui-catalog/inputs/sys-confirmed.json`은 조직 d9051c7의 05-roles.md 130개 행과 1:1 대응한다. v1/v2도 유지한다. v3은 v2 필드에 `behavior`를 필수로 더한다. behavior의 필수 문자열은 label/trigger/kind/gate/notes/productRevision(제품 SHA 40자리)이며 추가 키·빈값은 거절한다. 원문의 `Y-10.refresh` 표기를 rawRole에 그대로 저장하며, 기존 TAX의 `<X-01.open-tax-list>` 표기도 그대로 유지한다. sourceScreen/actionToken/role과 원문의 어휘가 정확히 일치해야 한다. trim·동의어 추측은 없다. actionToken의 ASCII 대문자 suffix 대조만 기존 계약대로 명시한다. 모든 실제 대응은 sys-role-map.md에서 소유팀이 검토할 수 있다.

근거 revisionKind=GIT_COMMIT은 조직 커밋에 들어 있는 문서 바이트의 contentSha256을 가리킨다. WORKTREE_BASE도 하위 호환한다. SYS 문구·trigger·gate·notes·제품 revision은 정본 및 TS ELEMENTS, SQL evidence_json에 보존한다. 공통 반복 정책과 M70/S80 정의, 제외 사유는 sys-role-map.md에 확정 원문 그대로 보존한다. locator는 소스 위치/요소 설명이며 실행 CSS나 새 testid를 발명하지 않는다.

TAX는 조직 f2eb1a1 문서와 전 입력 b1e07bb trace-inventory로 근거를 갱신했다. 28등록/15제외와 코드·locator는 유지한다. SYS는 확정 분모 130/130 등록, 분모 안 미확정 제외 0이며 Y-30·도달 불가 legacy·무핸들러 준비 기능은 원문 정책대로 분모 밖이다. 정의되지 않은 제외 기능 수를 임의로 산출하지 않는다. 등록은 런타임/서버 gate 성공이나 영역 T2 완료를 뜻하지 않는다.

```sh
# TAX bo.3 정본에서 최초 1회만 실행한 SYS 반입 명령
python3 tools/ui-catalog/import_roles.py --roles ui-catalog/inputs/sys-confirmed.json \
  --version 1.0.0-bo.4 --output /path/to/catalog.next.json
# 승인된 next 정본 등록 후 생성 1회; GS-BO TS 반입은 별도 작업
python3 tools/ui-catalog/generate.py
```

이번 실제 입력 수용은 verify_sys.py로 한 번만 수행한다. 이전 TAX·합성·common·웹·SQL 시험은 재실행하지 않는다. common Java 바이트 반입과 고정 hash/version 검사기 갱신, GS-BO TS 소비본·검사기·fixture 갱신이 후속이다. 실제 제품 검사기 경로는 쓰기 범위 밖이라 확인하지 않았다.
