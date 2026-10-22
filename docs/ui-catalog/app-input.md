# APP 화면·메뉴 정본 반입 입력

`app-input.json`은 GS-APP `4c28c781a1a34da832b6609494e59498b2241772`에서 추출한 A13 반입용 입력이다. base-common `d500946bb7dd` B1 `ui-catalog/catalog.json`의 screens/menus/aliases 레코드 형식을 따른다. 실제 정본 통합·버전 확정·생성기 APP 지원·발행은 A13 후속 작업이다. 제품 코드는 변경하지 않는다. `golmok.ui.event.id`는 유지한다. GS-MEM의 앱 복사본은 포함하지 않는다.

## 수량과 분모

- 현재 `routes.ts` 라우트 바인딩 **138개**와 registry의 10개 시나리오 index 화면을 대조한다. 경로나 구현 공유도 바인딩별 화면 정체성은 보존한다.
- 생성 카탈로그 **156개 = 현재 라우트 138 + 미연결 15 + 특수 3**를 모두 보존한다. 약 138개와의 차이 18개는 누락이 아니다.
- 미연결은 `i02/*` 14개와 `b01/loginidchange` 1개다. 첨부 10-19 inventory의 155개(138+14+3)보다 현재 카탈로그에는 loginidchange가 1개 더 있다. 과거 소스의 변경 원인은 이번 자료로 확인하지 않았다.
- 특수는 `special/loading-showcase`, `special/local-interaction-cleanup`, `special/screen-hub`다. 현재 라우트가 아닌 18개는 이 입력에서 PLANNED로 보수적으로 표시한다. 이는 미구현/폐기 판정이 아니라 활성 라우트 미확인 표시다. A13은 이를 활성 화면으로 오인하지 않아야 한다.
- 전체 메뉴 TREE **8그룹·28말단**, 하단 ITEMS **5개**, 총 **33메뉴**다. 그룹은 화면 목적지가 없는 접기 컨테이너라 inventory.menuGroups에만 보존한다. 메뉴 parent=null, 그룹은 menuMappings.group에 보존한다. B1 parent는 목적지 화면을 필수로 요구하므로 가짜 그룹 화면을 만들지 않는다.

## 코드와 별칭

표준 코드 APP-{업무번호 접두}-{3자리 번호}를 각 영역에서 원본 카탈로그 등장 순서대로 010부터 10씩 최초 배정했다. 화면 하나마다 고유하며 공유 business_no를 그대로 표준 코드로 쓰지 않는다. 이 최초 배정 이후 재정렬로 재번호를 부여하지 않는다. 버전 `1.0.0-app.1`은 반입 입력 버전이며 base-common 최종 통합 버전은 A13이 확정한다. 영역 이름은 확인된 업무 접두 그대로 두었다.

B1 screens.businessNumber에 business_no를, aliases의 INTERNAL_ID에 기존 GM-SCR-*를 보존한다. B1은 BUSINESS_NO kind가 없으므로 업무번호와 옛 화면번호는 LEGACY kind로 보존한다. 공유 값은 AMBIGUOUS/target=null/candidates로 모두 보존하고 유일 화면으로 변환하지 않는다. ROUTE 별칭은 바인딩 키와 원래 경로 템플릿을 보존한다. 동일 경로가 여러 바인딩이면 모호성을 유지한다. 문맥 쿼리는 inventory.screenMappings.routeContext로 원문을 보존하며 경로에 임의로 합치지 않는다. inventory의 internal_id/business_no/legacy는 원문 1:1 대응 근거이며 별도의 정본이 아니다.

화면 이름은 internalScreenName, 경로는 routeTemplate을 사용한다. implementation은 현재 화면의 구현 바인딩이 소속된 실제 시나리오 index.ts를 가리킨다. 화면 컴포넌트 개별 파일명을 추정하지 않는다. 미연결·특수는 implementation=null이다. 원본 implementationBinding은 inventory에 남긴다.

메뉴 코드는 APP-MENU-{TREE 그룹}-{목적지 key}, 하단은 APP-MENU-TAB-{ITEMS.k}로 최초 배정한다. 코드 영문은 신규 제안이다. menuMappings.key에는 메뉴의 고정 쿼리까지 보존한다. 하단 target은 ITEMS의 기본 목적지다. 임직원 home/ticket/account의 조건부 목적지는 inventory.tabStaffTargets에 따로 보존한다. 활동·교환·바우처의 액션 알림 분기는 같은 화면에 activityScope/activityState, exchangeState/exchangeResult, walletMode/useMode 쿼리를 추가하므로 별도 화면으로 늘리지 않았다. 권한·기능·검색에 따른 노출은 메뉴 수에서 제외하지 않는다.

## B1 호환 경계와 검사

레코드 필드/별칭 복합키(system,kind,value,version)/상태는 B1 형식을 따른다. 다만 현재 B1 생성기는 BO 전용으로 APP 화면을 거절하고 inventory.registryIds/menuIds/issueCatalogIds/shellIds도 BO 전용이다. 따라서 이 JSON을 현재 B1에 그대로 실행할 수 있다고 주장하지 않는다. A13은 APP 활성화·inventory 분모 확장 후 BO 정본에 병합하고 별칭 version과 introducedVersion을 통합 버전에 맞춰야 한다. sources에는 사용한 원본 파일의 revision·SHA-256을 고정했다.

원본 대비 검사 1회는 현재 라우트·registry·카탈로그·메뉴/탭을 다시 읽어 분모를 만들고 화면/코드/메뉴/별칭 복합키 중복, 누락·초과, 이름·경로·문맥·별칭 후보 보존과 메뉴 목적지를 대조한다. 공유 경로·업무번호·옛 번호의 모호성은 원본 특성이며 정본 행 중복과 구별한다. 검사 스크립트·출력·patch는 TEAM_OUT_DIR에 보관한다. 앱 실행·전체 회귀·base-common 생성기 실행은 하지 않는다.
