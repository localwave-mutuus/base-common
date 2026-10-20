# I01 종료 8역할 대조 (bo.8)

소유팀 정본: brief 첨부 `attach-1-i01-close-action-roles.md` (I01 종료 처리 추적 요소 등록 요청). 화면 BO-ISS-010, 원천 I-01.
base-common 입력 `16d59fabb82a3e74c93d38ed2e5b23156d755125`.
GS-BO 고정 입력 `418a27c385f8a8a50818880da7c3ee40b68f82e6` + `261020-1328-i01-close-actions-ui.a04` 누적 patch.
patch 정본 `artifacts/261020-1328-i01-close-actions-ui/261020-1328-i01-close-actions-ui.a04/patch.diff`.
SHA-256 `cae2ae1749d4c421d34490acbc1a567ac74d7086849e48a79de1bdfcf3a9f4ce` (실제 파일 바이트 확인). 소스 줄은 patch 적용 결과이며 원본 제품 줄이 아니다. 제품 통합 커밋 없음.

| 논리 역할 | 정확 생성 TS 상수 키 | 소스 locator | 공유·원문 근거 |
|---|---|---|---|
| I-01.open-row-done | `ELEMENT_CODES["BO-ISS-010-OPEN-ROW-DONE"]` | CloseActions.tsx:86~90 DONE 분기; Home 데스크톱 행·모바일 카드 전달점 | 단일 행 종료. 데스크톱/모바일·반복 행 동일 역할. 대상 이슈 ID는 별도 맥락. |
| I-01.open-row-rejected | `ELEMENT_CODES["BO-ISS-010-OPEN-ROW-REJECTED"]` | CloseActions.tsx:86~90 REJECTED 분기; Home 데스크톱 행·모바일 카드 전달점 | 단일 행 종료(미수용). 데스크톱/모바일·반복 행 동일 역할. 대상 이슈 ID는 별도 맥락. |
| I-01.open-bulk-done | `ELEMENT_CODES["BO-ISS-010-OPEN-BULK-DONE"]` | CloseActions.tsx:86~90 DONE 분기; Home issue-decision-panel 전달점 | 선택 묶음 종료. 선택 1건도 bulk. 대상 개수로 행/일괄을 구분하지 않고 호출 위치 구분 prop 필요. |
| I-01.open-bulk-rejected | `ELEMENT_CODES["BO-ISS-010-OPEN-BULK-REJECTED"]` | CloseActions.tsx:86~90 REJECTED 분기; Home issue-decision-panel 전달점 | 선택 묶음 종료(미수용). 선택 1건도 bulk. 대상 개수로 행/일괄을 구분하지 않고 호출 위치 구분 prop 필요. |
| I-01.confirm-close | `ELEMENT_CODES["BO-ISS-010-CONFIRM-CLOSE"]` | CloseActions.tsx:94~97 Modal footer 처리 Button | 종료 처리 확인. 행/일괄·DONE/REJECTED·같은 요청 다시 확인은 공유 역할. 목적 단계·대상·재시도 여부는 별도 맥락. |
| I-01.cancel-close | `ELEMENT_CODES["BO-ISS-010-CANCEL-CLOSE"]` | CloseActions.tsx:93 Modal footer 취소 Button; 91~92 Modal onCancel | 종료 취소. 행/일괄·DONE/REJECTED 공유. Modal X/ESC/배경은 동일 함수이나 포털 DOM 미확정; 별도 ID 발급 없음. |
| I-01.change-close-reason | `ELEMENT_CODES["BO-ISS-010-CHANGE-CLOSE-REASON"]` | CloseActions.tsx:101~102 Input.TextArea aria-label="종료 사유" | 종료 사유. 행/일괄·DONE/REJECTED 공유 필수 입력. 사용자 입력 원문을 추적 값으로 보내지 않음. |
| I-01.change-completion-step | `ELEMENT_CODES["BO-ISS-010-CHANGE-COMPLETION-STEP"]` | CloseActions.tsx:104~106 Input.TextArea aria-label="완료 작업 내용" | 완료 작업 내용. 행/일괄 공유 DONE 전용 필수 입력; REJECTED에는 없음. 사용자 입력 원문 추적 금지. |

기존 SELECT-STAGE/SELECT-SYSTEM 및 330개 요소/역할/귀속은 유지하고 8개만 추가하여 338개다. 행 선택 체크박스에 새 ID를 발급하지 않는다. 확인/취소/입력은 공유하며 목적 단계·대상은 별도 맥락이다. Modal X/ESC/배경은 포털 내부 DOM 지원 미확정으로 별도 exact ID를 만들지 않는다. 사용자 입력 원문 trace 금지.

생성 TS 반입 대상은 `generated/ui-catalog/bo-catalog.ts`이며 실제 hash는 이번 실행 result.json/한국어보고에 기록한다. 제품 부착은 A08, GS-BO TS 반입은 A02 후속 task/P00 통합이다. 실제 추적 수집·키보드·포털·API·개발환경 시험·실행·배포 성공을 의미하지 않는다. 서버 계약 `ffdcc9e6fab499d20aabfa657573477ad08bfe6d`는 별도 통합이다.

검증은 `PYTHONDONTWRITEBYTECODE=1 python3 tools/ui-catalog/verify_iss_close.py` 최초 1회만 실행한다. 기존 성공 입력 시험을 재실행하지 않는다. schema/공통생성기 변경 없음.
