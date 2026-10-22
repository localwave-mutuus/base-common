# UI 카탈로그 CI 검사

## 현재 검사 범위

`.github/workflows/ui-catalog-check.yml`은 main push·PR·수동 실행에서 오프라인 검사 단계 1개를 실행한다. 발행은 하지 않는다.

`PYTHONDONTWRITEBYTECODE=1 python3 tools/ui-catalog/generate.py --check`는 기존 `diagnose`, 요소 검사 `elements.validate`, 생성 함수 `render`를 재사용한다. 정본 `ui-catalog/catalog.json`을 읽어 다음 6개 내용을 메모리에서 생성하고 커밋된 파일과 바이트 단위로 비교한다. 검사 모드는 파일을 쓰지 않는다.

- `generated/ui-catalog/bo-catalog.ts`
- `generated/ui-catalog/UiCodeAliases.java`
- `generated/ui-catalog/catalog.sql` (`tools/ui-catalog/schema.sql` 포함)
- `generated/ui-catalog/diagnostics.json`
- `docs/ui-catalog/review.md`
- `docs/ui-catalog/app-alias-candidates.md`

추가로 생성 Java 내용과 `lib/src/main/java/ai/mutuus/common/uicatalog/UiCodeAliases.java`를 비교한다. `lib/pom.xml`의 명시적 라이브러리 버전을 `ui-catalog/version-map.json`에서 찾아 그 값과 catalogVersion을 비교한다. 표에 버전이 없거나 값이 다르면 실패한다. 현재 대응은 `0.6.9 → 1.0.0-ui.9`다. 다음 발행 때 대응표에 한 줄을 추가한다. 버전과 생성물은 이 정책 적용으로 변경하지 않는다.

화면 코드·요소 코드·별칭 복합키(system/kind/value/version)의 중복과 정본 screens에 없는 화면을 참조하는 요소는 기존 검사가 거절한다. 모호 별칭은 경고로 보존한다. 현재 안내된 APP 후보 53개와 BO 모호 별칭 1개를 합한 경고 수는 54개이며, 미사용 여부를 별도로 판정하는 분석은 없다.

## 기존 입력과 재생성 경계

현재 사본에 다음 입력 경로가 있다.

- `ui-catalog/inputs/app-input.json`: APP 화면·메뉴·별칭 반입 자료. 계약은 `docs/ui-catalog/app-input.md`에 있다.
- `ui-catalog/inputs/tax-confirmed.json`, `sys-confirmed.json`, `dsh-sup-confirmed.json`, `member-confirmed.json`, `iss-confirmed.json`: 기존 확정 역할표.
- `ui-catalog/inputs/dsh-sup-coverage.json`, `member-coverage.json`: 역할 대조·제외 근거 자료.

`tools/ui-catalog/import_roles.py`는 기존 catalog와 역할표를 받아 새 catalog 파일을 지정한 출력 경로에 만든다. `elements.import_roles`는 기존 화면을 유지하고 요소를 추가하며 새 버전을 요구한다. 이미 등록된 역할은 충돌로 거절하므로 현재 정본에 과거 입력을 순서대로 재실행하는 복원 경로가 아니다. `verify_tax.py`, `verify_sys.py`, `verify_dsh_sup.py`, `verify_member.py`, `verify_iss.py`, `verify_iss_close.py`는 과거 기준 정본·외부 출력 디렉터리의 자료를 전제로 한 일회성 수용 스크립트다. 이번에는 실행하지 않았다.

`tools/ui-catalog/generate.py`는 catalog.json을 입력으로 생성물만 만든다. 현재 확인한 도구에는 APP 반입 자료와 역할표만으로 전체 catalog.json을 처음부터 복원하는 경로가 없다. 따라서 이 CI는 **catalog.json 자체의 재생성 비교를 하지 않는다**. 제품 소스의 신규 미등록 코드 전수 검색도 하지 않으며, 정본 내부 참조와 inventory에 명시된 별칭 누락을 검사한다.

## 중복 실패 자료

`ui-catalog/fixtures/duplicate-screen.json`은 정본 screens 첫 행을 복사해 추가하는 변형 지시다. 이전 시도에서 성공한 중복 거절 시험은 재실행하지 않는다. P00 확정 정책을 적용한 현재 정본 통과 시험만 1회 수행한다.

## 기준과 확정 범위

작업 기준은 `6858e5d8e0cace8a2cf0bbf9e5a2a3776e723fc0`, 발행 기준은 `5cc807f8d0d82ff244bc461b7aec2952c20fee84`다. P00 갱신 20:0x 결정에 따라 버전 대응표를 검사하며 버전 올림·생성물 수정은 하지 않는다.

완료 비교 범위는 catalog.json에서 생성 Java/리소스까지 차이 0이다. 범위 제외: inputs→catalog.json 재생성(경로 없음, 후속 T9 잔여). 기초 스냅샷·TAX 갱신·APP 병합 절차는 이번 범위에 필요하지 않다.

이전 중복 시험의 `duplicate screens: BO-ISS-010` 거절 결과는 첨부 인계에서 재사용하며 재실행하지 않는다. 원격은 https://github.com/localwave-mutuus/base-common 이다. 이번 main SHA·CI URL·시험 출력은 실행 결과 보고서에 남긴다.
