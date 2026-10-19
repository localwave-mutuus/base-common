# 회원 원문 → 요소 코드 소유팀 검토표

확정 조직 revision: `bab24fe0f7270738e7871c2f94d7e2dded31b630`; 원문 SHA-256: `bb0acebfe60cb29ab954565015cf5cc989ea6b6eb85ffdc98cf4de28e46acda2`.
제품 고정 aa0d4f9c65fb4306d09f58ad08e159683d4b40ca. 86fea80은 통합 근거로 사용하지 않았다.
논리 분모292, 등록152, 미등록140. sourceColumns에 원문8열 전부 보존.

원문 논리 역할 한 행은 한 코드; 같은 화면의 반복 대상·동일 행동은 합치고 화면이 다르면 별도 귀속. M-50 동일 testId의 상세 열기와 확인 실행은 분리. 대상 값·ID는 코드에 넣지 않는다.

원문 별도 gate 명세 미공급; 단순조회·위험조치 단계 분류와 동작설명만 보존하며 권한·서버 처리 성공을 추론하지 않는다.

정본 owner A08은 유지한다. 역할 공급 A11과 소유권 변경은 구분한다. 아래는 A11/A08 검토용이며 소유팀 승인 완료를 뜻하지 않는다.

| 화면 | 분모 | 등록 | 미등록 | 소유팀 검토 |
|---|---:|---:|---:|---|
| M-01 | 37 | 14 | 23 | A11 공급/A08 정본, 승인 미확인 |
| M-02 | 34 | 15 | 19 | A11 공급/A08 정본, 승인 미확인 |
| M-10 | 30 | 16 | 14 | A11 공급/A08 정본, 승인 미확인 |
| M-11 | 16 | 10 | 6 | A11 공급/A08 정본, 승인 미확인 |
| M-20 | 29 | 16 | 13 | A11 공급/A08 정본, 승인 미확인 |
| M-30 | 48 | 15 | 33 | A11 공급/A08 정본, 승인 미확인 |
| M-40 | 52 | 33 | 19 | A11 공급/A08 정본, 승인 미확인 |
| M-50 | 46 | 33 | 13 | A11 공급/A08 정본, 승인 미확인 |

| 원문 역할 | 정본 화면 | 명시 코드 | locator 근거 | 동작·단계 | 공유 범위 | 등록/미등록 사유 |
|---|---|---|---|---|---|---|
| M-01.search-members | BO-MBR-010 | 미등록 | screens/m01/index.tsx:86 / filter-q | 두 글자 이상 검색어 입력·지우기, IME 완료 후 조회; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-01.filter-status | BO-MBR-010 | 미등록 | screens/m01/index.tsx:86 / filter-status | 전체·활성·정지·탈퇴·가입 대기 상태 선택; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-01.reset-filters | BO-MBR-010 | 미등록 | screens/m01/index.tsx:86 / filter-reset | 검색 조건 초기화; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-01.reload-members | BO-MBR-010 | 미등록 | screens/m01/index.tsx:90 | 오류 후 목록 다시 불러오기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-member | BO-MBR-010 | 미등록 | screens/m01/index.tsx:66,92 / member-row-{id} | 행·자세히 보기로 회원 또는 가입 대기 패널 선택; 수정키 링크는 상세 이동; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-01.change-page | BO-MBR-010 | 미등록 | screens/m01/index.tsx:93 | 페이지 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.change-page-size | BO-MBR-010 | 미등록 | screens/m01/index.tsx:93 | 20·50명 표시 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-full-detail | BO-MBR-010 | 미등록 | screens/m01/index.tsx:95 | 회원 전체 상세 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-application | BO-MBR-010 | 미등록 | screens/m01/index.tsx:95; screens/m01/PendingSignupPanel.tsx:48 | 가입 대기 신청의 M-11 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-pii | BO-MBR-010 | BO-MBR-010-OPEN-PII | screens/m02/index.tsx:238,419 / pii-open, panel-pii-open | 연락처 사유 입력 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.input-pii-reason | BO-MBR-010 | BO-MBR-010-INPUT-PII-REASON | screens/m02/index.tsx:434 / pii-reason | 연락처 열람 사유 입력; 위험조치 입력 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.submit-pii | BO-MBR-010 | BO-MBR-010-SUBMIT-PII | screens/m02/index.tsx:205 / pii-submit | 사유를 포함한 연락처 열람 요청; 위험조치 실행 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.close-pii | BO-MBR-010 | 미등록 | screens/m02/index.tsx:204 | 연락처 열람 단계 취소; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-member-number | BO-MBR-010 | 미등록 | screens/m02/index.tsx:233,418 | 회원 번호 details 펼치기·접기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-previous-member-number | BO-MBR-010 | 미등록 | screens/m02/index.tsx:415 | 이전 요청 회원 번호 펼치기·접기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-unsuspend | BO-MBR-010 | BO-MBR-010-OPEN-UNSUSPEND | screens/m02/index.tsx:410 / action-이용 정지 해제 | 이용 정지 해제 검토 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.open-revoke-sessions | BO-MBR-010 | BO-MBR-010-OPEN-REVOKE-SESSIONS | screens/m02/index.tsx:410 / action-모든 로그인 종료 | 세션 회수 검토 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.open-suspend | BO-MBR-010 | BO-MBR-010-OPEN-SUSPEND | screens/m02/index.tsx:410 / action-이용 정지 | 이용 정지 검토 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.open-audit | BO-MBR-010 | 미등록 | screens/m02/index.tsx:420 | 현재 회원 작업 이력 Z-30 이동; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-security-info | BO-MBR-010 | 미등록 | screens/m02/index.tsx:462 | 인증·보안 정보 레이어 열기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.open-action-history | BO-MBR-010 | 미등록 | screens/m02/index.tsx:462 | 조치 이력 정보 레이어 열기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.close-action | BO-MBR-010 | BO-MBR-010-CLOSE-ACTION | screens/m02/MemberActionModal.tsx:269,274 / action-close | 취소 또는 결과 확인 후 조치 단계 닫기; 단순조회 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.next-action | BO-MBR-010 | BO-MBR-010-NEXT-ACTION | screens/m02/MemberActionModal.tsx:278 / action-next | 사유 작성 뒤 본인확인 단계로 이동; 위험조치 진입 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.verify-action | BO-MBR-010 | BO-MBR-010-VERIFY-ACTION | screens/m02/MemberActionModal.tsx:283 / action-verify | 본인확인 후 영향 미리보기 요청; 위험조치 실행 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.execute-action | BO-MBR-010 | BO-MBR-010-EXECUTE-ACTION | screens/m02/MemberActionModal.tsx:288 / action-execute | 현재 선택 조치 실행 요청; 위험조치 실행 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.select-action-reason | BO-MBR-010 | BO-MBR-010-SELECT-ACTION-REASON | screens/m02/MemberActionModal.tsx:316 / action-reason-code | 조치 사유 코드 선택; 위험조치 입력 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.input-action-reason | BO-MBR-010 | BO-MBR-010-INPUT-ACTION-REASON | screens/m02/MemberActionModal.tsx:327 / action-reason-text | 조치 사유 입력; 위험조치 입력 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.input-action-password | BO-MBR-010 | BO-MBR-010-INPUT-ACTION-PASSWORD | screens/m02/MemberActionModal.tsx:338 / action-password | 관리자 본인확인 입력; 위험조치 입력 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.confirm-action-target | BO-MBR-010 | BO-MBR-010-CONFIRM-ACTION-TARGET | screens/m02/MemberActionModal.tsx:373 / action-confirmation-input | 미리보기 확인 문구 입력; 위험조치 확인 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.open-pii | BO-MBR-020 | BO-MBR-020-OPEN-PII | screens/m02/index.tsx:238,419 / pii-open, panel-pii-open | 연락처 사유 입력 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.input-pii-reason | BO-MBR-020 | BO-MBR-020-INPUT-PII-REASON | screens/m02/index.tsx:434 / pii-reason | 연락처 열람 사유 입력; 위험조치 입력 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.submit-pii | BO-MBR-020 | BO-MBR-020-SUBMIT-PII | screens/m02/index.tsx:205 / pii-submit | 사유를 포함한 연락처 열람 요청; 위험조치 실행 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.close-pii | BO-MBR-020 | 미등록 | screens/m02/index.tsx:204 | 연락처 열람 단계 취소; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-member-number | BO-MBR-020 | 미등록 | screens/m02/index.tsx:233,418 | 회원 번호 details 펼치기·접기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-previous-member-number | BO-MBR-020 | 미등록 | screens/m02/index.tsx:415 | 이전 요청 회원 번호 펼치기·접기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-voucher | BO-MBR-020 | 미등록 | screens/m02/index.tsx:323 | 바우처 T-21 상세 이동; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-trade | BO-MBR-020 | 미등록 | screens/m02/index.tsx:344 | 거래 T-31 상세 이동; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-inquiry | BO-MBR-020 | 미등록 | screens/m02/index.tsx:365 | 문의 C-11 상세 이동; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-dispute | BO-MBR-020 | 미등록 | screens/m02/index.tsx:381 | 분쟁 C-21 상세 이동; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-unsuspend | BO-MBR-020 | BO-MBR-020-OPEN-UNSUSPEND | screens/m02/index.tsx:410 / action-이용 정지 해제 | 이용 정지 해제 검토 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.open-revoke-sessions | BO-MBR-020 | BO-MBR-020-OPEN-REVOKE-SESSIONS | screens/m02/index.tsx:410 / action-모든 로그인 종료 | 세션 회수 검토 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.open-suspend | BO-MBR-020 | BO-MBR-020-OPEN-SUSPEND | screens/m02/index.tsx:410 / action-이용 정지 | 이용 정지 검토 단계 열기; 위험조치 진입 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.open-audit | BO-MBR-020 | 미등록 | screens/m02/index.tsx:420 | 현재 회원 작업 이력 Z-30 이동; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-security-info | BO-MBR-020 | 미등록 | screens/m02/index.tsx:462 | 인증·보안 정보 레이어 열기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-action-history | BO-MBR-020 | 미등록 | screens/m02/index.tsx:462 | 조치 이력 정보 레이어 열기; 단순조회 | MemberWorkspace 공유; M-01 embedded / M-02 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.close-action | BO-MBR-020 | BO-MBR-020-CLOSE-ACTION | screens/m02/MemberActionModal.tsx:269,274 / action-close | 취소 또는 결과 확인 후 조치 단계 닫기; 단순조회 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.next-action | BO-MBR-020 | BO-MBR-020-NEXT-ACTION | screens/m02/MemberActionModal.tsx:278 / action-next | 사유 작성 뒤 본인확인 단계로 이동; 위험조치 진입 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.verify-action | BO-MBR-020 | BO-MBR-020-VERIFY-ACTION | screens/m02/MemberActionModal.tsx:283 / action-verify | 본인확인 후 영향 미리보기 요청; 위험조치 실행 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.execute-action | BO-MBR-020 | BO-MBR-020-EXECUTE-ACTION | screens/m02/MemberActionModal.tsx:288 / action-execute | 현재 선택 조치 실행 요청; 위험조치 실행 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.select-action-reason | BO-MBR-020 | BO-MBR-020-SELECT-ACTION-REASON | screens/m02/MemberActionModal.tsx:316 / action-reason-code | 조치 사유 코드 선택; 위험조치 입력 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.input-action-reason | BO-MBR-020 | BO-MBR-020-INPUT-ACTION-REASON | screens/m02/MemberActionModal.tsx:327 / action-reason-text | 조치 사유 입력; 위험조치 입력 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.input-action-password | BO-MBR-020 | BO-MBR-020-INPUT-ACTION-PASSWORD | screens/m02/MemberActionModal.tsx:338 / action-password | 관리자 본인확인 입력; 위험조치 입력 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.confirm-action-target | BO-MBR-020 | BO-MBR-020-CONFIRM-ACTION-TARGET | screens/m02/MemberActionModal.tsx:373 / action-confirmation-input | 미리보기 확인 문구 입력; 위험조치 확인 | MemberWorkspace → MemberActionModal 공유; 선택 조치별 같은 컨트롤 재사용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-02.select-member-tab | BO-MBR-020 | 미등록 | screens/m02/index.tsx:91-99,213 | 기본정보·인증보안·임직원·사업장·자산·거래·민원·조치 탭 선택; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.back-to-members | BO-MBR-020 | 미등록 | screens/m02/index.tsx:467,482 | 회원 찾기로 이동; 두 버튼 동일 역할; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.reload-member | BO-MBR-020 | 미등록 | screens/m02/index.tsx:470 | 회원 조회 재시도; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.select-search-member | BO-MBR-020 | 미등록 | screens/m02/index.tsx:492 | 검색 결과 회원 상세 이동; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.open-member-process | BO-MBR-020 | BO-MBR-020-OPEN-MEMBER-PROCESS | screens/m02/index.tsx:515,520 / member-process-open | 조치 검토 링크·회원 처리 버튼으로 처리 패널 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.reload-applications | BO-MBR-030 | 미등록 | screens/m10/index.tsx:76 | 목록 조회 재시도; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.open-application | BO-MBR-030 | 미등록 | screens/m10/index.tsx:85 | 행·신청 보기로 ApplicationPanel 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.change-page | BO-MBR-030 | 미등록 | screens/m10/index.tsx:87 | 목록 페이지 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.change-page-size | BO-MBR-030 | 미등록 | screens/m10/index.tsx:87 | 목록 표시 수 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.open-full-detail | BO-MBR-030 | BO-MBR-030-OPEN-FULL-DETAIL | screens/m11/panel.tsx:19 / application-full-detail | M-11 전체 상세 열기; 단순조회 | ApplicationPanel 공유 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.reload-applications | BO-MBR-050 | 미등록 | screens/m20/index.tsx:54 | 목록 조회 재시도; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.open-application | BO-MBR-050 | 미등록 | screens/m20/index.tsx:61 | 행·신청 보기로 ApplicationPanel 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.change-page | BO-MBR-050 | 미등록 | screens/m20/index.tsx:63 | 목록 페이지 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.change-page-size | BO-MBR-050 | 미등록 | screens/m20/index.tsx:63 | 목록 표시 수 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.open-full-detail | BO-MBR-050 | BO-MBR-050-OPEN-FULL-DETAIL | screens/m11/panel.tsx:19 / application-full-detail | M-11 전체 상세 열기; 단순조회 | ApplicationPanel 공유 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.select-status-tab | BO-MBR-030 | 미등록 | screens/m10/index.tsx:43,73 | 심사 대기·승인·반려·전체 탭 선택; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.reload-application | BO-MBR-030 | 미등록 | screens/m11/index.tsx:146,160 | 오류 재시도·새로 고침으로 신청 상세 조회; 동일 load; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.open-pii | BO-MBR-030 | BO-MBR-030-OPEN-PII | screens/m11/index.tsx:160 / pii-open | 연락처 열람 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-approve | BO-MBR-030 | BO-MBR-030-OPEN-APPROVE | screens/m11/index.tsx:166 / approve-open | 가입 승인 요청 확인 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-reject | BO-MBR-030 | BO-MBR-030-OPEN-REJECT | screens/m11/index.tsx:166 / reject-open | 가입 반려 요청 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.refresh-decision | BO-MBR-030 | BO-MBR-030-REFRESH-DECISION | screens/m11/index.tsx:167,172 / result-refresh | 결정 기록 재조회; 조회 중단 시 같은 역할 비활성; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.close-pii | BO-MBR-030 | 미등록 | screens/m11/index.tsx:175 | 연락처 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.input-pii-reason | BO-MBR-030 | BO-MBR-030-INPUT-PII-REASON | screens/m11/index.tsx:175 / pii-reason | 개인정보 열람 사유 입력; 위험조치 입력 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.submit-pii | BO-MBR-030 | BO-MBR-030-SUBMIT-PII | screens/m11/index.tsx:175 / pii-submit | 연락처 열람 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.close-approve | BO-MBR-030 | 미등록 | screens/m11/index.tsx:176 | 승인 확인 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.submit-approve | BO-MBR-030 | BO-MBR-030-SUBMIT-APPROVE | screens/m11/index.tsx:176 / approve-submit | 가입 승인 결정 접수 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.close-reject | BO-MBR-030 | 미등록 | screens/m11/index.tsx:177 | 반려 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.input-reject-reason | BO-MBR-030 | BO-MBR-030-INPUT-REJECT-REASON | screens/m11/index.tsx:177 / reject-reason | 반려 사유 입력; 위험조치 입력 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.submit-reject | BO-MBR-030 | BO-MBR-030-SUBMIT-REJECT | screens/m11/index.tsx:177 / reject-submit | 가입 반려 결정 접수 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-rejection-text | BO-MBR-030 | BO-MBR-030-OPEN-REJECTION-TEXT | screens/m11/index.tsx:159 / rejection-full-open | 반려 사유 전체 보기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-verification-info | BO-MBR-030 | BO-MBR-030-OPEN-VERIFICATION-INFO | screens/m11/index.tsx:178 / application-info-verification | 확인 기록 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-delegation-info | BO-MBR-030 | BO-MBR-030-OPEN-DELEGATION-INFO | screens/m11/index.tsx:178 / application-info-delegation | 위임장 기록 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-terms-info | BO-MBR-030 | BO-MBR-030-OPEN-TERMS-INFO | screens/m11/index.tsx:178 / application-info-terms | 이용약관 기록 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-document-info | BO-MBR-030 | BO-MBR-030-OPEN-DOCUMENT-INFO | screens/m11/index.tsx:178 / application-info-document | 서류 정보 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-10.open-history-info | BO-MBR-030 | BO-MBR-030-OPEN-HISTORY-INFO | screens/m11/index.tsx:178 / application-info-history | 이전 결정 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.reload-application | BO-MBR-050 | 미등록 | screens/m11/index.tsx:146,160 | 오류 재시도·새로 고침으로 신청 상세 조회; 동일 load; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.open-pii | BO-MBR-050 | BO-MBR-050-OPEN-PII | screens/m11/index.tsx:160 / pii-open | 연락처 열람 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-approve | BO-MBR-050 | BO-MBR-050-OPEN-APPROVE | screens/m11/index.tsx:166 / approve-open | 가입 승인 요청 확인 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-reject | BO-MBR-050 | BO-MBR-050-OPEN-REJECT | screens/m11/index.tsx:166 / reject-open | 가입 반려 요청 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.refresh-decision | BO-MBR-050 | BO-MBR-050-REFRESH-DECISION | screens/m11/index.tsx:167,172 / result-refresh | 결정 기록 재조회; 조회 중단 시 같은 역할 비활성; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.close-pii | BO-MBR-050 | 미등록 | screens/m11/index.tsx:175 | 연락처 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.input-pii-reason | BO-MBR-050 | BO-MBR-050-INPUT-PII-REASON | screens/m11/index.tsx:175 / pii-reason | 개인정보 열람 사유 입력; 위험조치 입력 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.submit-pii | BO-MBR-050 | BO-MBR-050-SUBMIT-PII | screens/m11/index.tsx:175 / pii-submit | 연락처 열람 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.close-approve | BO-MBR-050 | 미등록 | screens/m11/index.tsx:176 | 승인 확인 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.submit-approve | BO-MBR-050 | BO-MBR-050-SUBMIT-APPROVE | screens/m11/index.tsx:176 / approve-submit | 가입 승인 결정 접수 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.close-reject | BO-MBR-050 | 미등록 | screens/m11/index.tsx:177 | 반려 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.input-reject-reason | BO-MBR-050 | BO-MBR-050-INPUT-REJECT-REASON | screens/m11/index.tsx:177 / reject-reason | 반려 사유 입력; 위험조치 입력 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.submit-reject | BO-MBR-050 | BO-MBR-050-SUBMIT-REJECT | screens/m11/index.tsx:177 / reject-submit | 가입 반려 결정 접수 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-rejection-text | BO-MBR-050 | BO-MBR-050-OPEN-REJECTION-TEXT | screens/m11/index.tsx:159 / rejection-full-open | 반려 사유 전체 보기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-verification-info | BO-MBR-050 | BO-MBR-050-OPEN-VERIFICATION-INFO | screens/m11/index.tsx:178 / application-info-verification | 확인 기록 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-delegation-info | BO-MBR-050 | BO-MBR-050-OPEN-DELEGATION-INFO | screens/m11/index.tsx:178 / application-info-delegation | 위임장 기록 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-terms-info | BO-MBR-050 | BO-MBR-050-OPEN-TERMS-INFO | screens/m11/index.tsx:178 / application-info-terms | 이용약관 기록 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-document-info | BO-MBR-050 | BO-MBR-050-OPEN-DOCUMENT-INFO | screens/m11/index.tsx:178 / application-info-document | 서류 정보 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-20.open-history-info | BO-MBR-050 | BO-MBR-050-OPEN-HISTORY-INFO | screens/m11/index.tsx:178 / application-info-history | 이전 결정 정보 레이어 열기; 단순조회 | ApplicationReview embedded | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.reload-application | BO-MBR-040 | 미등록 | screens/m11/index.tsx:146,160 | 오류 재시도·새로 고침으로 신청 상세 조회; 동일 load; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-11.open-pii | BO-MBR-040 | BO-MBR-040-OPEN-PII | screens/m11/index.tsx:160 / pii-open | 연락처 열람 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.open-approve | BO-MBR-040 | BO-MBR-040-OPEN-APPROVE | screens/m11/index.tsx:166 / approve-open | 가입 승인 요청 확인 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.open-reject | BO-MBR-040 | BO-MBR-040-OPEN-REJECT | screens/m11/index.tsx:166 / reject-open | 가입 반려 요청 단계 열기; 위험조치 진입 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.refresh-decision | BO-MBR-040 | BO-MBR-040-REFRESH-DECISION | screens/m11/index.tsx:167,172 / result-refresh | 결정 기록 재조회; 조회 중단 시 같은 역할 비활성; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.close-pii | BO-MBR-040 | 미등록 | screens/m11/index.tsx:175 | 연락처 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-11.input-pii-reason | BO-MBR-040 | BO-MBR-040-INPUT-PII-REASON | screens/m11/index.tsx:175 / pii-reason | 개인정보 열람 사유 입력; 위험조치 입력 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.submit-pii | BO-MBR-040 | BO-MBR-040-SUBMIT-PII | screens/m11/index.tsx:175 / pii-submit | 연락처 열람 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.close-approve | BO-MBR-040 | 미등록 | screens/m11/index.tsx:176 | 승인 확인 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-11.submit-approve | BO-MBR-040 | BO-MBR-040-SUBMIT-APPROVE | screens/m11/index.tsx:176 / approve-submit | 가입 승인 결정 접수 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.close-reject | BO-MBR-040 | 미등록 | screens/m11/index.tsx:177 | 반려 단계 닫기; 단순조회 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-11.input-reject-reason | BO-MBR-040 | BO-MBR-040-INPUT-REJECT-REASON | screens/m11/index.tsx:177 / reject-reason | 반려 사유 입력; 위험조치 입력 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.submit-reject | BO-MBR-040 | BO-MBR-040-SUBMIT-REJECT | screens/m11/index.tsx:177 / reject-submit | 가입 반려 결정 접수 요청; 위험조치 실행 | ApplicationReview 공유; M-10/M-20 embedded, M-11 전체 상세 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.back-to-list | BO-MBR-040 | BO-MBR-040-BACK-TO-LIST | screens/m11/index.tsx:145,168 / back-to-m10 | 원래 목록으로; 중복 버튼 동일 역할; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-11.jump-to-decision | BO-MBR-040 | 미등록 | screens/m11/index.tsx:148 | 가입 결정 검토 영역으로 초점·스크롤 이동; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-application | BO-MBR-070 | 미등록 | screens/m30/index.tsx:91,107 / m30-row-{id} | 행·신청 확인으로 심사 패널 열기; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-30.open-reject | BO-MBR-070 | BO-MBR-070-OPEN-REJECT | screens/m30/index.tsx:98 / m30-reject-open | 탈퇴 신청 반려 단계 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.open-approve | BO-MBR-070 | BO-MBR-070-OPEN-APPROVE | screens/m30/index.tsx:98 / m30-approve-open | 탈퇴 승인 단계 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.close-decision | BO-MBR-070 | 미등록 | screens/m30/index.tsx:98 | 심사 사유 단계 닫기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.submit-decision | BO-MBR-070 | BO-MBR-070-SUBMIT-DECISION | screens/m30/index.tsx:98 / m30-decision-submit | 선택된 승인·반려 요청 전송; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.input-decision-reason | BO-MBR-070 | BO-MBR-070-INPUT-DECISION-REASON | screens/m30/index.tsx:98 / m30-decision-reason | 심사 사유 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.continue-writing | BO-MBR-070 | 미등록 | screens/m30/index.tsx:100 | 페이지 이동 취소; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.discard-writing | BO-MBR-070 | 미등록 | screens/m30/index.tsx:100 | 입력 버리고 예약 이동 수행; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.select-review-area | BO-MBR-070 | BO-MBR-070-SELECT-REVIEW-AREA | screens/m30/index.tsx:101 / m30-area-review | 탈퇴 신청 심사 영역 선택; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.select-privacy-area | BO-MBR-070 | BO-MBR-070-SELECT-PRIVACY-AREA | screens/m30/index.tsx:101 / m30-area-privacy | 탈퇴 개인정보 정리 영역 선택; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.select-status-tab | BO-MBR-070 | 미등록 | screens/m30/index.tsx:104 / m30-tab-{key} | 심사 대기·승인·반려·전체 탭 선택; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-30.reload-applications | BO-MBR-070 | 미등록 | screens/m30/index.tsx:106 | 목록 조회 재시도; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.change-page | BO-MBR-070 | 미등록 | screens/m30/index.tsx:108 | 페이지 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.change-page-size | BO-MBR-070 | 미등록 | screens/m30/index.tsx:108 | 20·50건 표시 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-full-detail | BO-MBR-070 | 미등록 | screens/m30/index.tsx:114 | 선택 심사의 전체 상세 새 창 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-review-info | BO-MBR-070 | 미등록 | screens/m30/index.tsx:114 | 심사 기록 정보 레이어 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-privacy-process | BO-MBR-070 | BO-MBR-070-OPEN-PRIVACY-PROCESS | screens/m30/WithdrawnPrivacyPanel.tsx:180 / m30-privacy-select | 정리 대상 선택 패널 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.select-privacy-member | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:140 / m30-member-picker | 탈퇴 회원 검색·선택; 위험조치 입력 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-30.input-privacy-reason | BO-MBR-070 | BO-MBR-070-INPUT-PRIVACY-REASON | screens/m30/WithdrawnPrivacyPanel.tsx:142 / m30-privacy-reason | 정리 사유 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.input-privacy-password | BO-MBR-070 | BO-MBR-070-INPUT-PRIVACY-PASSWORD | screens/m30/WithdrawnPrivacyPanel.tsx:143 / m30-privacy-password | 관리자 본인확인 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.preview-privacy | BO-MBR-070 | BO-MBR-070-PREVIEW-PRIVACY | screens/m30/WithdrawnPrivacyPanel.tsx:144 / m30-privacy-preview | 본인확인 후 대상·영향 조회 및 요청 생성; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.select-privacy-request | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:146 / m30-request-picker | 기존 정리 요청 검색·선택; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-30.load-privacy-approval | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:149 | 대상·승인 상태 조회; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.approve-privacy | BO-MBR-070 | BO-MBR-070-APPROVE-PRIVACY | screens/m30/WithdrawnPrivacyPanel.tsx:159 / m30-privacy-approve | 다른 관리자 본인확인 후 승인 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.confirm-privacy-target | BO-MBR-070 | BO-MBR-070-CONFIRM-PRIVACY-TARGET | screens/m30/WithdrawnPrivacyPanel.tsx:160 / m30-privacy-confirm-target | 현재 정리 대상·영향 확인 체크; 위험조치 확인 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.execute-privacy | BO-MBR-070 | BO-MBR-070-EXECUTE-PRIVACY | screens/m30/WithdrawnPrivacyPanel.tsx:161 / m30-privacy-execute | 개인정보 단건 정리 실행 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.read-privacy-result | BO-MBR-070 | BO-MBR-070-READ-PRIVACY-RESULT | screens/m30/WithdrawnPrivacyPanel.tsx:162 / m30-privacy-result | 동일 요청 처리 기록 조회; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.reset-privacy-target | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:163 | 대조 후 다른 대상 조회로 초기화; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-privacy-full-detail | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:180 | 현재 개인정보 정리 상세 새 창 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-privacy-info | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:180 | 정리 근거·승인 기록 레이어 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-privacy-member-number | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:180 | 현재·이전 대상 확인 번호 details 펼치기·접기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.continue-privacy-writing | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:180 | 직접 진입 페이지 이동 취소; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.discard-privacy-writing | BO-MBR-070 | 미등록 | screens/m30/WithdrawnPrivacyPanel.tsx:180 | 직접 진입 입력 버리고 이동; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.open-place | BO-MBR-060 | 미등록 | screens/m40/index.tsx:452,554 | 행·자세히 보기로 지정 업체 처리 패널 열기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.filter-region | BO-MBR-060 | BO-MBR-060-FILTER-REGION | screens/m40/index.tsx:526 / m40-filter-region | 지역 검색 입력·지우기; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.filter-status | BO-MBR-060 | BO-MBR-060-FILTER-STATUS | screens/m40/index.tsx:529 / m40-filter-status | 업체 대표 상태 선택; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.reset-filters | BO-MBR-060 | BO-MBR-060-RESET-FILTERS | screens/m40/index.tsx:538 / filter-reset | 지역·상태 조건 초기화; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.previous-page | BO-MBR-060 | BO-MBR-060-PREVIOUS-PAGE | screens/m40/index.tsx:565 / m40-page-prev | 이전 목록 페이지; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.next-page | BO-MBR-060 | BO-MBR-060-NEXT-PAGE | screens/m40/index.tsx:568 / m40-page-next | 다음 목록 페이지; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.open-activity | BO-MBR-060 | BO-MBR-060-OPEN-ACTIVITY | screens/m40/index.tsx:592 / m40-activity-open | 활동 기록 작성 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.open-status | BO-MBR-060 | BO-MBR-060-OPEN-STATUS | screens/m40/index.tsx:595 / m40-status-open | 상태 변경 작성 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.select-link-member | BO-MBR-060 | 미등록 | screens/m40/index.tsx:617 / m40-member-picker | 가입 완료 회원 검색·선택; 위험조치 입력 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-40.preview-link | BO-MBR-060 | BO-MBR-060-PREVIEW-LINK | screens/m40/index.tsx:618 / m40-link-preview | 회원 연결 영향 미리보기; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.execute-link | BO-MBR-060 | BO-MBR-060-EXECUTE-LINK | screens/m40/index.tsx:621 / m40-link-execute | 확인한 회원 연결 실행 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.input-link-reason | BO-MBR-060 | BO-MBR-060-INPUT-LINK-REASON | screens/m40/index.tsx:666 / m40-link-reason | 회원 연결 사유 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.close-activity | BO-MBR-060 | 미등록 | screens/m40/index.tsx:685 | 활동 기록 작성 닫기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.submit-activity | BO-MBR-060 | BO-MBR-060-SUBMIT-ACTIVITY | screens/m40/index.tsx:688 / m40-activity-submit | 활동 기록 저장 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.select-activity-type | BO-MBR-060 | BO-MBR-060-SELECT-ACTIVITY-TYPE | screens/m40/index.tsx:699 / m40-activity-type-{value} | 방문·전화·메모 종류 선택; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.input-activity-summary | BO-MBR-060 | BO-MBR-060-INPUT-ACTIVITY-SUMMARY | screens/m40/index.tsx:707 / m40-activity-summary | 활동 요약 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.input-activity-reason | BO-MBR-060 | BO-MBR-060-INPUT-ACTIVITY-REASON | screens/m40/index.tsx:711 / m40-activity-reason | 활동 사유 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.close-status | BO-MBR-060 | 미등록 | screens/m40/index.tsx:721 | 상태 변경 단계 닫기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.submit-status | BO-MBR-060 | BO-MBR-060-SUBMIT-STATUS | screens/m40/index.tsx:724 / m40-status-submit | 선택 상태 변경 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.select-target-status | BO-MBR-060 | BO-MBR-060-SELECT-TARGET-STATUS | screens/m40/index.tsx:735 / m40-status-{value} | 변경할 상태 선택; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.input-status-reason | BO-MBR-060 | BO-MBR-060-INPUT-STATUS-REASON | screens/m40/index.tsx:743 / m40-status-reason | 상태 변경 사유 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.open-full-text | BO-MBR-060 | 미등록 | screens/m40/index.tsx:666,707,711,743 | 작성 사유·요약 전체 보기; 동일 showFull; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.open-nominators-info | BO-MBR-060 | BO-MBR-060-OPEN-NOMINATORS-INFO | screens/m40/index.tsx:749 / m40-info-nominators | 지정 근거 보기; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.open-activities-info | BO-MBR-060 | BO-MBR-060-OPEN-ACTIVITIES-INFO | screens/m40/index.tsx:749 / m40-info-activities | 활동 기록 보기; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.open-invite | BO-MBR-060 | BO-MBR-060-OPEN-INVITE | screens/m40/signupInvite.tsx:116 / invite-panel-open | 문자 시험 패널 열기; 위험조치 진입 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.select-invite-target | BO-MBR-060 | BO-MBR-060-SELECT-INVITE-TARGET | screens/m40/signupInvite.tsx:123 / invite-target-{id} | 현재 목록 문자 대상 선택; 위험조치 입력 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.select-invite-template | BO-MBR-060 | BO-MBR-060-SELECT-INVITE-TEMPLATE | screens/m40/signupInvite.tsx:125 / invite-template | 활성 문자 문구 선택; 위험조치 입력 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.preview-invite | BO-MBR-060 | BO-MBR-060-PREVIEW-INVITE | screens/m40/signupInvite.tsx:127 / invite-preview | 문구·제외 대상 미리보기; 단순조회 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.review-invite-send | BO-MBR-060 | BO-MBR-060-REVIEW-INVITE-SEND | screens/m40/signupInvite.tsx:127 / invite-send | 문자 시험 확인 단계 열기; 위험조치 진입 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.input-optout-reason | BO-MBR-060 | BO-MBR-060-INPUT-OPTOUT-REASON | screens/m40/signupInvite.tsx:129 / invite-optout-reason | 수신 제외·해제 사유 입력; 위험조치 입력 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.review-optout | BO-MBR-060 | BO-MBR-060-REVIEW-OPTOUT | screens/m40/signupInvite.tsx:130 / invite-optout | 수신 제외 설정 확인 단계 열기; 위험조치 진입 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.review-optout-clear | BO-MBR-060 | BO-MBR-060-REVIEW-OPTOUT-CLEAR | screens/m40/signupInvite.tsx:130 / invite-optout-clear | 수신 제외 해제 검토 열기; 최종 확인 비활성; 위험조치 진입 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.refresh-invite-history | BO-MBR-060 | BO-MBR-060-REFRESH-INVITE-HISTORY | screens/m40/signupInvite.tsx:133 / invite-refresh | 문자 이력 다시 확인; 단순조회 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.review-invite-retry | BO-MBR-060 | BO-MBR-060-REVIEW-INVITE-RETRY | screens/m40/signupInvite.tsx:135 / invite-retry-{id} | 실패 요청 다시 요청 검토; 위험조치 진입 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.review-invite-check | BO-MBR-060 | BO-MBR-060-REVIEW-INVITE-CHECK | screens/m40/signupInvite.tsx:135 / invite-check-{id} | 불명·전송 중 결과 확인 검토; 위험조치 진입 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.close-invite-review | BO-MBR-060 | BO-MBR-060-CLOSE-INVITE-REVIEW | screens/m40/signupInvite.tsx:139 / invite-review-cancel | 확인 단계 닫기; 단순조회 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.confirm-invite-review | BO-MBR-060 | BO-MBR-060-CONFIRM-INVITE-REVIEW | screens/m40/signupInvite.tsx:139 / invite-review-confirm | 현재 검토 종류의 처리 요청; 위험조치 실행 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.open-invite-history | BO-MBR-060 | BO-MBR-060-OPEN-INVITE-HISTORY | screens/m40/signupInvite.tsx:144 / invite-history-open | 문자 저장 이력 레이어 열기; 단순조회 | M-40 SignupInvite 별도 ProcessPanel | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.open-create | BO-MBR-080 | BO-MBR-080-OPEN-CREATE | screens/m50/index.tsx:369 / m50-create-open | 업체 수동 등록 편집 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.select-all-tab | BO-MBR-080 | BO-MBR-080-SELECT-ALL-TAB | screens/m50/index.tsx:380 / m50-tab-all | 전체 업체 탭 선택; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.select-pending-tab | BO-MBR-080 | BO-MBR-080-SELECT-PENDING-TAB | screens/m50/index.tsx:380 / m50-confirm-tab | 자진 등록 확인 대기 탭 선택; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.select-categories-tab | BO-MBR-080 | BO-MBR-080-SELECT-CATEGORIES-TAB | screens/m50/index.tsx:380 / m50-tab-categories | 업종 체계 탭 선택; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.search-places | BO-MBR-080 | BO-MBR-080-SEARCH-PLACES | screens/m50/index.tsx:382 / m50-search | 사업자번호·상호·주소 검색 입력·지우기; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.select-bulk-place | BO-MBR-080 | BO-MBR-080-SELECT-BULK-PLACE | screens/m50/index.tsx:390 / m50-select-{id} | 일괄 업종 변경 대상 체크; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.open-place | BO-MBR-080 | BO-MBR-080-OPEN-PLACE | screens/m50/index.tsx:399,402 / m50-confirm-{bn} | 자세히 보기·목록 확인 버튼은 같은 choose로 상세 선택; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.confirm-place | BO-MBR-080 | BO-MBR-080-CONFIRM-PLACE | screens/m50/index.tsx:458 / m50-confirm-{bn} | 상세에서 자진 등록 확인 처리 요청; 목록 버튼과 다른 동작; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.change-page | BO-MBR-080 | 미등록 | screens/m50/index.tsx:409 | 페이지 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.change-page-size | BO-MBR-080 | 미등록 | screens/m50/index.tsx:409 | 20·50건 표시 변경; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.open-edit | BO-MBR-080 | BO-MBR-080-OPEN-EDIT | screens/m50/index.tsx:471 / m50-edit-open | 현재 업체 수정 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.close-editor | BO-MBR-080 | BO-MBR-080-CLOSE-EDITOR | screens/m50/index.tsx:490 / m50-cancel | 업체 편집 닫기; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.save-place | BO-MBR-080 | BO-MBR-080-SAVE-PLACE | screens/m50/index.tsx:491 / m50-save | 등록 또는 수정 저장 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-business-number | BO-MBR-080 | BO-MBR-080-INPUT-BUSINESS-NUMBER | screens/m50/index.tsx:505 / m50-field-businessNo | 사업자번호 입력; 수정 모드 비활성; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-place-name | BO-MBR-080 | BO-MBR-080-INPUT-PLACE-NAME | screens/m50/index.tsx:509 / m50-field-name | 상호 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.select-place-category | BO-MBR-080 | 미등록 | screens/m50/index.tsx:513 / m50-field-categoryCode | 업종 검색·선택; 위험조치 입력 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-50.input-owner-name | BO-MBR-080 | BO-MBR-080-INPUT-OWNER-NAME | screens/m50/index.tsx:518 / m50-field-ownerName | 대표자 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.search-address | BO-MBR-080 | BO-MBR-080-SEARCH-ADDRESS | screens/m50/index.tsx:524 / m50-address-search | 주소 검색 호출; 외부 위젯 실행 여부 미확인; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-opened-date | BO-MBR-080 | BO-MBR-080-INPUT-OPENED-DATE | screens/m50/index.tsx:536 / m50-field-openedAt | 개업일 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-tax-status | BO-MBR-080 | BO-MBR-080-INPUT-TAX-STATUS | screens/m50/index.tsx:540 / m50-field-taxStatus | 세금 상태 입력·제안 선택; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-taxation-type | BO-MBR-080 | BO-MBR-080-INPUT-TAXATION-TYPE | screens/m50/index.tsx:545 / m50-field-taxationType | 과세 유형 입력·제안 선택; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-note | BO-MBR-080 | BO-MBR-080-INPUT-NOTE | screens/m50/index.tsx:551 / m50-field-note | 메모 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.open-place-info | BO-MBR-080 | BO-MBR-080-OPEN-PLACE-INFO | screens/m50/index.tsx:556 / m50-info-open | 업체정보 파생 근거 레이어 열기; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.add-parent-category | BO-MBR-080 | BO-MBR-080-ADD-PARENT-CATEGORY | screens/m50/categoryManagement.tsx:63 / m50-category-add-parent | 대분류 추가 작성 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.add-child-category | BO-MBR-080 | BO-MBR-080-ADD-CHILD-CATEGORY | screens/m50/categoryManagement.tsx:63 / m50-category-add-child | 중분류 추가 작성 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.reload-categories | BO-MBR-080 | 미등록 | screens/m50/categoryManagement.tsx:63 | 업종 목록 재조회; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.edit-category | BO-MBR-080 | BO-MBR-080-EDIT-CATEGORY | screens/m50/categoryManagement.tsx:70 / m50-category-edit-{code} | 업종 편집 열기; 위험조치 진입 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-category-code | BO-MBR-080 | BO-MBR-080-INPUT-CATEGORY-CODE | screens/m50/categoryManagement.tsx:75 / m50-category-code | 업종 코드 입력; 기존 코드 비활성; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.select-parent-category | BO-MBR-080 | BO-MBR-080-SELECT-PARENT-CATEGORY | screens/m50/categoryManagement.tsx:76 / m50-category-parent | 대분류 선택; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-category-name | BO-MBR-080 | BO-MBR-080-INPUT-CATEGORY-NAME | screens/m50/categoryManagement.tsx:77 / m50-category-name | 업종 이름 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.input-category-order | BO-MBR-080 | BO-MBR-080-INPUT-CATEGORY-ORDER | screens/m50/categoryManagement.tsx:78 / m50-category-order | 업종 표시 순서 입력; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.toggle-category-active | BO-MBR-080 | BO-MBR-080-TOGGLE-CATEGORY-ACTIVE | screens/m50/categoryManagement.tsx:79 / m50-category-active | 업종 사용 여부 체크; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.save-category | BO-MBR-080 | BO-MBR-080-SAVE-CATEGORY | screens/m50/categoryManagement.tsx:80 / m50-category-save | 업종 저장 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.close-category-editor | BO-MBR-080 | 미등록 | screens/m50/categoryManagement.tsx:80 | 업종 편집 닫기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.select-bulk-category | BO-MBR-080 | BO-MBR-080-SELECT-BULK-CATEGORY | screens/m50/categoryManagement.tsx:117 / m50-bulk-category-choice | 선택 업체에 적용할 업종 선택; 위험조치 입력 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.submit-bulk-category | BO-MBR-080 | BO-MBR-080-SUBMIT-BULK-CATEGORY | screens/m50/categoryManagement.tsx:118 / m50-bulk-category-submit | 선택 업체 업종 일괄 변경 요청; 위험조치 실행 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-01.close-process | BO-MBR-010 | 미등록 | components/ProcessPanel.tsx:31-32 / process-panel-close | 패널 닫기·목록으로·Escape; M-40 상세 닫기도 같은 역할; 단순조회 | 호출 화면의 ProcessPanel | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-01.continue-process-writing | BO-MBR-010 | 미등록 | components/ProcessPanel.tsx:33 | 패널 이동 취소하고 계속 작성; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.discard-process-writing | BO-MBR-010 | 미등록 | components/ProcessPanel.tsx:33 | 작성 내용을 버리고 예약 이동; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.close-info | BO-MBR-010 | 미등록 | components/InfoLayer.tsx:31 / info-layer | 정보 레이어 닫기·Escape; 단순조회 | 각 화면 InfoLayer; 동일 닫기 버튼 합침 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-02.close-process | BO-MBR-020 | 미등록 | components/ProcessPanel.tsx:31-32 / process-panel-close | 패널 닫기·목록으로·Escape; M-40 상세 닫기도 같은 역할; 단순조회 | 호출 화면의 ProcessPanel | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-02.continue-process-writing | BO-MBR-020 | 미등록 | components/ProcessPanel.tsx:33 | 패널 이동 취소하고 계속 작성; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.discard-process-writing | BO-MBR-020 | 미등록 | components/ProcessPanel.tsx:33 | 작성 내용을 버리고 예약 이동; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.close-info | BO-MBR-020 | 미등록 | components/InfoLayer.tsx:31 / info-layer | 정보 레이어 닫기·Escape; 단순조회 | 각 화면 InfoLayer; 동일 닫기 버튼 합침 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-10.close-process | BO-MBR-030 | 미등록 | components/ProcessPanel.tsx:31-32 / process-panel-close | 패널 닫기·목록으로·Escape; M-40 상세 닫기도 같은 역할; 단순조회 | 호출 화면의 ProcessPanel | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-10.continue-process-writing | BO-MBR-030 | 미등록 | components/ProcessPanel.tsx:33 | 패널 이동 취소하고 계속 작성; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.discard-process-writing | BO-MBR-030 | 미등록 | components/ProcessPanel.tsx:33 | 작성 내용을 버리고 예약 이동; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.close-info | BO-MBR-030 | 미등록 | components/InfoLayer.tsx:31 / info-layer | 정보 레이어 닫기·Escape; 단순조회 | 각 화면 InfoLayer; 동일 닫기 버튼 합침 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-20.close-process | BO-MBR-050 | 미등록 | components/ProcessPanel.tsx:31-32 / process-panel-close | 패널 닫기·목록으로·Escape; M-40 상세 닫기도 같은 역할; 단순조회 | 호출 화면의 ProcessPanel | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-20.continue-process-writing | BO-MBR-050 | 미등록 | components/ProcessPanel.tsx:33 | 패널 이동 취소하고 계속 작성; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.discard-process-writing | BO-MBR-050 | 미등록 | components/ProcessPanel.tsx:33 | 작성 내용을 버리고 예약 이동; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.close-info | BO-MBR-050 | 미등록 | components/InfoLayer.tsx:31 / info-layer | 정보 레이어 닫기·Escape; 단순조회 | 각 화면 InfoLayer; 동일 닫기 버튼 합침 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-40.close-process | BO-MBR-060 | 미등록 | components/ProcessPanel.tsx:31-32 / process-panel-close | 패널 닫기·목록으로·Escape; M-40 상세 닫기도 같은 역할; 단순조회 | 호출 화면의 ProcessPanel | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-40.continue-process-writing | BO-MBR-060 | 미등록 | components/ProcessPanel.tsx:33 | 패널 이동 취소하고 계속 작성; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.discard-process-writing | BO-MBR-060 | 미등록 | components/ProcessPanel.tsx:33 | 작성 내용을 버리고 예약 이동; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.close-info | BO-MBR-060 | 미등록 | components/InfoLayer.tsx:31 / info-layer | 정보 레이어 닫기·Escape; 단순조회 | 각 화면 InfoLayer; 동일 닫기 버튼 합침 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-30.close-process | BO-MBR-070 | 미등록 | components/ProcessPanel.tsx:31-32 / process-panel-close | 패널 닫기·목록으로·Escape; M-40 상세 닫기도 같은 역할; 단순조회 | 호출 화면의 ProcessPanel | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-30.continue-process-writing | BO-MBR-070 | 미등록 | components/ProcessPanel.tsx:33 | 패널 이동 취소하고 계속 작성; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.discard-process-writing | BO-MBR-070 | 미등록 | components/ProcessPanel.tsx:33 | 작성 내용을 버리고 예약 이동; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.close-info | BO-MBR-070 | 미등록 | components/InfoLayer.tsx:31 / info-layer | 정보 레이어 닫기·Escape; 단순조회 | 각 화면 InfoLayer; 동일 닫기 버튼 합침 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-50.close-process | BO-MBR-080 | 미등록 | components/ProcessPanel.tsx:31-32 / process-panel-close | 패널 닫기·목록으로·Escape; M-40 상세 닫기도 같은 역할; 단순조회 | 호출 화면의 ProcessPanel | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-50.continue-process-writing | BO-MBR-080 | 미등록 | components/ProcessPanel.tsx:33 | 패널 이동 취소하고 계속 작성; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.discard-process-writing | BO-MBR-080 | 미등록 | components/ProcessPanel.tsx:33 | 작성 내용을 버리고 예약 이동; 단순조회 | ProcessPanel 미저장 확인 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.close-info | BO-MBR-080 | 미등록 | components/InfoLayer.tsx:31 / info-layer | 정보 레이어 닫기·Escape; 단순조회 | 각 화면 InfoLayer; 동일 닫기 버튼 합침 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-01.open-columns | BO-MBR-010 | 미등록 | components/DataTable.tsx:122 / column-picker | 표시 열 설정 열기; 단순조회 | ColumnPicker | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-01.toggle-column | BO-MBR-010 | 미등록 | components/DataTable.tsx:116 | 열 표시 체크 변경; 단순조회 | ColumnPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.reset-columns | BO-MBR-010 | 미등록 | components/DataTable.tsx:118 | 열 표시·폭 기본값 복원; 단순조회 | ColumnPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.open-columns | BO-MBR-060 | 미등록 | components/DataTable.tsx:122 / column-picker | 표시 열 설정 열기; 단순조회 | ColumnPicker | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-40.toggle-column | BO-MBR-060 | 미등록 | components/DataTable.tsx:116 | 열 표시 체크 변경; 단순조회 | ColumnPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.reset-columns | BO-MBR-060 | 미등록 | components/DataTable.tsx:118 | 열 표시·폭 기본값 복원; 단순조회 | ColumnPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-01.copy-identifier | BO-MBR-010 | 미등록 | components/Code.tsx:13; screens/m02/index.tsx:233,323,344,413,415,418,507; components/IdentityStrip.tsx:24 | Code가 표시한 현재·이전 회원/신청/바우처/거래/요청/결과 번호 복사; 값만 다른 동일 행동; 단순조회 | Code copyable; M-01은 MemberWorkspace 내 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-02.copy-identifier | BO-MBR-020 | 미등록 | components/Code.tsx:13; screens/m02/index.tsx:233,323,344,413,415,418,507; components/IdentityStrip.tsx:24 | Code가 표시한 현재·이전 회원/신청/바우처/거래/요청/결과 번호 복사; 값만 다른 동일 행동; 단순조회 | Code copyable; M-01은 MemberWorkspace 내 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.copy-identifier | BO-MBR-070 | 미등록 | components/Code.tsx:13; screens/m30/index.tsx:91,93,95,98,114; components/IdentityStrip.tsx:24; screens/m30/WithdrawnPrivacyPanel.tsx:174,180 | Code가 표시한 현재·이전 회원/신청/바우처/거래/요청/결과 번호 복사; 값만 다른 동일 행동; 단순조회 | Code copyable; M-01은 MemberWorkspace 내 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.open-receipt-target | BO-MBR-060 | 미등록 | screens/m40/receiptIsolation.tsx:16 | 현재·이전 요청 대상 확인 번호 펼치기·접기; 단순조회 | ReceiptTrail 공유 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.open-receipt-target | BO-MBR-080 | 미등록 | screens/m40/receiptIsolation.tsx:16 | 현재·이전 요청 대상 확인 번호 펼치기·접기; 단순조회 | ReceiptTrail 공유 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.reset-filters | BO-MBR-080 | BO-MBR-080-RESET-FILTERS | screens/m50/index.tsx:384 / filter-reset | 검색 조건 초기화; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-50.reload-places | BO-MBR-080 | 미등록 | screens/m50/index.tsx:385 / m50-error | 업체 목록 다시 불러오기; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-50.reload-place | BO-MBR-080 | 미등록 | screens/m50/index.tsx:433 / m50-detail-error | 선택 업체 상세 다시 불러오기; 단순조회 | 화면 전용 | 미등록: 공용 컨트롤·행·컨테이너 testId의 내부 동작 locator 미확정 |
| M-50.open-full-text | BO-MBR-080 | 미등록 | screens/m50/index.tsx:469 | 메모 전체 보기; 단순조회 | 화면 전용 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-50.open-map | BO-MBR-080 | BO-MBR-080-OPEN-MAP | screens/m50/index.tsx:499 / m50-map-link | 지도 링크 새 창 열기; 외부 목적지는 실행·접속하지 않음; 단순조회 | 화면 전용 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-40.open-invite-full-text | BO-MBR-060 | 미등록 | screens/m40/signupInvite.tsx:128,141,142; screens/m40/receiptIsolation.tsx:22 | 문자 문구·검토 사유 전체 보기; 단순조회 | SignupInvite showFull | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-picker-privacy-member | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:36 | 찾기 창 열기; 단순조회 | privacy-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.close-picker-privacy-member | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:39 | 찾기 창 닫기·Escape; 단순조회 | privacy-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.clear-selection-privacy-member | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:36 | 현재 선택 지우기; 단순조회 | privacy-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-number-privacy-member | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:38 | 선택된 대상 확인용 번호 펼치기·접기; 단순조회 | privacy-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-picker-privacy-request | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:36 | 찾기 창 열기; 단순조회 | privacy-request EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.close-picker-privacy-request | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:39 | 찾기 창 닫기·Escape; 단순조회 | privacy-request EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.clear-selection-privacy-request | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:36 | 현재 선택 지우기; 단순조회 | privacy-request EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-number-privacy-request | BO-MBR-070 | 미등록 | components/EntityPicker.tsx:38 | 선택된 대상 확인용 번호 펼치기·접기; 단순조회 | privacy-request EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.open-picker-link-member | BO-MBR-060 | 미등록 | components/EntityPicker.tsx:36 | 찾기 창 열기; 단순조회 | link-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.close-picker-link-member | BO-MBR-060 | 미등록 | components/EntityPicker.tsx:39 | 찾기 창 닫기·Escape; 단순조회 | link-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.clear-selection-link-member | BO-MBR-060 | 미등록 | components/EntityPicker.tsx:36 | 현재 선택 지우기; 단순조회 | link-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.open-number-link-member | BO-MBR-060 | 미등록 | components/EntityPicker.tsx:38 | 선택된 대상 확인용 번호 펼치기·접기; 단순조회 | link-member EntityPicker | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-40.copy-picker-identifier | BO-MBR-060 | 미등록 | components/EntityPicker.tsx:38; components/Code.tsx:13 | 선택한 연결 회원 확인용 번호 복사; 단순조회 | EntityPicker Code | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-10.copy-identifier | BO-MBR-030 | 미등록 | screens/m11/index.tsx:147; components/IdentityStrip.tsx:24; components/Code.tsx:13 | 신청 식별 줄의 신청 번호 복사; 단순조회 | ApplicationReview → IdentityStrip 공유 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-20.copy-identifier | BO-MBR-050 | 미등록 | screens/m11/index.tsx:147; components/IdentityStrip.tsx:24; components/Code.tsx:13 | 신청 식별 줄의 신청 번호 복사; 단순조회 | ApplicationReview → IdentityStrip 공유 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-11.copy-identifier | BO-MBR-040 | 미등록 | screens/m11/index.tsx:147; components/IdentityStrip.tsx:24; components/Code.tsx:13 | 신청 식별 줄의 신청 번호 복사; 단순조회 | ApplicationReview → IdentityStrip 공유 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |
| M-30.open-member | BO-MBR-070 | BO-MBR-070-OPEN-MEMBER | screens/m30/index.tsx:95 / m30-identity | 심사 대상 회원 M-02로 이동; 단순조회 | 심사 본문 | 등록: 원문에 동작별 기존 testId 및 소스 행 명시 |
| M-30.open-result-member-number | BO-MBR-070 | 미등록 | screens/m30/index.tsx:93 | 이번 심사 요청 결과의 회원 확인용 번호 펼치기·접기; 단순조회 | 심사 결과 본문 | 미등록: 소스 행·동작 설명만 제공, 동작별 locator 미확정 |

## 원문 공유 본문·소유 경계

- M-01 `screens/m01/index.tsx:15,97`는 M-02의 MemberWorkspace를 embedded로 사용한다. M-01의 목록/가입 대기 패널은 M-01 소유이고 회원 처리 본문은 M-02 구현을 공유한다. M-01에서는 전체 상세의 바우처·거래·민원 링크/전체 조사 탭을 역할로 복제하지 않았다. 회원 번호·연락처·조치·보안/이력 레이어는 실제 embedded 분기에 해당한다. M-02 조치 모달도 `embedded`로 호출하므로 MemberStage의 독립 Modal 닫기 버튼을 추가하지 않았다.
- M-10/M-20은 `screens/m11/panel.tsx:18-21`의 ApplicationPanel → ApplicationReview를 embedded로 공유한다. 역할 접두사는 현재 표시 화면 M-10/M-20이며 상세 새 페이지 이동 후에는 M-11이다. M-11 전체 상세에는 embedded 전용 추가정보 버튼/InfoLayer가 없으므로 해당 역할을 공급하지 않는다. 확인 기록·위임장·약관·서류 정보는 조회 본문이며 서류 다운로드 버튼은 없다.
- W01 `screens/workflows/signup/review.tsx:3-6`는 M-01 `maskName`, `piiPolicy`의 `useMemberPiiProtection`, M-11의 `Application` 타입을 참조한다. W01은 자체 review UI이며 ApplicationReview를 렌더링하는 회원 화면으로 취급하지 않는다. 타입/PII 유틸의 공급 경계만 기록하며 W01 버튼·권한·처리 흐름은 이 회원 8화면 표에 추가하지 않는다.
- 공용 ProcessPanel/InfoLayer/EntityPicker/Code/ColumnPicker는 구현 소유를 이동하지 않고 호출 화면에 역할을 귀속한다. M-40 receiptIsolation은 M-50에서도 쓰이므로 파일 위치와 동작의 화면 소유를 혼동하지 않는다. EntityPicker의 검색 입력/후보 클릭·키보드 선택은 각 `select-…` 역할에 대응하고, 찾기 창 열기/닫기·선택 해제·번호 펼치기는 별도 행이다. M-30 `copy-identifier`에는 EntityPicker:38의 Code도 포함한다.
- `apps/web/docs/ui-tracing.md`: portal은 현재 URL 화면에 귀속하고 click→press / form submit / change로 기록한다. 역할 이름은 생성 요소 코드의 대체물이 아니다. 입력값·검색어·업무 ID를 수집하지 않는다. 실제 화면 생성 코드 연결은 정본 공급 후의 작업이다.
- 고정 catalog `generated/ui-catalog/bo-catalog.ts:215-335`의 회원 화면 owner는 A08이다. 현재 개선 작업을 A11이 맡는 것과 정본 owner 변경은 별개다. **A08→A11 작업 소유 이전의 정본 관리는 P00 책임**이며 이 문서는 catalog/registry/타팀 소유를 바꾸지 않는다.

## 원문 서두·반복/제외 정책

# 회원 화면 동작 역할 독립표

고정 입력: 1759 archive(aa0d4f9), SHA256 `a82e3add3bf35422e82b5af98a60c9d67bc0cdda11376d5510b7b3d106536625`.
이 attempt의 `TEAM_OUT_DIR/source`에 추출한 원본만 읽었다. 제품 저장소·1800 미리보기 변형본은 검색하지 않았다.
소스 경로는 archive의 `apps/web/src/` 기준이고 행은 고정 원본 기준이다. 표의 역할은 동작 식별용 제안 이름이며 권한 역할·ROLE TABLE·권한 부여 규칙이 아니다.

`SCREEN_CODES["BO-MBR-…"]`는 기존 화면 상수 참조이며 요소 코드가 아니다. `telemetry/uiTrace.ts:8`의 `ELEMENTS = Object.freeze({})`와 `apps/web/docs/ui-tracing.md`의 요소 정본 미공급 방침을 확인했다. 모든 행의 요소 생성 상수는 **미공급**이다. 임의 요소 코드·생성 심볼·data-ui-code를 만들지 않는다.

역할 하나에 같은 행동의 반복 행/버튼/입력 지우기를 합쳤다. 값·업무 ID·텍스트는 역할 이름에 넣지 않는다. 상태 선택 및 탭은 동일 setter의 선택 컨트롤 하나로 센다. 반대로 M-50의 같은 testId `m50-confirm-{bn}`은 목록에서 상세 열기와 상세에서 확인 실행이므로 두 역할이다. 읽기 전용 텍스트·상태·툴팁·포커스 가능한 정적 셀은 동작 역할이 아니다. 비활성 컨트롤도 실제 소스에 존재하므로 포함한다.

위험조치 진입/입력/확인/실행은 현재 UI의 조치 단계 분류이며 새 처리 흐름이나 권한 명세가 아니다. 개인정보 열람도 위험조치로 구분한다. 단순조회는 조회·이동·패널 닫기 등 UI 동작을 포함한다.


