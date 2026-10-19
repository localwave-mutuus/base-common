# 이슈 현황 2역할 정본 대조 (bo.7)

소유팀 문서: `teams/A08.화면개선/docs/10-design/issue-home-t2-input.md`. WORKTREE_BASE `a6c620e09b7b2cf133546ea6a17887da3503f15f` (문서 자체의 커밋을 뜻하지 않음), 문서 SHA-256 `83d72c76cdb6c3d5c662cf6f39b04f12b18ef8ecdd81390463e28f0320eedba1`.

제품 기준: `7db9239893fc3c005792566cbe4762d2a632f322`. base-common 입력: `eff418b6ca166bbb95f55d74f3d536662a9a9993`.

| 공급 역할 | 화면 | 정확 ELEMENT_CODES 키 | 동작 | locator |
|---|---|---|---|---|
| I-01.select-stage | BO-ISS-010 | `ELEMENT_CODES["BO-ISS-010-SELECT-STAGE"]` | press | .issue-pipeline[aria-label="단계별 이슈"] button.issue-stage 또는 nav.issue-status-chips[aria-label="단계별 이슈"] button.issue-status-chip; Home.tsx:64~78 |
| I-01.select-system | BO-ISS-010 | `ELEMENT_CODES["BO-ISS-010-SELECT-SYSTEM"]` | change | Home.tsx:82 Select aria-label="시스템"; AntD Select 소스 노드 계약, option portal DOM 미확정 |

이번 공급 분모2/등록2. 누적 SYS130+TAX28+DSH18+회원152+ISS2=330. 기존328행과 화면/owner는 불변이다.

최종 시나리오: I01 view → 단계클릭 press → 같은 단계버튼 Enter press(keyboard) → 시스템 change. view는 BO-ISS-010 화면 동작이며 요소를 발급하지 않는다. 검색 input/form/submit은 구현이 없어 등록하지 않는다. native button Enter는 submit이 아니다.

AntD Select는 소스 Select aria-label 계약만 보존한다. option portal DOM 미확정 및 portal change 의미추적 미지원 경계가 있으며 제품 추적 handler를 수정하지 않았다. 제품 부착·키보드 실행·전송/span/수집/DB 성공은 확인하지 않았다.

## 소유팀 원문 역할표 (그대로 보존)


| 공급 역할 | 실제 의미·동작 | 정확 소스/노드 경계 | testId·폼 | 발급/키보드 경계 |
|---|---|---|---|---|
| `I-01.view` | 현황 route 진입 후 화면 view | ScreenHost의 UiView route, UiTraceBoundary의 view effect | view 전용 testId/폼 없음 | 화면 BO-ISS-010 기존. 별도 요소 코드는 미발급이며 view를 press/submit 요소로 발명하지 않음 |
| `I-01.select-stage` | 처리 단계 group 변경·page=0·URL group 설정·GET 재조회 | Home.tsx:40 filter; desktop 71~78 `button.issue-stage` / mobile 64~67 `button.issue-status-chip`, 모두 onClick→filter(item.key), aria-pressed·disabled=decisionLocked | 필터 버튼 전용 testId 없음. `<form>`에 속하지 않음. `.issue-pipeline[aria-label="단계별 이슈"] button.issue-stage` 또는 `nav.issue-status-chips[aria-label="단계별 이슈"] button.issue-status-chip`는 소스상 기존 selector | 정본 요소 코드 미발급. 같은 단계선택 동작은 하나의 역할, group은 별도 대상값. native button의 Enter/Space 활성화는 클릭/press 경계이며 submit 의미가 아님. 실제 키보드 실행은 미검증 |
| `I-01.select-system` | 시스템 값 변경·page=0·GET systemCode 필터 | Home.tsx:82 `Select aria-label="시스템"`, onChange→setSystem(value)/setPage(0) | 전용 testId/폼 없음. Antd Select 실제 option portal DOM은 여기서 확정하지 않음 | 코드 미발급. source change 동작을 press나 Enter submit으로 바꾸지 않음. 정본 입력에 필요할 때 A02가 공용 실제 노드 계약 확정 |
| 검색 Enter 제출 | **구현 없음·역할 분모 제외** | Home 전체에 검색 input/검색 form/onSubmit/검색 onKeyDown 없음 | 검색 testId/폼 경로 **없음** | 신규 search/submit 역할·가상 코드·임시 폼을 만들지 않음. 이 화면에서 검색 Enter submit 성공 시나리오 실행 불가 |

동작 요소의 이번 최소 공급 분모는 단계선택·시스템선택 **2역할**이다. view는 화면 범위로 별도, 검색 제출은 미구현이므로 분모에서 제외한다. 전체 Home 기능의 전수 역할 분모가 아니다.


생성 version `1.0.0-bo.7`, digest `c2d12459aae715af7ed6c6111ef2778df2a58529f53e59f8963f3bdfd2390c10`. 재현 검증은 `PYTHONDONTWRITEBYTECODE=1 python3 tools/ui-catalog/verify_iss.py`이며 TEAM_OUT_DIR에 변경 전 baseline-catalog.json과 고정 issue-home-t2-input.md가 필요하다. 이 검증은 정본 반입·생성 1회와 런타임 Java 동기화를 수행하므로 이미 성공한 입력에 반복 실행하지 않는다.
