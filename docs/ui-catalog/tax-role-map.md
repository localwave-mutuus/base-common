# TAX 소유팀 검토용 원문 ↔ 코드 대조표

역할 43행 대조 수용: A09 `01M59PFJFKWFCM3MN8Z4NVFA7W`. 제품 기준 `99fded312c98e946fff9686cc23e88b4377e0ae6`.
정본 근거: 조직 `f2eb1a127862280526bd46665ad640a594651a3f`, `teams/A09.화면개선2/docs/10-design/ui/specs/tax-preview-v1/03-screens.md`.
전 입력 trace-inventory 근거: 조직 `b1e07bbc3396fafbe4eeb98340bdee27fff9f26e`, `teams/A09.화면개선2/docs/10-design/ui/review-inputs/tax-before/trace-inventory.md`.
이번 갱신은 GIT_COMMIT과 문서 바이트 SHA-256으로 근거만 승격한다. 기존 28등록·15제외·코드·locator·도입 버전은 유지한다.

locator는 원문 설명/템플릿을 그대로 보존한다. 실행 가능한 CSS로 재해석하지 않는다. `${...}`는 대상 ID 자리이며 코드에 들어가지 않는다. 비활성·위험 행동도 역할 식별만 등록하며 실행하지 않는다. 공용 내부와 설명만 있는 버튼은 보수적으로 제외한다.

| 화면 | 등록/전체 | locator 미확정/전체 |
|---|---|---|
| X-01 / BO-TAX-010 | 4/4 | 0/4 |
| X-10 / BO-TAX-020 | 5/5 | 0/5 |
| X-11 / BO-TAX-030 | 4/4 | 0/4 |
| X-20 / BO-TAX-040 | 9/18 | 9/18 |
| X-21 / BO-TAX-050 | 6/12 | 6/12 |

역할명은 43/43 확정이며 locator 확정 여부를 위 분모로 구분한다. X30은 미구현으로 분모 제외. SYS는 별도 [검토표](sys-role-map.md)에 130건 등록했다. TAX 전체 T2 완료 또는 locator 실동작 검증을 뜻하지 않는다.

| 원문 역할 | action token | 요소코드(등록 또는 예약 후보) | locator 원문 | 상태/누락 사유 |
|---|---|---|---|---|
| &lt;X-01.open-tax-list&gt; | open-tax-list | BO-TAX-010-OPEN-TAX-LIST | a[href="/x-10"] | 등록 |
| &lt;X-01.open-account-comparison&gt; | open-account-comparison | BO-TAX-010-OPEN-ACCOUNT-COMPARISON | a[href="/x-21?tab=accounts&mismatchOnly=true"] | 등록 |
| &lt;X-01.open-cross-comparison&gt; | open-cross-comparison | BO-TAX-010-OPEN-CROSS-COMPARISON | a[href="/x-21?tab=cross&mismatchOnly=true"] | 등록 |
| &lt;X-01.toggle-scope-help&gt; | toggle-scope-help | BO-TAX-010-TOGGLE-SCOPE-HELP | summary: 집계 범위와 전송 설정 안내 | 등록 |
| &lt;X-10.select-status&gt; | select-status | BO-TAX-020-SELECT-STATUS | tab-failed / tab-transmitted / tab-notsubmitted / tab-manual / tab-all | 등록 |
| &lt;X-10.change-search&gt; | change-search | BO-TAX-020-CHANGE-SEARCH | filter-q | 등록 |
| &lt;X-10.previous-page&gt; | previous-page | BO-TAX-020-PREVIOUS-PAGE | page-prev | 등록 |
| &lt;X-10.next-page&gt; | next-page | BO-TAX-020-NEXT-PAGE | page-next | 등록 |
| &lt;X-10.open-invoice&gt; | open-invoice | BO-TAX-020-OPEN-INVOICE | tax-row-${row.id} | 등록 |
| &lt;X-11.return-to-list&gt; | return-to-list | BO-TAX-030-RETURN-TO-LIST | back-to-x10 | 등록 |
| &lt;X-11.open-trade&gt; | open-trade | BO-TAX-030-OPEN-TRADE | goto-trade | 등록 |
| &lt;X-11.open-parent-invoice&gt; | open-parent-invoice | BO-TAX-030-OPEN-PARENT-INVOICE | goto-parent | 등록 |
| &lt;X-11.resend-invoice&gt; | resend-invoice | BO-TAX-030-RESEND-INVOICE | x11-not-ready-resend (비활성) | 등록 |
| &lt;X-20.change-search&gt; | change-search | BO-TAX-040-CHANGE-SEARCH | filter-q | 등록 |
| &lt;X-20.reset-filters&gt; | reset-filters | BO-TAX-040-RESET-FILTERS | ListFilterBar 초기화 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.change-page&gt; | change-page | BO-TAX-040-CHANGE-PAGE | Pager 페이지 버튼 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.change-page-size&gt; | change-page-size | BO-TAX-040-CHANGE-PAGE-SIZE | Pager 크기 선택 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.open-account&gt; | open-account | BO-TAX-040-OPEN-ACCOUNT | account-row-${a.id} 및 내역 보기 버튼: 같은 동작은 같은 이름 | 등록 |
| &lt;X-20.open-comparison&gt; | open-comparison | BO-TAX-040-OPEN-COMPARISON | 잔액 차이 확인 버튼 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.retry-read&gt; | retry-read | BO-TAX-040-RETRY-READ | ErrorBanner 다시 시도 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.close-process-panel&gt; | close-process-panel | BO-TAX-040-CLOSE-PROCESS-PANEL | ProcessPanel 닫기 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.open-entry-evidence&gt; | open-entry-evidence | BO-TAX-040-OPEN-ENTRY-EVIDENCE | ProcessPanelMore 자세히 / InfoLayer 열기 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.close-entry-evidence&gt; | close-entry-evidence | BO-TAX-040-CLOSE-ENTRY-EVIDENCE | InfoLayer 닫기 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-20.toggle-account-id&gt; | toggle-account-id | BO-TAX-040-TOGGLE-ACCOUNT-ID | summary: 장부 계정 확인용 번호 (같은 번호 펼침들) | 등록 |
| &lt;X-20.toggle-party-ids&gt; | toggle-party-ids | BO-TAX-040-TOGGLE-PARTY-IDS | summary: 당사자 확인용 번호 | 등록 |
| &lt;X-20.change-action-reason&gt; | change-action-reason | BO-TAX-040-CHANGE-ACTION-REASON | #wu05-reason | 등록 |
| &lt;X-20.release-hold&gt; | release-hold | BO-TAX-040-RELEASE-HOLD | ledger-release-${e.id} (위험, 실행 제외) | 등록 |
| &lt;X-20.open-refund-review&gt; | open-refund-review | BO-TAX-040-OPEN-REFUND-REVIEW | ledger-refund-r10 | 등록 |
| &lt;X-20.open-adjustment-review&gt; | open-adjustment-review | BO-TAX-040-OPEN-ADJUSTMENT-REVIEW | ledger-adjustment-r10 | 등록 |
| &lt;X-20.toggle-previous-receipts&gt; | toggle-previous-receipts | BO-TAX-040-TOGGLE-PREVIOUS-RECEIPTS | ledger-previous-receipts summary | 등록 |
| &lt;X-20.inspect-operation-decision&gt; | inspect-operation-decision | BO-TAX-040-INSPECT-OPERATION-DECISION | OperationDecisionStatus 내부 접수 확인; 공용 소유 확인 필요 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-21.select-comparison&gt; | select-comparison | BO-TAX-050-SELECT-COMPARISON | tab-accounts / tab-cross | 등록 |
| &lt;X-21.select-mismatch-filter&gt; | select-mismatch-filter | BO-TAX-050-SELECT-MISMATCH-FILTER | filter-mismatch 내 불일치만 / 전체 | 등록 |
| &lt;X-21.open-inspection&gt; | open-inspection | BO-TAX-050-OPEN-INSPECTION | inspection-row-A/${r.accountId} / inspection-row-C/${r.ownerId}/${r.issuerId} | 등록 |
| &lt;X-21.close-process-panel&gt; | close-process-panel | BO-TAX-050-CLOSE-PROCESS-PANEL | ProcessPanel 닫기 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-21.open-comparison-evidence&gt; | open-comparison-evidence | BO-TAX-050-OPEN-COMPARISON-EVIDENCE | 비교 근거 더 보기 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-21.close-comparison-evidence&gt; | close-comparison-evidence | BO-TAX-050-CLOSE-COMPARISON-EVIDENCE | InfoLayer 닫기 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-21.return-to-origin&gt; | return-to-origin | BO-TAX-050-RETURN-TO-ORIGIN | 이전 화면으로 돌아가기 | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-21.toggle-scope-help&gt; | toggle-scope-help | BO-TAX-050-TOGGLE-SCOPE-HELP | summary: 검사 범위와 건수 안내 | 등록 |
| &lt;X-21.toggle-account-id&gt; | toggle-account-id | BO-TAX-050-TOGGLE-ACCOUNT-ID | summary: 장부 계정 확인용 번호 / 계정 번호 보기 | 등록 |
| &lt;X-21.toggle-party-ids&gt; | toggle-party-ids | BO-TAX-050-TOGGLE-PARTY-IDS | summary: 계정 당사자 번호 보기 / 확인용 번호 | 등록 |
| &lt;X-21.recalculate&gt; | recalculate | BO-TAX-050-RECALCULATE | 재계산 (비활성) | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |
| &lt;X-21.create-adjustment&gt; | create-adjustment | BO-TAX-050-CREATE-ADJUSTMENT | 조정 전표 작성 (비활성) | 미등록: 공용 내부 locator 또는 설명만 존재; 소유자 정확한 locator 확인 대기 |

미등록 행의 코드는 기계적 1:1 대조용이며 정본/생성 상수가 아니다. 새 역할을 만들거나 동의어를 추정하지 않았다.
