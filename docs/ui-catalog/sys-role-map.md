# SYS 소유팀 검토표: 원문 ↔ 정본 코드

A10 확정 공급: 조직 `d9051c7219816cc3065376b1b3366b7827656757`, 제품 `ef89e424231bc36ce58fda10825573ed1d177ed3`.
원문 바이트 SHA-256: `6baa98e39243e8af6760f55e80032602d4f0edf6fdd6151e0f3e55e9ac9c2c1e`。 입력은 `ui-catalog/inputs/sys-confirmed.json`이다.
130/130 등록, 확정 분모 안 미확정 제외 0. Y-30과 도달 불가 legacy는 분모 밖이며 아래 원문 제외 목록을 유지한다.
소유팀 후속 코드 대조 승인 미수신. 아래 표는 검토용이며 실동작·영역 T2 완료 선언이 아니다.

| 원문 역할 | 요소코드 / ELEMENT_CODES 키 | 화면 | locator 원문 | 문구 | 트리거 | 종류 | gate | 비고·반복 |
|---|---|---|---|---|---|---|---|---|
| Y-01.open-policy | BO-SYS-010-OPEN-POLICY | BO-SYS-010 | y01/index.tsx:160 Button | 기능 정책·긴급 차단 | click → navigate(/k-60) | 이동 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-10.refresh | BO-SYS-020-REFRESH | BO-SYS-020 | y10/index.tsx:99 Button | 새로고침 | click → load: capabilities → canRead일 때 jobs/runs GET | 조회 | loading 표시; canRead 없으면 목록 비움 | 전용 |
| Y-10.check-command | BO-SYS-020-CHECK-COMMAND | BO-SYS-020 | y10/index.tsx:103 Button;65 check | 같은 요청 결과 확인 | click → commands/{pending} GET | 조회 | pending 있을 때 표시; requestId 일치 검사; 종결 시 pending 제거 | 전용 |
| Y-10.select-job | BO-SYS-020-SELECT-JOB | BO-SYS-020 | y10/index.tsx:106 DataGrid | 작업 행 | 행 click/Enter/Space → jobId 선택, offset=0, stage/상세 초기화 → runs GET | 조회 | 행 존재; 인스턴스 jobId | 전용 |
| Y-10.open-run | BO-SYS-020-OPEN-RUN | BO-SYS-020 | y10/index.tsx:114 ActionButton;y10/model.ts:10 runBlocked | 수동 실행 | click → RUN 모달; 사유·단계 초기화 | 로컬 | enabled/canWrite, pending 없음, 선택 작업 enabled/manualRunSupported, 조회 오류 없음 | 전용 |
| Y-10.all-jobs | BO-SYS-020-ALL-JOBS | BO-SYS-020 | y10/index.tsx:115 Button | 전체 작업 보기 | click → jobId 비움, offset=0, 상세 제거 → runs GET | 조회 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-10.open-run-detail | BO-SYS-020-OPEN-RUN-DETAIL | BO-SYS-020 | y10/index.tsx:119 DataGrid;54 detail | 처리 번호 / 작업 행 | 행 click/Enter/Space → run·attempts GET | 조회 | runId 일치·history 배열 검사, 최신 선택 응답만 적용; runType/runId 인스턴스 | 전용 |
| Y-10.previous-page | BO-SYS-020-PREVIOUS-PAGE | BO-SYS-020 | y10/index.tsx:126 Button | 이전 | click → offset-50 → runs GET | 조회 | offset>0 && !loading | 전용 |
| Y-10.next-page | BO-SYS-020-NEXT-PAGE | BO-SYS-020 | y10/index.tsx:126 Button | 다음 | click → offset+50 → runs GET | 조회 | runs.length>=50 && offset<10000 && !loading && !error | 전용 |
| Y-10.refresh-detail | BO-SYS-020-REFRESH-DETAIL | BO-SYS-020 | y10/index.tsx:128 Button | 결과 갱신 | click → 선택 run·attempts GET | 조회 | selected 있을 때 | 전용 |
| Y-10.open-resolution | BO-SYS-020-OPEN-RESOLUTION | BO-SYS-020 | y10/index.tsx:141 ActionButton;y10/model.ts:18 resolveBlocked | 수동 검토 종료 | click → RESOLVE 모달; 사유/결과 초기화 | 로컬 | enabled/canWrite, pending 없음, RUN/manualResolutionSupported/MANUAL_REVIEW, detailError 없음 | 전용 |
| Y-10.close-action | BO-SYS-020-CLOSE-ACTION | BO-SYS-020 | y10/index.tsx:145 Button/Modal;components/Modal.tsx:36 | 닫기 | footer click 또는 모달 배경 click → modal=null | 로컬 | busy 중 닫기 차단; X/ESC 없음 | A02 Modal + 전용 footer; 같은 닫기 역할 |
| Y-10.submit-action | BO-SYS-020-SUBMIT-ACTION | BO-SYS-020 | y10/index.tsx:145 ActionButton;79 send | 확인 후 요청 | click → RUN: jobs/{jobId}/runs POST; RESOLVE: runs/RUN/{runId}/resolution POST | 쓰기 | busy/pending 차단; runBlocked/resolveBlocked 재검사; trim 사유 필수; RUN stages 있으면 stage 필수; requestId 생성·sessionStorage 보존; 명시 거절 외 오류는 재전송 차단 | 같은 버튼·역할; mode 메타데이터 RUN/RESOLVE |
| Y-10.select-stage | BO-SYS-020-SELECT-STAGE | BO-SYS-020 | y10/index.tsx:148 Select | 실행 단계 | change → stage | 로컬 | RUN && stages.length>0 | 전용 |
| Y-10.select-resolution | BO-SYS-020-SELECT-RESOLUTION | BO-SYS-020 | y10/index.tsx:149 Select | 확인 결과 | change → resolution | 로컬 | RESOLVE; CLOSED_NO_EFFECT / CLOSED_EFFECT_CONFIRMED | 전용 |
| Y-10.input-reason | BO-SYS-020-INPUT-REASON | BO-SYS-020 | y10/index.tsx:150 Input.TextArea | 처리 사유 | change → reason | 로컬 | maxLength=240; submit 시 trim 필수; 개인정보·비밀번호·본인확인 정보 입력 금지 안내 | 전용 |
| Y-20.filter-completion | BO-SYS-030-FILTER-COMPLETION | BO-SYS-030 | y20/index.tsx:79–90 Button 배열 | 미처리만 / 전체 | click → incompleteOnly 변경, page=0 → outbox GET | 조회 | option true/false 인스턴스; 동일 필터 역할 | 전용 |
| Y-20.input-event-type | BO-SYS-030-INPUT-EVENT-TYPE | BO-SYS-030 | y20/index.tsx:93 Input | 이벤트 유형 (예: Voucher.VoucherExpired) | change 즉시 eventType 변경, page=0 → outbox GET | 조회 | 요청 시 trim; submit 버튼 없음 | 전용 |
| Y-20.previous-page | BO-SYS-030-PREVIOUS-PAGE | BO-SYS-030 | y20/index.tsx:146 Button | 이전 | click → page-1 → outbox GET | 조회 | page>0 | 전용 |
| Y-20.next-page | BO-SYS-030-NEXT-PAGE | BO-SYS-030 | y20/index.tsx:149 Button | 다음 | click → page+1 → outbox GET | 조회 | page<totalPages-1 | 전용 |
| Y-40.edit-policy | BO-SYS-050-EDIT-POLICY | BO-SYS-050 | y40/index.tsx:211 ActionButton;100–101 snapshotFor/editStart | 편집 | click → module/slot의 유효값+raw expectedSnapshot을 editor에 복사 | 로컬 | !busy; module/slot 인스턴스 | 전용 |
| Y-40.restore-policy | BO-SYS-050-RESTORE-POLICY | BO-SYS-050 | y40/index.tsx:212 ActionButton;111 restore | 기본값 복원 | click → file-policies/{module}/{slot} DELETE | 쓰기 | !busy && overridden; expectedSnapshot·Idempotency-Key; 확인창/사유 입력 없음 | ActionButton disabledReason이 실제 disabled로 반영됨 |
| Y-40.input-visibility | BO-SYS-050-INPUT-VISIBILITY | BO-SYS-050 | y40/index.tsx:231 Input | visibility | change → editor.visibility | 로컬 | editor 존재; 입력 자체 busy disabled 없음 | 전용 |
| Y-40.input-max-size | BO-SYS-050-INPUT-MAX-SIZE | BO-SYS-050 | y40/index.tsx:231 Input | maxSize | change → editor.maxSize | 로컬 | editor 존재; 입력 자체 busy disabled 없음 | 전용 |
| Y-40.input-content-types | BO-SYS-050-INPUT-CONTENT-TYPES | BO-SYS-050 | y40/index.tsx:231 Input | contentTypes | change → editor.contentTypes | 로컬 | editor 존재; 입력 자체 busy disabled 없음 | 전용 |
| Y-40.input-access | BO-SYS-050-INPUT-ACCESS | BO-SYS-050 | y40/index.tsx:231 Input | access | change → editor.access | 로컬 | editor 존재; 입력 자체 busy disabled 없음 | 전용 |
| Y-40.input-max-count | BO-SYS-050-INPUT-MAX-COUNT | BO-SYS-050 | y40/index.tsx:231 Input | maxCount | change → editor.maxCount | 로컬 | editor 존재; 입력 자체 busy disabled 없음 | 전용 |
| Y-40.toggle-variants | BO-SYS-050-TOGGLE-VARIANTS | BO-SYS-050 | y40/index.tsx:231 checkbox | variants | change → editor.variants | 로컬 | editor 존재; 입력 자체 busy disabled 없음 | 전용 |
| Y-40.save-policy | BO-SYS-050-SAVE-POLICY | BO-SYS-050 | y40/index.tsx:231 ActionButton;102 save | 저장 | click → file-policies/{module}/{slot} PUT | 쓰기 | !busy && editor; expectedSnapshot·Idempotency-Key; contentTypes split/trim/filter/sort, maxCount Number; 별도 확인/사유/입력 유효성 gate 없음 | 전용 |
| Y-40.close-editor | BO-SYS-050-CLOSE-EDITOR | BO-SYS-050 | y40/index.tsx:231 Button | 닫기 | click → editor=null | 로컬 | busy 중에도 버튼 활성; 확인 없음 | 전용 |
| Y-50.select-step | BO-SYS-060-SELECT-STEP | BO-SYS-060 | y50/index.tsx:544 ScreenTabs;178 STEPS;240 moveTab | 1. 수집 확인 / 2. 업종 분류 결정 / 3. 자료 반영 확인 | 탭 선택 → tab; decisions에서 LOCAL runs, releases에서 release 목록 GET | 조회 | 공유 역할, step=status/decisions/releases; 열린 레이어 있으면 ProcessPanel.requestChange | A02 ScreenTabs; status legacy GET 없음 |
| Y-50.select-local-run | BO-SYS-060-SELECT-LOCAL-RUN | BO-SYS-060 | y50/localChangeCategories.tsx:71–73 Select;39 read | 변경분 작업 고르기 | change → 선택/판본/페이지/규칙 폼 초기화 → categories GET | 조회 | !busy; runId 인스턴스; 응답 ruleVersion을 부모 processing에 전달 | 전용 |
| Y-50.refresh-categories | BO-SYS-060-REFRESH-CATEGORIES | BO-SYS-060 | y50/localChangeCategories.tsx:79 Button | 다시 불러오기 | click → 선택 run/page/version categories GET | 조회 | selected && !busy | 전용 |
| Y-50.edit-category-rule | BO-SYS-060-EDIT-CATEGORY-RULE | BO-SYS-060 | y50/localChangeCategories.tsx:85 Button | 연결 규칙 처리 | click → group 선택; place/target/evidence 비움, MAPPED 설정 | 로컬 | !busy && publicCategoryCode; group 인스턴스 | 전용 |
| Y-50.previous-category-page | BO-SYS-060-PREVIOUS-CATEGORY-PAGE | BO-SYS-060 | y50/localChangeCategories.tsx:87 Button | 이전 | click → page-1 → categories GET | 조회 | !busy && page>0 | 전용 |
| Y-50.next-category-page | BO-SYS-060-NEXT-CATEGORY-PAGE | BO-SYS-060 | y50/localChangeCategories.tsx:87 Button | 다음 | click → page+1 → categories GET | 조회 | !busy && data.hasMore | 전용 |
| Y-50.select-rule-decision | BO-SYS-060-SELECT-RULE-DECISION | BO-SYS-060 | y50/localChangeCategories.tsx:90 Select | 처리 방법 | change → MAPPED/MAP_EXCLUDED/HELD | 로컬 | group && !busy | 전용 |
| Y-50.select-target-category | BO-SYS-060-SELECT-TARGET-CATEGORY | BO-SYS-060 | y50/localChangeCategories.tsx:91 Select | 지정할 업종 / 업종 이름으로 찾아 고르기 | 검색·선택 → target | 로컬 | !busy && decision=MAPPED; 후보 data.candidates 안에서 선택 | 전용 |
| Y-50.select-rule-scope | BO-SYS-060-SELECT-RULE-SCOPE | BO-SYS-060 | y50/localChangeCategories.tsx:92 Select | 적용 범위 | change → 전체 공공 업종 또는 조회된 표본 업체 예외 | 로컬 | !busy; 전체는 빈 place, 개별은 group.samples에 있는 sourcePlaceId | 전용 |
| Y-50.input-rule-version | BO-SYS-060-INPUT-RULE-VERSION | BO-SYS-060 | y50/localChangeCategories.tsx:94 Input | 새 규칙 판본 이름 | change → newVersion | 로컬 | !busy | 전용 |
| Y-50.input-rule-evidence | BO-SYS-060-INPUT-RULE-EVIDENCE | BO-SYS-060 | y50/localChangeCategories.tsx:95 Input | 처리 사유(담당자는 로그인 계정으로 기록) | change → evidence | 로컬 | !busy | 전용 |
| Y-50.save-rule-version | BO-SYS-060-SAVE-RULE-VERSION | BO-SYS-060 | y50/localChangeCategories.tsx:96 Button;47 save | 새 판본 저장 | click → runs/{run}/rules POST | 쓰기 | pending ref/busy; selected/group/newVersion.trim/evidence.trim 필수; MAPPED 유효 후보, 개별 범위 표본 검사; baseVersion 포함; 확인창 없음 | 저장만으로 재연결하지 않음 |
| Y-50.rematch-categories | BO-SYS-060-REMATCH-CATEGORIES | BO-SYS-060 | y50/localChangeCategories.tsx:98 Button;61 rematch | 전체 다시 연결 실행 | click → runs/{run}/rematch POST → categories GET | 쓰기 | !busy && version; pending ref/selected 재검사; 확인/사유 추가 없음 | 전용 |
| Y-50.refresh-processing | BO-SYS-060-REFRESH-PROCESSING | BO-SYS-060 | y50/localChangeProcessing.tsx:43 Button;16 read | 승인 요약 다시 불러오기 | click → runs/{run}/processing?version GET | 조회 | run 존재 && !busy; handler에서 ruleVersion 없으면 return | 전용 |
| Y-50.geocode | BO-SYS-060-GEOCODE | BO-SYS-060 | y50/localChangeProcessing.tsx:47 Button | 주소 위치 변환(한 번에 20건까지) | click → processing/geocode?limit=20 POST → 요약 GET | 쓰기 | !busy && summary; action pending ref; 확인/사유 없음 | 전용 |
| Y-50.toggle-source-metadata | BO-SYS-060-TOGGLE-SOURCE-METADATA | BO-SYS-060 | y50/localChangeProcessing.tsx:49 details/summary | 원본 자료 정보 보기 | click/키보드 → details 열기·닫기 | 로컬 | summary 존재; API 없음 | 전용 |
| Y-50.input-policy-version | BO-SYS-060-INPUT-POLICY-VERSION | BO-SYS-060 | y50/localChangeProcessing.tsx:52 Input | 지역 정책 판본 | change → policy | 로컬 | !busy; 비어 있음에 대한 별도 UI 차단 없음 | 전용 |
| Y-50.toggle-protection-links | BO-SYS-060-TOGGLE-PROTECTION-LINKS | BO-SYS-060 | y50/localChangeProcessing.tsx:53 details/summary | 기술 근거 · 회원 보호 연결 목록 | click/키보드 → details 열기·닫기 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-50.input-protection-links | BO-SYS-060-INPUT-PROTECTION-LINKS | BO-SYS-060 | y50/localChangeProcessing.tsx:53 Input.TextArea | 소유 서비스가 확인한 보호 연결 JSON | change → metadata | 로컬 | !busy; prepare에서 JSON 배열 검사; 확인 없이 [] 대체 금지 안내 | 전용 |
| Y-50.input-package-reason | BO-SYS-060-INPUT-PACKAGE-REASON | BO-SYS-060 | y50/localChangeProcessing.tsx:54 Input | 승인 요청 사유 | change → reason | 로컬 | !busy; prepare trim 필수 | 전용 |
| Y-50.input-step-up-password | BO-SYS-060-INPUT-STEP-UP-PASSWORD | BO-SYS-060 | y50/localChangeProcessing.tsx:55 Input.Password | 본인확인 — 현재 관리자 비밀번호 | change → password; Password 기본 표시/숨김 affordance | 로컬 | !busy; action finally에서 비움 | 실제 요소 명세만 기록; 비밀번호 입력·인증 호출 실행하지 않음 |
| Y-50.request-package | BO-SYS-060-REQUEST-PACKAGE | BO-SYS-060 | y50/localChangeProcessing.tsx:56 Button;22 prepare | 승인 요청 보내기 | click → processing/step-up POST → processing/prepare POST | 쓰기 | !busy && summary.ready && password && reason.trim && !prepared; JSON 배열; pending ref; stepUpToken 전달; 확인문구 없음 | 준비·승인 요청이며 운영 자료 적용 아님 |
| Y-50.input-package-id | BO-SYS-060-INPUT-PACKAGE-ID | BO-SYS-060 | y50/localChangeProcessing.tsx:61 Input | 승인 요청 번호 붙여넣기(예외) | change → packageId; prepared=null | 로컬 | !busy | 전용 |
| Y-50.load-package | BO-SYS-060-LOAD-PACKAGE | BO-SYS-060 | y50/localChangeProcessing.tsx:63 Button;28 current | 승인 요청 불러오기 | click → packages/{packageId} GET | 조회 | !busy && packageId; action finally에서 password 비움 | 전용 |
| Y-50.toggle-package-manifest | BO-SYS-060-TOGGLE-PACKAGE-MANIFEST | BO-SYS-060 | y50/localChangeProcessing.tsx:65 details/summary | 승인 요청 내용 보기 | click/키보드 → details 열기·닫기 | 로컬 | prepared 존재 | 전용 |
| Y-50.approve-package | BO-SYS-060-APPROVE-PACKAGE | BO-SYS-060 | y50/localChangeProcessing.tsx:66 Button;29 approve | 다른 관리자로 승인 | click → packages/{id}/step-up POST → approve POST → package GET | 쓰기 | !busy && REQUESTED && password; pending ref; 다른 관리자 요구는 문구이며 UI requesterId 비교 없음; 서버 보호를 UI 구현으로 오인 금지 | 전용 |
| Y-50.export-package | BO-SYS-060-EXPORT-PACKAGE | BO-SYS-060 | y50/localChangeProcessing.tsx:69 Button;33 download | 승인 파일 내보내기 | click → packages/{id}/export 직접 fetch GET → blob ZIP 저장 | 조회·파일 | !busy && APPROVED; pending ref; 자동 재전송 없음 | 공용 api 추적 경로 밖; 동적 link는 같은 export 역할 |
| Y-60.select-input-type | BO-SYS-070-SELECT-INPUT-TYPE | BO-SYS-070 | y60/index.tsx:447/450 Button | 사업자번호 / 상호 | click → inputType; 회원 후보 초기화 | 로컬 | 타입별 인스턴스; busy 차단 없음 | 전용 |
| Y-60.input-collection-query | BO-SYS-070-INPUT-COLLECTION-QUERY | BO-SYS-070 | y60/index.tsx:455 Input | 사업자등록번호 10자리 / 상호 | change → inputValue; 회원 후보 초기화 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-60.start-collection | BO-SYS-070-START-COLLECTION | BO-SYS-070 | y60/index.tsx:457 Button;291 startCollection | 수집 요청 | click → business-information-collections POST → 목록/상세 GET | 쓰기 | !busy && !unavailable && input.trim; 사업자번호는 하이픈 제거 후 10자리; Idempotency-Key; 별도 확인/사유 없음 | 전용 |
| Y-60.collect-member-candidate | BO-SYS-070-COLLECT-MEMBER-CANDIDATE | BO-SYS-070 | y60/index.tsx:483 Button;291 startCollection | 이 업체로 수집 | click → memberId 포함 같은 수집 POST | 쓰기 | 회원 후보 응답 있을 때; !busy만 명시 gate; memberId 인스턴스 | 방어적 memberCandidates 응답 분기; 서버에서 반환됨을 보장하지 않음 |
| Y-60.filter-status | BO-SYS-070-FILTER-STATUS | BO-SYS-070 | y60/index.tsx:499 ListFilterBar.select | 전체 상태 등 | change → status, page=0 → 목록 GET | 조회 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-60.filter-requester | BO-SYS-070-FILTER-REQUESTER | BO-SYS-070 | y60/index.tsx:500 ListFilterBar.select | 전체 요청자 / 관리자 / 사장 | change → requesterType, page=0 → 목록 GET | 조회 | UI 동작 존재; 현재 BFF 지원 차이 아래 참조 | 전용 |
| Y-60.sort-requests | BO-SYS-070-SORT-REQUESTS | BO-SYS-070 | y60/index.tsx:501 ListFilterBar.select | 최신 요청순 / 낮은 신뢰도순 / 만료 임박순 | change → sort, page=0 → 목록 GET | 조회 | UI 동작 존재; 현재 BFF 지원 차이 아래 참조 | 전용 |
| Y-60.reset-filters | BO-SYS-070-RESET-FILTERS | BO-SYS-070 | y60/index.tsx:508;components/ListFilterBar.tsx:68 Button | 검색 조건 초기화 | click → status/requesterType 비움, 최신순, page=0 → 목록 GET | 조회 | 추가 gate 없음 | A02 ListFilterBar; 전용 onReset |
| Y-60.open-request | BO-SYS-070-OPEN-REQUEST | BO-SYS-070 | y60/index.tsx:525–529 DataTable;274 openDetail | 수집 요청 행 | 행 click/Enter/Space → 상세 GET; 후보/필드/preview/사유/확인/결과 초기화 | 조회 | requestId 인스턴스 | A02 DataTable 행 활성화 |
| Y-60.previous-page | BO-SYS-070-PREVIOUS-PAGE | BO-SYS-070 | y60/index.tsx:539 Button | 이전 | click → page-1 → 목록 GET | 조회 | rows!==null && !unavailable && total>0; page>0 | 전용 |
| Y-60.next-page | BO-SYS-070-NEXT-PAGE | BO-SYS-070 | y60/index.tsx:542 Button | 다음 | click → page+1 → 목록 GET | 조회 | rows!==null && !unavailable && total>0; page<totalPages-1 | 전용 |
| Y-60.close-detail | BO-SYS-070-CLOSE-DETAIL | BO-SYS-070 | y60/index.tsx:558/561 Modal/Button | 닫기 | footer 또는 배경 click → detail=null | 로컬 | busy 닫기 보호 없음; ESC/X 없음 | A02 Modal + 전용 footer |
| Y-60.refresh-collection | BO-SYS-070-REFRESH-COLLECTION | BO-SYS-070 | y60/index.tsx:564 Button;320 runRefresh | 재수집 | click → {requestId}/refresh POST → 상세/목록 GET | 쓰기 | !busy; Idempotency-Key; 확인/사유 없음 | 전용 |
| Y-60.open-reject | BO-SYS-070-OPEN-REJECT | BO-SYS-070 | y60/index.tsx:567 Button | 거절 | click → rejectModal(reason 빈값) | 로컬 | !busy | 전용 |
| Y-60.preview-fields | BO-SYS-070-PREVIEW-FIELDS | BO-SYS-070 | y60/index.tsx:570 Button;335 runPreview | 미리보기 | click → {requestId}/preview POST → snapshot 저장 | 쓰기·미리보기 | !busy && candidateId && selectedFields.size>0; Idempotency-Key; 사유/확인 초기화 | GET로 취급 금지 |
| Y-60.execute-fields | BO-SYS-070-EXECUTE-FIELDS | BO-SYS-070 | y60/index.tsx:573 Button;355 runExecute | 확정 실행 | click → {requestId}/execute POST → 목록/상세 GET | 쓰기 | !busy && preview && reason.trim && confirmation.trim===preview.confirmationText && !expired; 실행 시 만료 재검사; snapshotId/expected 버전·Idempotency-Key; 확인·사유 trim 전송 | 전용 |
| Y-60.open-audit | BO-SYS-070-OPEN-AUDIT | BO-SYS-070 | y60/index.tsx:584 Button href | 작업 이력 확인 | click → /z-30 이동 | 이동 | executed 있을 때 | 전용 |
| Y-60.select-candidate | BO-SYS-070-SELECT-CANDIDATE | BO-SYS-070 | y60/index.tsx:628 Panel;416 pickCandidate | 후보 상호/원천 카드 | click → candidateId, 선택 필드 비움, preview=null | 로컬 | 후보 존재; busy disabled 없음; candidateId 인스턴스 | 전용 |
| Y-60.open-candidate-source | BO-SYS-070-OPEN-CANDIDATE-SOURCE | BO-SYS-070 | y60/index.tsx:636 a | 원문 링크 | click → externalUrl 새 창 | 이동 | externalUrl 존재; noreferrer | 카드 내 링크는 stopPropagation 없음: 카드 선택도 발생 가능 |
| Y-60.toggle-field | BO-SYS-070-TOGGLE-FIELD | BO-SYS-070 | y60/index.tsx:650–656 checkbox;98 FIELD_DEFS;406 toggleField | 상호 / 업종 코드 / 도로명 주소 / 지번 주소 / 우편번호 / 전화 / 좌표 / 플레이스 링크 | change → 필드 Set 토글, preview=null; click 전파 차단 | 로컬 | 값 non-null && 선택 후보 일치; busy 차단 없음; candidateId/field 인스턴스 | 전용 |
| Y-60.input-execute-reason | BO-SYS-070-INPUT-EXECUTE-REASON | BO-SYS-070 | y60/index.tsx:714 Input.TextArea | 사유(필수) | change → reason | 로컬 | preview 있을 때; busy disabled 없음 | 전용 |
| Y-60.input-confirmation | BO-SYS-070-INPUT-CONFIRMATION | BO-SYS-070 | y60/index.tsx:719 Input | 확인문구 입력 | change → confirmation | 로컬 | preview 있을 때; busy disabled 없음 | 전용 |
| Y-60.close-reject | BO-SYS-070-CLOSE-REJECT | BO-SYS-070 | y60/index.tsx:729/732 Modal/Button | 닫기 | footer 또는 배경 click → rejectModal=null | 로컬 | busy 닫기 보호 없음; ESC/X 없음 | A02 Modal + 전용 footer; detail 닫기와 별개 대상 |
| Y-60.reject-request | BO-SYS-070-REJECT-REQUEST | BO-SYS-070 | y60/index.tsx:735 Button;389 runReject | 거절 | click → {requestId}/reject POST → 목록 GET; 상세 닫음 | 쓰기 | !busy && reject reason.trim; Idempotency-Key; 거절 모달에서 사유 필수 | 전용 |
| Y-60.input-reject-reason | BO-SYS-070-INPUT-REJECT-REASON | BO-SYS-070 | y60/index.tsx:744 Input.TextArea | 사유(필수) | change → rejectModal.reason | 로컬 | 거절 모달; maxLength prop 없음 | 전용 |
| Y-60.toggle-column-picker | BO-SYS-070-TOGGLE-COLUMN-PICKER | BO-SYS-070 | components/DataTable.tsx:109–122 Popover/Button | 열 | click → 열 팝오버 열기/닫기 | 로컬 | 호출 y60/index.tsx:515 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-60.toggle-column | BO-SYS-070-TOGGLE-COLUMN | BO-SYS-070 | components/DataTable.tsx:99–116 Checkbox | 각 열 이름 | change → hidden 설정 저장 | 로컬 | 마지막 보이는 열은 숨기지 않음; column key 인스턴스 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-60.reset-columns | BO-SYS-070-RESET-COLUMNS | BO-SYS-070 | components/DataTable.tsx:118 Button | 기본값으로 | click → hidden=[]/widths={} 저장 | 로컬 | 추가 gate 없음 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-60.toggle-target-identifier | BO-SYS-070-TOGGLE-TARGET-IDENTIFIER | BO-SYS-070 | components/TargetIdentity.tsx:18 details/summary | 대상 확인용 번호 / 기술 근거 보기 | click/키보드 → details 열기·닫기 | 로컬 | id 있을 때; targetType/id 인스턴스 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-60.copy-target-identifier | BO-SYS-070-COPY-TARGET-IDENTIFIER | BO-SYS-070 | components/TargetIdentity.tsx:18 Code;components/Code.tsx:13 Typography.copyable | 복사 / 복사됨 | click → 식별자 clipboard 복사 | 로컬 | 값 있을 때; row click 전파 차단 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-70.filter-status | BO-SYS-080-FILTER-STATUS | BO-SYS-080 | y70/index.tsx:337–343 ListFilterBar.segmented;ui/index.tsx:74–84 SegmentButtons | 대기 / 후보 선택 / 초안 / 확정 / 반려 | 선택 → queueStatus, page=0 → menu-documents GET | 조회 | status 인스턴스; pending submission이어도 필터 gate 없음 | A02 SegmentButtons |
| Y-70.reset-filters | BO-SYS-080-RESET-FILTERS | BO-SYS-080 | y70/index.tsx:349;components/ListFilterBar.tsx:68 | 검색 조건 초기화 | click → PENDING, page=0 → 목록 GET | 조회 | 추가 gate 없음 | A02 ListFilterBar |
| Y-70.open-document | BO-SYS-080-OPEN-DOCUMENT | BO-SYS-080 | y70/index.tsx:359–364 DataTable;231 openDetail | 문서 행 | 행 click/Enter/Space → 문서 GET; 종류별 source/image, 상태별 draft GET | 조회 | 다른 상세 열려 있으면 requestChange: busy/dirty 보호; documentId 인스턴스 | A02 DataTable/ProcessPanel; SOURCE_URL·PLACE_LOOKUP→source, IMAGE→직접 fetch image; DRAFTED·CONFIRMED·REJECTED→draft |
| Y-70.previous-page | BO-SYS-080-PREVIOUS-PAGE | BO-SYS-080 | y70/index.tsx:374 Button | 이전 | click → page-1 → 목록 GET | 조회 | rows!==null && !unavailable && total>0 && page>0 | 전용 |
| Y-70.next-page | BO-SYS-080-NEXT-PAGE | BO-SYS-080 | y70/index.tsx:377 Button | 다음 | click → page+1 → 목록 GET | 조회 | rows!==null && !unavailable && total>0 && page<totalPages-1 | 전용 |
| Y-70.open-place | BO-SYS-080-OPEN-PLACE | BO-SYS-080 | y70/index.tsx:449 Button href | 플레이스 보기 | click → candidate.placeUrl 새 창 | 이동 | source.candidates 존재; noreferrer; 후보 선택 쓰기 없음 | 전용 |
| Y-70.input-manual-category | BO-SYS-080-INPUT-MANUAL-CATEGORY | BO-SYS-080 | y70/index.tsx:464 Input | 메뉴 분류 이름 | change → manualCategory | 로컬 | 수동 폼 gate M70; IDLE; maxLength80 | 전용 |
| Y-70.input-manual-name | BO-SYS-080-INPUT-MANUAL-NAME | BO-SYS-080 | y70/index.tsx:466 Input | 메뉴 N 이름 | change → manualItems[i].name | 로컬 | M70; IDLE; maxLength120; row index는 인스턴스 | 전용 |
| Y-70.input-manual-price | BO-SYS-080-INPUT-MANUAL-PRICE | BO-SYS-080 | y70/index.tsx:467 Input number | 메뉴 N 가격 | change → manualItems[i].price | 로컬 | M70; IDLE; min1/max100000000; row index 인스턴스 | 전용 |
| Y-70.add-manual-item | BO-SYS-080-ADD-MANUAL-ITEM | BO-SYS-080 | y70/index.tsx:469 Button | 메뉴 행 추가 | click → 빈 메뉴 행 추가 | 로컬 | M70; IDLE && manualItems.length<100; 삭제 버튼 없음 | 전용 |
| Y-70.input-manual-hours | BO-SYS-080-INPUT-MANUAL-HOURS | BO-SYS-080 | y70/index.tsx:470 Input | 영업시간 | change → manualHours | 로컬 | M70; IDLE; maxLength255 | 전용 |
| Y-70.input-manual-reason | BO-SYS-080-INPUT-MANUAL-REASON | BO-SYS-080 | y70/index.tsx:471 Input.TextArea | 수동 초안 처리 사유 | change → manualReason | 로컬 | M70; IDLE; maxLength500 | 전용 |
| Y-70.save-manual-draft | BO-SYS-080-SAVE-MANUAL-DRAFT | BO-SYS-080 | y70/index.tsx:472 Button;285 submitManual;101 manualDraftPayload | 수동 초안 저장 | click → menu-documents/{id}/draft POST → 목록 GET | 쓰기 | M70; IDLE; submitting ref; 사유 trim 1..500; 메뉴 있으면 분류 1..80, 이름1..120, 정수 가격1..100000000, 최대100; 메뉴 또는 영업시간 필수; expectedDocumentVersion/Idempotency-Key; 별도 확인문구 없음 | SAVED는 사장님 확인 대기; 메뉴 반영 아님; UNKNOWN은 재제출 금지 |
| Y-70.recover-manual-result | BO-SYS-080-RECOVER-MANUAL-RESULT | BO-SYS-080 | y70/index.tsx:476 Button;311 recoverManual | 문서 · 최신 초안 확인 | click → 문서·draft GET; 이전 입력 보존 | 조회 | UNKNOWN 또는 REJECTED; submitting ref; REJECTED만 조회 후 IDLE 복귀; UNKNOWN은 최신 초안으로 자기 제출 성공 확정 안 함 | 전용 |
| Y-70.toggle-column-picker | BO-SYS-080-TOGGLE-COLUMN-PICKER | BO-SYS-080 | components/DataTable.tsx:109–122 Popover/Button | 열 | click → 열 팝오버 열기/닫기 | 로컬 | 호출 y70/index.tsx:350 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-70.toggle-column | BO-SYS-080-TOGGLE-COLUMN | BO-SYS-080 | components/DataTable.tsx:99–116 Checkbox | 각 열 이름 | change → hidden 설정 저장 | 로컬 | 마지막 보이는 열은 숨기지 않음; column key 인스턴스 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-70.reset-columns | BO-SYS-080-RESET-COLUMNS | BO-SYS-080 | components/DataTable.tsx:118 Button | 기본값으로 | click → hidden=[]/widths={} 저장 | 로컬 | 추가 gate 없음 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-70.toggle-target-identifier | BO-SYS-080-TOGGLE-TARGET-IDENTIFIER | BO-SYS-080 | components/TargetIdentity.tsx:18 details/summary | 대상 확인용 번호 / 기술 근거 보기 | click/키보드 → details 열기·닫기 | 로컬 | id 있을 때; targetType/id 인스턴스 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-70.copy-target-identifier | BO-SYS-080-COPY-TARGET-IDENTIFIER | BO-SYS-080 | components/TargetIdentity.tsx:18 Code;components/Code.tsx:13 Typography.copyable | 복사 / 복사됨 | click → 식별자 clipboard 복사 | 로컬 | 값 있을 때; row click 전파 차단 | 소유 A02; 공용 재사용; 제품 쓰기 없음 |
| Y-70.close-process-panel | BO-SYS-080-CLOSE-PROCESS-PANEL | BO-SYS-080 | y70/index.tsx:386;components/ProcessPanel.tsx:24/31/32 | 처리 패널 닫기 / 목록으로 | click 또는 ESC → requestChange(closeDetail) | 로컬 | SUBMITTING이면 이동 차단; dirty이면 버림 확인 | 소유 A02; 여러 닫기 표면 같은 역할 |
| Y-70.continue-writing | BO-SYS-080-CONTINUE-WRITING | BO-SYS-080 | components/ProcessPanel.tsx:17/33; y70/index.tsx:386 | 계속 작성 | click → 이동 취소, 원래 포커스 복귀 | 로컬 | dirty 이동 확인 표시 때 | 소유 A02 |
| Y-70.discard-and-move | BO-SYS-080-DISCARD-AND-MOVE | BO-SYS-080 | components/ProcessPanel.tsx:18/33; y70/index.tsx:386 | 작성 내용 버리고 이동 | click → 대기 중 이동/닫기 실행 | 로컬 | dirty 이동 확인 표시 때; 확인 중 본문 inert | 소유 A02 |
| Y-80.select-file-service | BO-SYS-090-SELECT-FILE-SERVICE | BO-SYS-090 | y80/index.tsx:62 Select | 서비스 | change → app, 파일 결과 초기화 | 로컬 | app/member/bo/batch; 업무 검색 서비스와 별개 | 전용 |
| Y-80.input-file-date | BO-SYS-090-INPUT-FILE-DATE | BO-SYS-090 | y80/index.tsx:64 Input date | 협정 세계시 날짜 | change → date, 파일 결과 초기화 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-80.list-files | BO-SYS-090-LIST-FILES | BO-SYS-090 | y80/index.tsx:66 Button;31 load | 조회 | click → log-files GET(environment/app/date) | 조회 | configuration.enabled && date; busy loading; 이전 응답 sequence 무효화 | 전용 |
| Y-80.download-file | BO-SYS-090-DOWNLOAD-FILE | BO-SYS-090 | y80/index.tsx:74 Button;43 download | 내려받기 / 파일명 내려받기 | click → log-files/download POST → 만료 검사 후 URL 파일 저장 | 쓰기·다운로드 발급 | 다른 파일 다운로드 중 disabled; expiresAt<=현재면 차단; 확인/사유 없음 | 발급 이력 감사 기록; file.key/name 인스턴스; link.click은 같은 역할 |
| Y-80.next-files | BO-SYS-090-NEXT-FILES | BO-SYS-090 | y80/index.tsx:78 Button | 다음 파일 | click → nextToken으로 목록 GET | 조회 | nextToken 존재 && !busy | 전용 |
| Y-80.select-user-type | BO-SYS-090-SELECT-USER-TYPE | BO-SYS-090 | y80/actionSearch.tsx:98 Select | 사용자 찾기 기준: 이메일 / 이름 / 사업자·대표자·골목 아이디 | change → userType; 후보/선택회원/검색 결과 초기화 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-80.input-user-query | BO-SYS-090-INPUT-USER-QUERY | BO-SYS-090 | y80/actionSearch.tsx:100 Input | 사용자 검색어 | change → queryText; 후보/선택회원/검색 결과 초기화 | 로컬 | 길이 EMAIL254/NAME100/MEMBER_QUERY120; autoComplete off | 전용 |
| Y-80.find-users | BO-SYS-090-FIND-USERS | BO-SYS-090 | y80/actionSearch.tsx:102 Button;52 findUsers | 회원 후보 찾기 | click → log-files/users GET(type/q/limit20) | 조회 | enabled && queryText.trim; handler userBusy 차단; 검색 결과/선택회원 초기화 | 전용 |
| Y-80.select-member | BO-SYS-090-SELECT-MEMBER | BO-SYS-090 | y80/actionSearch.tsx:103 Select | 검색할 회원 고르기 / 후보에서 회원 고르기 | change/allowClear → 후보에 있는 memberId 또는 빈값; 결과 초기화 | 로컬 | 후보 존재 && !userBusy && !traceId.trim; clear는 app=batch 또는 traceId 있을 때; memberId 인스턴스 | 전용 |
| Y-80.input-search-from | BO-SYS-090-INPUT-SEARCH-FROM | BO-SYS-090 | y80/actionSearch.tsx:111 Input datetime-local | 업무 기록 시작 일시(한국 시간) | change → from; 검색 결과 초기화 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-80.input-search-to | BO-SYS-090-INPUT-SEARCH-TO | BO-SYS-090 | y80/actionSearch.tsx:112 Input datetime-local | 업무 기록 종료 일시(한국 시간) | change → to; 검색 결과 초기화 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-80.select-search-services | BO-SYS-090-SELECT-SEARCH-SERVICES | BO-SYS-090 | y80/actionSearch.tsx:113 Select multiple | 업무 기록 서비스(복수 선택) | change → 허용 서비스만 중복제거/정렬/comma join; 검색 결과 초기화 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-80.input-request-trace | BO-SYS-090-INPUT-REQUEST-TRACE | BO-SYS-090 | y80/actionSearch.tsx:114 Input | 요청 추적값 붙여넣기(선택) | change → requestTraceId; 검색 결과 초기화 | 로컬 | !traceId.trim; autoComplete off | 전용 |
| Y-80.search-actions | BO-SYS-090-SEARCH-ACTIONS | BO-SYS-090 | y80/actionSearch.tsx:115 Button;60 search | 업무 기록 검색 | click → log-files/search GET; SCANNING cursor 자동 이어 조회 | 조회 | S80; pending ref; KST→UTC; 응답 기간/app/memberId/requestTraceId 결속 검사 | 회원검색과 상관키검색은 같은 버튼 역할 |
| Y-80.input-trace-id | BO-SYS-090-INPUT-TRACE-ID | BO-SYS-090 | y80/actionSearch.tsx:118 Input | 상관키로 검색 | change → traceId; 첫 비어있음→값 있음이면 traceAt 앞뒤10분 설정; 결과 초기화 | 로컬 | maxLength128; autoComplete off | 전용 |
| Y-80.input-trace-time | BO-SYS-090-INPUT-TRACE-TIME | BO-SYS-090 | y80/actionSearch.tsx:119 Input | 상관키 발생 시각(한국 시간) | change → traceAt; traceId 있으면 앞뒤10분; 결과 초기화 | 로컬 | 추가 gate 없음; 확인·사유 없음 | 전용 |
| Y-80.set-trace-window | BO-SYS-090-SET-TRACE-WINDOW | BO-SYS-090 | y80/actionSearch.tsx:120 Button | 발생 시각 앞뒤 10분 | click → from/to=traceAt ±10분; 결과 초기화 | 로컬 | traceId.trim && traceAt; 유효 시각일 때만 기간 변경 | 전용 |
| Y-80.open-hit-detail | BO-SYS-090-OPEN-HIT-DETAIL | BO-SYS-090 | y80/actionSearch.tsx:133 Table.onRow /141 Button | 기록 행 / 자세히 보기 | 행 또는 버튼 click → info=row | 로컬 | hit 존재; 버튼 stopPropagation; row key+line 인스턴스 | 두 표면 동일 동작·공유 역할; 행 키보드 활성 prop 없음 |
| Y-80.close-hit-detail | BO-SYS-090-CLOSE-HIT-DETAIL | BO-SYS-090 | y80/actionSearch.tsx:143 InfoLayer;components/InfoLayer.tsx:31 | 닫기 | footer/배경 click 또는 ESC → info=null | 로컬 | info 존재 | A02 InfoLayer→Modal keyboard=true; X 없음 |
| Y-80.toggle-hit-identifiers | BO-SYS-090-TOGGLE-HIT-IDENTIFIERS | BO-SYS-090 | y80/actionSearch.tsx:148 details/summary | 각 영역의 확인용 번호 보기 | click/키보드 → details 열기·닫기 | 로컬 | info 존재 | 전용 |
| Y-80.next-actions | BO-SYS-090-NEXT-ACTIONS | BO-SYS-090 | y80/actionSearch.tsx:150 Button;60 search | 다음 기록 | click → cursor GET, 같은 generation에 hits 이어붙임 | 조회 | COMPLETE && nextCursor && !busy; S80 재검사 | 전용 |

## 공급 계약·반복 정책 (원문)


역할은 생성 코드가 아닌 A02 정본 입력 키다. 형식은 `<기존화면ID>.<영문소문자-역할>`이며 각 화면 안에서 유일하다. BO-SYS 화면 코드를 요소 코드로 쓰거나 역할 문자열·testid로 요소 코드를 합성하지 않는다. 모든 행의 **현재 생성요소상수는 없음**이다. 근거: `telemetry/uiTrace.ts:8 ELEMENTS={}` 및 `apps/web/docs/ui-tracing.md:7–9`. 화면 코드는 `generated/ui-catalog/bo-catalog.ts:667–801`의 기존 화면 매핑만 사용한다.

분모는 아래 8개 구현 화면의 **도달 가능한 의미 동작 역할 수**다. 조건부 표시·비활성 상태가 있는 실구현은 포함하고, 상수 false 분기로 도달 불가한 legacy·준비 중 무핸들러 기능은 제외한다. 자동 진입 GET·poll·SCANNING 이어조회, 포커스 이동, 단순 텍스트/툴팁/스크롤은 별도 사용자 역할이 아니다. 네이티브 details, 링크, 열 설정, 복사처럼 공용 자식이 제공하는 실제 사용자 동작은 포함한다. 전역 메뉴·인증·공용 shell은 화면 본문 분모 밖이며 소유 A02다.

행 반복의 jobId/runId/requestId/documentId/candidateId/sourcePlaceId/module/slot/file key/column key/field/index 등은 **인스턴스 구분용 값**이지 코드 접미사가 아니다. 값·문구·검색어·개인/비밀정보를 추적 속성으로 보내라는 뜻이 아니다. 현재 tracing 허용 키에는 인스턴스 메타데이터가 없으므로 이 표의 인스턴스 설명은 명세에서만 사용하고, 전송이 필요하면 별도 허용 계약을 따라야 한다.

같은 동작은 중복 버튼·행·키보드 표면에도 같은 역할을 쓴다(상태 옵션, 닫기 footer/배경, Y-80 행/자세히, 반복 메뉴 입력). 서로 다른 상태 대상을 닫거나 다른 후속 처리를 하는 요소는 분리한다(Y-60 상세 닫기/거절 닫기, Y-10 행 상세/결과 갱신). AntD Select의 옵션·검색·clear, Password의 표시 전환, Popover 바깥 닫기 등 라이브러리 내부 표면은 부모 의미 역할에 포함하며 별도 제품 기능을 만들지 않는다. 실제 form onSubmit은 없어 모든 요청 버튼은 click 동작이며 submit 이벤트를 임의 추가하지 않는다.

종류: `조회`=GET, `로컬`=React/표 설정/clipboard/details 상태, `이동`=내·외부 탐색, `쓰기`=POST/PUT/DELETE. preview도 POST이므로 쓰기다. export GET과 다운로드 발급 POST를 구별한다. 표의 gate는 **현재 프런트 코드 사실**이며 서버 권한/검증의 성공 보장이 아니다. 언급 없는 확인·사유는 없는 것이며 새로 만들지 않는다.


## 원문 분모·조건·제외·한계 (확정 문서 그대로)



| 기존 화면 | 화면 코드 | 고유 역할 수 |
|---|---|---:|
| Y-01 | BO-SYS-010 | 1 |
| Y-10 | BO-SYS-020 | 15 |
| Y-20 | BO-SYS-030 | 4 |
| Y-40 | BO-SYS-050 | 10 |
| Y-50 | BO-SYS-060 | 27 |
| Y-60 | BO-SYS-070 | 30 |
| Y-70 | BO-SYS-080 | 22 |
| Y-80 | BO-SYS-090 | 21 |
| 합계 | 구현 8화면 | 130 |

## 조건 정의와 공용 소유 경계

- **M70:** `y70/index.tsx:461`의 `detail.row.status==='PENDING' && !detail.expired && !detail.documentError`. 입력/제출은 추가로 `submitState==='IDLE'`만 허용한다. `manualDirty`는 `:201`에서 IDLE이며 메뉴 이름/가격 또는 영업시간 또는 사유가 있을 때다. 분류 이름만 바꾸면 dirty로 판정하지 않는 현재 한계가 있다. 제출 시 `:286–309`에서 문서 상태·버전·만료·중복을 다시 보호한다. 400/401/403/409/410/422/429는 REJECTED, 그 외 실패는 UNKNOWN이다. 이전 제출 정보는 문서별 Map에 남아 같은 문서를 다시 열어도 UNKNOWN 재제출을 허용하지 않는다.
- **S80:** `y80/actionSearch.tsx:61–81/115`: enabled, from/to/app 필수; 상관키가 없고 app 문자열이 정확히 `batch`가 아니면 memberId 필요하다(여러 서비스에 batch를 포함하는 것과 다름). 시작<종료, 유효한 KST 시각, 상관키는 `[A-Za-z0-9._:-]{1,128}` 및 최대20분 검사. 상관키가 있으면 memberId는 보내지 않고 `traceId`로 검색하며, 없으면 선택 회원과 optional `requestTraceId`를 쓴다. UI 서비스 선택은 app/member/bo/batch의 중복 없는 정렬 문자열이다. SCANNING은 같은 generation으로 cursor를 자동 조회하고 COMPLETE의 content만 결과에 붙인다. 다음 기록도 같은 조건이다.
- **Y-10 쓰기:** `y10/index.tsx:79–96`에서 canRead가 아닌 enabled/canWrite와 작업·상태 지원 여부를 쓰기 gate로 검사한다. canRead는 목록 조회와 5초 접수 상태 poll의 gate다. pending이 있을 때 명시 같은 요청 GET은 별도 canRead disabled가 없다. 등록 작업의 ON/OFF·주기는 표시뿐이며 토글/편집 기능이 아니다.
- **Y-40 쓰기 결과:** `y40/index.tsx:119–126`의 결정 봉투는 공용 상태 poll, legacy/즉시반영/unknown은 재조회와 안내로 처리한다. 저장/복원 클릭을 실제 적용 성공으로 기록하지 않는다. 기본값 복원에는 별도 confirm 모달이 없으며, 편집 입력과 닫기는 busy로 막혀 있지 않다.
- **Y-50 승인 범위:** `localChangeProcessing.tsx:20–36`의 action은 pending ref로 중복 실행을 막고 오류 때 자동 재전송하지 않으며 password를 finally에서 비운다. 다른 관리자 승인·step-up은 실제 UI/API 흐름 명세일 뿐 이번 작업에서 수행하지 않았다. requesterId는 타입에 있지만 버튼 활성 조건에 비교가 없다. 정책 판본은 빈값에 대한 UI 검사 없이 전송되고, 보호 연결은 JSON 배열까지만 프런트 검사한다. 운영 환경 실제 적용 버튼은 없다.
- **Y-60 선택 변경:** `y60/index.tsx:405–429`에서 후보/필드 변경은 preview를 무효화한다. 상세·거절 모달 닫기나 후보/필드 입력에 busy 차단을 있다고 가정하지 않는다. 거절 사유 UI에는 500자 maxLength가 없으므로 머리 주석의 계약 설명을 구현 gate로 적지 않았다.
- **A02 공용:** `ui/index.tsx:90–105 ActionButton`은 disabledReason이 있으면 실제 disabled 처리하고 이유를 표시한다. `DataTable.tsx:57–64`/`ui/index.tsx:194–209`는 click/Enter/Space 행 활성화와 중첩 컨트롤 제외를 제공한다. `TargetIdentity.tsx:18`의 summary는 행 활성화 제외 selector(`ui/index.tsx:193`)에 명시되지 않아 펼침 클릭과 행 열기가 함께 발생할 수 있다. `Code.tsx:14`의 복사는 전파를 차단한다.
- **A02 닫기:** 공용 Modal은 `components/Modal.tsx:42–46`에서 X 없음/기본 ESC 없음/배경 닫기 허용이다. Y-10은 busy를 onClose에서 차단하고 Y-60은 차단하지 않는다. Y-80 InfoLayer만 `keyboard=true`를 전달한다. Y-70 ProcessPanel은 `:16/24/31–33`에서 busy 중 requestChange를 차단하고 dirty면 inline 버림 확인을 띄운다. 닫기 X·목록으로·ESC는 같은 화면 역할이다. 버림 확인 중에는 본문 inert이며 계속 작성은 이동을 취소한다.
- **A02 자동 조회:** `OperationDecisionStatus.tsx:18–38`는 5초 GET poll만 제공하고 사용자 갱신/재시도 버튼은 없다(Y-40 `:226`, Y-60 `:434`, 도달 불가 Y-50 `:830`). TargetIdentity/useMemberIdentities의 이름 GET도 사용자 역할을 새로 만들지 않는다. 공용 컴포넌트의 구현/카탈로그 갱신 소유는 A02다. 이 문서는 화면 귀속 역할만 공급하며 공용 코드를 수정하지 않는다.

## 미구현·차단·상충: 생성에서 제외할 목록

| 화면/위치 | 현재 사실과 제외 범위 |
|---|---|
| Y-30 / BO-SYS-040 | `generated/ui-catalog/bo-catalog.ts:710–723`: implementation=null, PLANNED. 전용 동작 역할 0개. 공용 Placeholder를 메시지 발송·재시도 기능으로 변환하거나 준비 중 버튼을 생성하지 않는다. |
| Y-01 `y01/index.tsx:1–4/269–271` | 구성 상태 조회와 K-60 이동만 존재한다. 실제 헬스 상세·정책 편집·긴급차단 토글·수동 새로고침은 없다. |
| Y-20 `y20/index.tsx:1–2/71/124–135` | 수동 재발행·재시도는 없다. 설명에는 재시도 횟수·마지막 오류가 있으나 Row/표에는 해당 값이 없다. preview는 행 title 텍스트이며 클릭 상세가 아니다. |
| Y-50 `y50/index.tsx:202/261/289/348/376/415` | `LEGACY_CURATION_READS_READY=false`. summary/unmapped/decision-queue/rule-candidates/business-categories를 조회하지 않는다. 성공 fixture로 gate를 우회하지 않는다. LOCAL 자식 컴포넌트는 이 gate 밖(`:686–687`)이며 위 표에 포함한다. |
| Y-50 `y50/index.tsx:598–659` | status legacy 목록의 검색/분류/지역/초기화/행 상세/더 보기 전체가 렌더 차단이다. 역할 생성 제외. |
| Y-50 `y50/index.tsx:711–791` | legacy 결정 큐 검색/정렬/초기화/행·결정 버튼/페이지가 렌더 차단이다. 역할 생성 제외. |
| Y-50 `y50/index.tsx:793–866/870–968` | decisions 초기값은 빈 배열, candidates=null이고 이를 채우는 legacy 경로가 막혀 있다. 결정/철회/사유·업종·제안 선택/저장/처리 패널 닫기·버림/2단계로 이동/K-20 이동은 코드가 남아도 현재 사용자 경로로 도달할 수 없다. LOCAL 규칙 저장은 이 legacy 상태를 채우지 않는다. 따라서 전용 역할·공용 ProcessPanel 인스턴스 역할 생성에서 제외한다. |
| Y-50 `y50/index.tsx:671–679` | 릴리스 표는 onRowClick이 없어 행 선택·상세·활성화 버튼이 없다. provider/region 필터도 현재 JSX에는 없다. 주석의 필터 설명을 실제 기능으로 생성하지 않는다. |
| Y-50 `localChangeCategories.tsx:29–35/75–76` | LOCAL_CHANGE_SOURCE_ABSENT와 404는 자료 없음/환경 미제공을 구별해 표시한다. 프런트 선택 목록에는 targetEnvironment 라벨이 있으나 LOCAL만 통과시키는 선택 gate는 없다. 서버 허용 범위와 UI gate를 혼동하지 않는다. |
| Y-60 `y60/index.tsx:510–514` | 상호 검색 Input은 항상 disabled·onChange 없음이며 사유가 표시된다. 별도 검색 역할/요청 생성 금지. |
| Y-60 `y60/index.tsx:249/252`; backend `apps/backend/src/main/java/ai/mutuus/backoffice/businesscollection/BusinessInformationCollectionController.java:61–67` | UI requesterType/sort는 변경·GET 전송되지만 Controller 목록 인자는 status/page/size뿐이다. UI 역할은 실재하므로 표에 포함하되 필터·정렬이 서버에서 적용된다고 단정하지 않는다. 목록 keyword는 전송하지 않는다. |
| Y-60 `y60/index.tsx:307–309/483` | memberCandidates는 방어 렌더 분기다. 실제 응답에서 도달 여부는 보장하지 않는다. 코드는 존재하므로 조건부 역할로 포함하고, 확정 성공 fixture를 새로 만들지 않는다. |
| Y-70 `y70/index.tsx:333–334` | 접수 대행·확정 대행·업체 후보 고르기는 disabledReason과 무핸들러 준비 중 버튼이다. 역할 생성 제외. 후보 카드의 선택됨은 읽기 표시다. 실제 동작은 플레이스 보기만 존재한다. |
| Y-70 `y70/index.tsx:241–269` | source/image/draft 410은 보존 만료로 분리한다. 이미지는 표시만 하며 확대·다운로드 동작이 없다. 수동 초안은 사장님 확인 대기까지만 저장하며 메뉴 확정·사장 대행 반영은 없다. |
| Y-80 `y80/index.tsx:61` | 환경은 Tag 표시뿐이며 환경 변경 Select가 없다. 파일 날짜는 UTC, 업무 검색은 KST다. 다음 파일은 페이지 교체이고 다음 기록은 이어붙임이다. |
| Y-50 export `localChangeProcessing.tsx:34`; Y-70 image `y70/index.tsx:252` | 직접 fetch여서 공용 api UI context 전파가 적용된 것으로 볼 수 없다. UI role·generated 상수를 공급하는 것만으로 transport 추적이 완성되지는 않는다. |

동작 정의에 대한 열린 질문: **0개**. 상기 상충은 현재 소스 사실로 명시했으며 숨겨진 정상 동작을 추정하지 않았다. 요소 정본 코드/생성 상수의 발급과 공용 tracing 등록은 A02 후속 공급 범위다. 이 역할표는 기존 생성 요소가 있다는 선언이 아니다.

## 검증 범위

검증 이유: 소스의 사용자 동작 요소가 역할표 또는 명시적 제외 사유로 빠짐없이 추적되는지, 임의 생성 코드가 섞이지 않았는지 확인하기 위해 정적 대조 1회를 수행했다. 결과는 130개 고유 역할·화면 매핑·참조 위치·생성 상수 부재 검사 통과, 수동 이벤트 대조 누락 0건이다. 대상은 8개 index와 Y-50 두 자식/Y-80 ActionSearch 및 표에서 참조한 공용 컴포넌트다. 소스 이벤트·링크·summary, 역할 키의 형식/중복, 화면 매핑, 생성 상수 부재를 대조했다. 결과 증거는 `TEAM_OUT_DIR/sys-roles-static.txt`에 둔다.

브라우저·캡처·axe·시험·채점·mutation은 실행하지 않는다. 런타임 렌더링·서버 gate·실데이터 결과·다운로드 성공·클립보드 성공·AntD 내부 DOM별 추적은 확인하지 않는다. 이번 결과는 소스 기반 생성기 입력 MD 패치에 한정한다.

