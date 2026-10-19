# DSH/SUP 소유팀 검토표: 원문 역할 ↔ 명시 코드

A08 소유. 조직 확정 revision `f7c3c019f10ea2de27b7ce1138b267fe8f1b8eb7`(brief 승인 근거), 원문 SHA-256 `133b20b4a40b25d0fba7890ff6136289ec0d7a44be0d508ab465ee05df57c471`.
제품 통합 기준 `aa2a4e1ce92a6f0f277a04f25bbfc83ab8e0fde0`, 제품 patch SHA-256 `7b469a5ce6000962ca61b9b96b018c013b79fa7de993b74dd3221dfad8a0ba2d`. 원문 정적 읽기 제품은 `aa0d4f9c65fb4306d09f58ad08e159683d4b40ca`이며 통합 제품과 혼동하지 않는다.
원문의 PM 검증/통합 대기 문구는 과거 상태로 보존한다. 이번 등록 권한은 brief가 지정한 f7c3c01 확정 정본이다. 소유팀 코드 대조 승인은 미수신이며 아래 표는 검토용이다.

누적 기준: base-common `88a683909e87b7e75f159319772d7dd0cac2482c` + 1755.a04 누적 patch 최종 blob, `1.0.0-bo.4`, SYS130/TAX28. 이번 version `1.0.0-bo.5`, 총 요소176.

SUP는 작업 영역 이름이다. 기존 S-01/S-10 화면의 정본 귀속은 BO-CNT-010/020이므로 BO-SUP 코드를 새로 만들지 않는다. locator는 원문 근거 문자열이며 실행 CSS 변환·공용 내부 selector 생성은 하지 않는다.

| 화면 | 분모 | 등록 | 미등록 |
|---|---:|---:|---:|
| D-01 / BO-DSH-010 | 3 | 1 | 2 |
| S-01 / BO-CNT-010 | 18 | 10 | 8 |
| S-10 / BO-CNT-020 | 15 | 7 | 8 |
| 합계 | 36 | 18 | 18 |

| 원문 역할 | 화면 귀속 | 명시 코드 / ELEMENT_CODES 키 | 동작 원문 | locator 근거 원문 | 등록/미등록 사유 |
|---|---|---|---|---|---|
| D-01.open-related-list | BO-DSH-010 | 미발급 | KPI·행·목록/기록 링크·문의 합계에서 기존 목록 이동, d01-origin 저장 | D: onRowClick·Link·KpiTile href. 명시 링크 `a[data-d01-origin]`; 행 rowTestId와 KPI 내부 DOM은 공용 계약 | 미등록: 역할 확정, 명시 Link locator 확정; KPI/DataGrid 내부 locator 미확정 |
| D-01.toggle-scope-help | BO-DSH-010 | BO-DSH-010-TOGGLE-SCOPE-HELP | “무엇을 센 숫자인가요?” 펼침/닫힘 | D: `details.d01-help > summary`; 대상 섹션 testId 별도 | 등록: 확정 |
| D-01.retry-read | BO-DSH-010 | 미발급 | 오류 시 운영 요약 재조회 | D: ErrorBanner onRetry → load; busy면 onRetry 없음 | 미등록: 역할 확정, ErrorBanner 내부 locator 미확정 |
| S-01.select-status | BO-CNT-010 | 미발급 | 전체/발행/임시저장/보관 선택 → set({tab}) | C: TABS·ScreenTabs; testId 계약 tab-all/tab-published/tab-draft/tab-archived | 미등록: 역할 확정; 공용 ScreenTabs 내부 locator 미확정 |
| S-01.change-search | BO-CNT-010 | 미발급 | 제목/질문 검색값 변경 → set({q}) | C: ListFilterBar search; testId 계약 filter-q | 미등록: 역할 확정; 공용 검색 내부 locator 미확정 |
| S-01.reset-filters | BO-CNT-010 | 미발급 | 필터 초기화 → reset | C: ListFilterBar onReset | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-01.open-create | BO-CNT-010 | BO-CNT-010-OPEN-CREATE | 새 공지/FAQ 작성 → EMPTY_EDITOR | C: [data-testid="open-create"] | 등록: 확정 |
| S-01.open-content | BO-CNT-010 | 미발급 | 행 id의 상세 조회 후 편집 열기 | C: onRowClick → openEdit(row.id); rowTestId content-row-${row.id} | 미등록: 역할 확정; DataGrid 내부 locator 미확정, 대상 row.id 별도 |
| S-01.retry-read | BO-CNT-010 | 미발급 | 목록 조회 오류에서 load 재시도 | C: ErrorBanner onRetry | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-01.change-page | BO-CNT-010 | 미발급 | 목록 페이지 변경 → set({page}) | C: Pager onPage | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-01.change-page-size | BO-CNT-010 | 미발급 | 페이지 크기 변경 → set({size}) | C: Pager onSize | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-01.close-editor | BO-CNT-010 | 미발급 | 편집 닫기 → setEditor(null) | C: Modal onClose·footer 닫기 버튼; content-editor는 Modal testId 계약 | 미등록: 역할 확정; Modal 닫기 내부 locator 미확정, footer 버튼에 별도 testid 없음 |
| S-01.change-title | BO-CNT-010 | BO-CNT-010-CHANGE-TITLE | 제목/질문 입력 (최대 200자) | C: [data-testid="editor-title"] | 등록: 확정 |
| S-01.select-category | BO-CNT-010 | BO-CNT-010-SELECT-CATEGORY | 분류 선택, 빈 값은 분류 없음 | C: select[data-testid="editor-category"]; 대상 options.code | 등록: 확정 |
| S-01.change-body | BO-CNT-010 | BO-CNT-010-CHANGE-BODY | 본문/답변 입력 | C: [data-testid="editor-body"] | 등록: 확정 |
| S-01.save-draft | BO-CNT-010 | BO-CNT-010-SAVE-DRAFT | 신규 임시저장 / 기존 수정 저장 → save(draft) | C: data-testid="editor-save"·editor-save-reason | 등록: 확정 (ActionButton 공급 testid 계약; 내부 구조 미지정) |
| S-01.publish-content | BO-CNT-010 | BO-CNT-010-PUBLISH-CONTENT | 발행 → save(publish) | C: data-testid="editor-publish"·editor-publish-reason | 등록: 확정 (ActionButton 공급 testid 계약; 내부 구조 미지정) |
| S-01.archive-content | BO-CNT-010 | BO-CNT-010-ARCHIVE-CONTENT | 보관으로 이동 → save(archive) | C: data-testid="editor-archive"·editor-archive-reason | 등록: 확정 (ActionButton 공급 testid 계약; 내부 구조 미지정) |
| S-01.select-pinned | BO-CNT-010 | BO-CNT-010-SELECT-PINNED | 일반/고정 선택 → pinned=false/true | C: !faq 분기 Space.Compact의 Button onClick·aria-pressed, 텍스트 일반/고정 | 등록: 확정; 전용 testid 없음, 새 selector 없음 |
| S-01.select-visibility | BO-CNT-010 | BO-CNT-010-SELECT-VISIBILITY | 노출/숨김 선택 → visible=true/false | C: !faq 분기 Space.Compact의 Button onClick·aria-pressed, 텍스트 노출/숨김 | 등록: 확정; 전용 testid 없음, 새 selector 없음 |
| S-01.change-publish-at | BO-CNT-010 | BO-CNT-010-CHANGE-PUBLISH-AT | 예약 발행 시각 입력 | C: input type=datetime-local, data-testid="editor-publish-at" | 등록: 확정 |
| S-10.select-status | BO-CNT-020 | 미발급 | 전체/발행/임시저장/보관 선택 → set({tab}) | C: TABS·ScreenTabs; testId 계약 tab-all/tab-published/tab-draft/tab-archived | 미등록: 역할 확정; 공용 ScreenTabs 내부 locator 미확정 |
| S-10.change-search | BO-CNT-020 | 미발급 | 제목/질문 검색값 변경 → set({q}) | C: ListFilterBar search; testId 계약 filter-q | 미등록: 역할 확정; 공용 검색 내부 locator 미확정 |
| S-10.reset-filters | BO-CNT-020 | 미발급 | 필터 초기화 → reset | C: ListFilterBar onReset | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-10.open-create | BO-CNT-020 | BO-CNT-020-OPEN-CREATE | 새 공지/FAQ 작성 → EMPTY_EDITOR | C: [data-testid="open-create"] | 등록: 확정 |
| S-10.open-content | BO-CNT-020 | 미발급 | 행 id의 상세 조회 후 편집 열기 | C: onRowClick → openEdit(row.id); rowTestId content-row-${row.id} | 미등록: 역할 확정; DataGrid 내부 locator 미확정, 대상 row.id 별도 |
| S-10.retry-read | BO-CNT-020 | 미발급 | 목록 조회 오류에서 load 재시도 | C: ErrorBanner onRetry | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-10.change-page | BO-CNT-020 | 미발급 | 목록 페이지 변경 → set({page}) | C: Pager onPage | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-10.change-page-size | BO-CNT-020 | 미발급 | 페이지 크기 변경 → set({size}) | C: Pager onSize | 미등록: 역할 확정; 공용 내부 locator 미확정 |
| S-10.close-editor | BO-CNT-020 | 미발급 | 편집 닫기 → setEditor(null) | C: Modal onClose·footer 닫기 버튼; content-editor는 Modal testId 계약 | 미등록: 역할 확정; Modal 닫기 내부 locator 미확정, footer 버튼에 별도 testid 없음 |
| S-10.change-title | BO-CNT-020 | BO-CNT-020-CHANGE-TITLE | 제목/질문 입력 (최대 200자) | C: [data-testid="editor-title"] | 등록: 확정 |
| S-10.select-category | BO-CNT-020 | BO-CNT-020-SELECT-CATEGORY | 분류 선택, 빈 값은 분류 없음 | C: select[data-testid="editor-category"]; 대상 options.code | 등록: 확정 |
| S-10.change-body | BO-CNT-020 | BO-CNT-020-CHANGE-BODY | 본문/답변 입력 | C: [data-testid="editor-body"] | 등록: 확정 |
| S-10.save-draft | BO-CNT-020 | BO-CNT-020-SAVE-DRAFT | 신규 임시저장 / 기존 수정 저장 → save(draft) | C: data-testid="editor-save"·editor-save-reason | 등록: 확정 (ActionButton 공급 testid 계약; 내부 구조 미지정) |
| S-10.publish-content | BO-CNT-020 | BO-CNT-020-PUBLISH-CONTENT | 발행 → save(publish) | C: data-testid="editor-publish"·editor-publish-reason | 등록: 확정 (ActionButton 공급 testid 계약; 내부 구조 미지정) |
| S-10.archive-content | BO-CNT-020 | BO-CNT-020-ARCHIVE-CONTENT | 보관으로 이동 → save(archive) | C: data-testid="editor-archive"·editor-archive-reason | 등록: 확정 (ActionButton 공급 testid 계약; 내부 구조 미지정) |

## gate·반복 정책 보존

같은 화면의 같은 동작 반복은 동일 역할로 묶고 대상 id/값은 별도로 기록한다(TAX 방식). 상태 탭은 같은 상태 선택 handler이므로 한 역할이다. 페이지 방향도 같은 페이지 변경 역할로 묶는다. 고정/노출의 양쪽 값은 각각 한 설정 역할이다. 모달 여러 닫기 경로도 한 역할이다. 서로 다른 화면은 별도 역할이다. 읽기 요소·자동 조회·포커스 복원은 동작 분모에서 제외한다.

- D01 GET `/admin/summary`와 하단 GET `/admin/remediation/reconciler`는 독립 조회다. 403은 기존 권한 안내, 오류/로딩 중 이전 자료 안내를 유지한다. `—`는 0이 아니고 failedBatches는 collected:false다. 최근 실행 50건 링크는 미집계 실패 건수와 같은 범위가 아니다. 문의 링크는 전체 상태이며 시간 구간 자동 적용이 없다. 두 잔액 검사의 합계는 고유 회원 수·손실액이 아니다. origin 포커스·스크롤 복원은 사용자 동작 역할로 더 세지 않는다.
- S01/S10 목록 GET `/admin/support/contents`는 kind/status/q/page/size 계약, 분류 GET `/admin/support/contents/options?kind=…`, 편집 GET `/admin/support/contents/${id}`는 available·post가 유효할 때만 모달을 연다. available:false·조회 실패·빈 결과·초기 로딩 분기를 유지하며 새 권한별 버튼 조건은 없다. 권한 허용 여부는 이 정적 조사에서 확인하지 않았다.
- 저장은 신규 POST `/admin/support/contents`, 기존 PUT `/admin/support/contents/${editor.id}`에 매번 기존 Idempotency-Key를 보낸다. draft/publish/archive는 같은 요청 경로의 payload 모드다. busy 또는 제목/본문 trim 결과가 비면 저장·발행 비활성, 보관은 busy 또는 status가 PUBLISHED가 아니면 비활성이다. 신규/기존 저장 문구 차이는 동일 역할이다. 닫기에는 소스상 별도 busy 제한이 없다.
- 공지 전용 pinned/visible·예약 입력은 FAQ에서 렌더하지 않는다. FAQ pinned/visible payload는 null이다. 예약값이 있으면 기존 ISO 변환을 쓰며 기존 글을 열 때 publishAt 편집값은 빈 문자열로 시작한다. API 쓰기 위임 미가용 503 등 성공 여부는 미검증이다.
- S10 C-11 `bo-faq-draft`는 sessionStorage를 1회 소비하여 질문/답변을 채운다. S10의 클릭 컨트롤이 아니므로 별도 수신 역할을 만들지 않는다. 저장 후 OperationDecisionStatus의 `/admin/operation-decisions` 5초 폴링은 읽기 표시뿐이다(해당 컴포넌트 소스 확인). 수동 확인·재실행 역할을 만들지 않는다.
- 공용 Pager·Modal·ScreenTabs·ListFilterBar·DataGrid·ErrorBanner의 내부 locator는 A02 소유 확인 대기다. Panel·OperationDecisionStatus 내부 locator도 이 문서에서 확정하지 않는다. 메뉴·로그인·공용 껍데기, 읽기 수치/날짜/상태 태그는 이 동작 분모 밖이다. 분모 36은 DOM 노드 수나 시각 품질 점수가 아니다.

## D-01 반복 대상 근거 (원문, 코드에 혼입하지 않음)

 반복 18진입점(지표 행 13 + KPI 4 + 문의 합계 1)을 한 역할로 센다. 행과 그 안 링크는 같은 대상·동작이다. 대상 목록:

| 대상 id (rowTestId / origin) | 목적지 |
|---|---|
| card-stalledTransactions | /t-12?tab=STALLED |
| d01-failed-decisions | /t-12?tab=FAILED_DECISION |
| d01-unissued-contracts | /t-12?tab=UNISSUED_CONTRACT |
| d01-residual-reservations | /t-12?tab=RESIDUAL_RESERVATION |
| d01-over72h / d01-24to72h / d01-under24h | /c-10 |
| card-openDisputes | /c-20 |
| card-pendingApplications | /m-10 |
| card-pendingManualVerifications | /m-20 |
| card-failedTaxInvoices | /x-10 |
| card-reconciliationMismatches | /x-21 |
| card-failedBatches | /y-10 |
| kpi-stalled / kpi-failedDecisions / kpi-over72h / kpi-openDisputes | 순서대로 /t-12?tab=STALLED / /t-12?tab=FAILED_DECISION / /c-10 / /c-20 |
| inquiry-total（Link testid: card-unansweredInquiries） | /c-10 |

KPI 공급 testId는 순서대로 `d01-kpi-stalled`, `d01-kpi-failed-decisions`, `d01-kpi-over72h`, `d01-kpi-disputes`다. 도움말 대상 섹션은 `today-queue`, `d01-support`, `d01-review`, `d01-finance`, `d01-system`이다. ReconcilerStatus dashboard는 읽기/30초 자동 조회만 있어 수동 재조회 역할을 발명하지 않는다.


## 검토 범위

입력은 dsh-sup-confirmed.json(등록18)과 dsh-sup-coverage.json(분모36·미등록사유18)이다. 혼합 locator 역할 D-01.open-related-list는 명시 Link만 따로 등록하지 않는다. S-01 고정/노출은 문구·aria-pressed 원문 근거를 그대로 보존하며 selector를 발명하지 않는다. 보관·저장·발행 gate와 반복 정책은 SQL behavior에도 보존한다.

확인하지 못함: locator DOM/실동작, API/권한 성공, A08 최종 코드 대조 승인, 제품 소비본 반입. 원본 통합·커밋·네트워크·제품 시험 없음.
