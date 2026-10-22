# BO·APP 코드 검토 목록

GENERATED — DO NOT EDIT. catalogVersion=1.0.0-ui.9; catalogDigest=sha256:57d3de2512f1eab902db683798eab9db8c83b6ad1abbf683dd029a123a80a8f4

사용자 승인 전 검토안. 메뉴 영문 항목은 설계의 STATUS 패턴을 따른 신규 식별자다.

| 표준 코드 | 이름 | 기존 코드 / 주소 | 상태 | 소유 | 메뉴 |
|---|---|---|---|---|---|
| APP-ADM-010 | 운영 대시보드 | ADM-010 / /admin | PLANNED | A14 | — |
| APP-ADM-020 | 공지 관리 | ADM-020 / /admin/notices | PLANNED | A14 | — |
| APP-ADM-030 | 공지 편집 | ADM-020 / /admin/notices/edit | PLANNED | A14 | — |
| APP-ADM-040 | 이벤트 관리 | ADM-030 / /admin/events | PLANNED | A14 | — |
| APP-ADM-050 | 이벤트 편집 | ADM-030 / /admin/events/edit | PLANNED | A14 | — |
| APP-ADM-060 | 매거진 관리 | ADM-040 / /admin/magazine | PLANNED | A14 | — |
| APP-ADM-070 | 매거진 편집 | ADM-040 / /admin/magazine/edit | PLANNED | A14 | — |
| APP-ADM-080 | FAQ 관리 | ADM-050 / /admin/faq | PLANNED | A14 | — |
| APP-ADM-090 | FAQ 편집 | ADM-050 / /admin/faq/edit | PLANNED | A14 | — |
| APP-ADM-100 | 1:1 문의함 | ADM-060 / /admin/inquiries | PLANNED | A14 | — |
| APP-ADM-110 | 문의 답변 | ADM-060 / /admin/inquiries/answer | PLANNED | A14 | — |
| APP-ADM-120 | 제품 기능 정책 | ADM-070 / /admin/product-policy | PLANNED | A14 | — |
| APP-ADM-130 | 가입 신청 관리 | ADM-080 / /admin/merchant-applications | PLANNED | A14 | — |
| APP-ADM-140 | 이의제기 관리 | ADM-090 / /admin/disputes | PLANNED | A14 | — |
| APP-ADM-150 | LOCAL 상호작용 데이터 정리 | ADM-110 / /admin/local-interaction-cleanup | PLANNED | A14 | — |
| APP-AUT-010 | 스플래시 | AUT-010 / /auth/splash | IMPLEMENTED | A14 | — |
| APP-AUT-020 | 로그인 | AUT-010 / /auth/login | IMPLEMENTED | A14 | — |
| APP-AUT-030 | DEV 자동 로그인 | AUT-010 / /auth/dev-login | IMPLEMENTED | A14 | — |
| APP-AUT-040 | 아이디 찾기 | AUT-030 / /auth/login-id-recovery | IMPLEMENTED | A14 | — |
| APP-AUT-050 | 비밀번호 찾기 | AUT-020 / /auth/password-reset | IMPLEMENTED | A14 | — |
| APP-AUT-060 | 이메일 코드 확인 | AUT-020 / /auth/password-reset/code | IMPLEMENTED | A14 | — |
| APP-AUT-070 | 새 비밀번호 | AUT-020 / /auth/password-reset/new | IMPLEMENTED | A14 | — |
| APP-AUT-080 | 변경 완료 | AUT-020 / /auth/password-reset/done | IMPLEMENTED | A14 | — |
| APP-AUT-090 | SNS 인증 콜백 | AUT-010 / /auth/oauth/kakao/callback | IMPLEMENTED | A14 | — |
| APP-AUT-100 | SNS 로그아웃 콜백 | AUT-010 / /auth/oauth/kakao/logout/callback | IMPLEMENTED | A14 | — |
| APP-CHT-010 | 거래처 대화방 (보조) | CHT-020 / /chat-rooms/[id] | IMPLEMENTED | A14 | — |
| APP-CHT-020 | 거래처 대화방 | CHT-030 / /chat-rooms/[id] | IMPLEMENTED | A14 | — |
| APP-CHT-030 | 거래처 대화 | CHT-010 / /chat-rooms | IMPLEMENTED | A14 | APP-MENU-STORE-CHATLIST |
| APP-CHT-040 | 거래처 대화방 | CHT-010 / /chat-rooms/[id] | IMPLEMENTED | A14 | — |
| APP-CHT-050 | 제안서 전송 | CHT-010 / /chat-rooms/[id]/proposal | IMPLEMENTED | A14 | — |
| APP-CHT-060 | 일정 조율 | CHT-010 / /chat-rooms/[id]/schedule | IMPLEMENTED | A14 | — |
| APP-EXC-010 | 지정 교환 | EXC-110 / /exchange-proposals | IMPLEMENTED | A14 | APP-MENU-EXCHANGE-HUB, APP-MENU-TAB-MATCH |
| APP-EXC-020 | 지정 교환 시작 | EXC-110 / /exchange-proposals/explore | IMPLEMENTED | A14 | APP-MENU-EXCHANGE-C1 |
| APP-EXC-030 | 내 가입 초대 | EXC-110 / /exchange-proposals/explore/invitations | IMPLEMENTED | A14 | APP-MENU-EXCHANGE-RECRUITMENT |
| APP-EXC-040 | 업체 마이크로홈 · 상대 확인 | EXC-110 / /exchange-proposals/explore/shop | IMPLEMENTED | A14 | — |
| APP-EXC-050 | 교환 제안서 작성 | EXC-120 / /exchange-proposals/compose | IMPLEMENTED | A14 | — |
| APP-EXC-060 | 제안 발송 · 수락 대기 | EXC-120 / /exchange-proposals/[id] | IMPLEMENTED | A14 | — |
| APP-EXC-070 | 제안 수락 · 핵심조항 동의 · 수락자 | EXC-120 / /exchange-proposals/[id]/consent-receiver | IMPLEMENTED | A14 | — |
| APP-EXC-080 | 교환계약 승인 · 쌍방 확정 | EXC-130 / /exchange-contracts/[id] | IMPLEMENTED | A14 | — |
| APP-EXC-090 | 제안 거절 · 무산 (예외) | EXC-120 / /exchange-proposals/[id]/declined | IMPLEMENTED | A14 | — |
| APP-EXC-100 | 내 모집·참여 | EXC-210 / /exchange-notices | IMPLEMENTED | A14 | APP-MENU-MARKET-HUB |
| APP-EXC-110 | 교환 모집 보기 | EXC-210 / /exchange-notices/board | IMPLEMENTED | A14 | APP-MENU-MARKET-DBOARD |
| APP-EXC-120 | 교환 모집 등록 | EXC-220 / /exchange-notices/compose | IMPLEMENTED | A14 | APP-MENU-MARKET-D1 |
| APP-EXC-130 | 교환 모집 조회·참여 | EXC-220 / /exchange-notices/[id] | IMPLEMENTED | A14 | — |
| APP-EXC-140 | 참여자 모집중 | EXC-220 / /exchange-notices/[id]/waiting | IMPLEMENTED | A14 | — |
| APP-EXC-150 | 의향 접수 완료 | EXC-220 / /exchange-notices/[id]/close | IMPLEMENTED | A14 | — |
| APP-EXC-160 | 교환 모집 참여자 선정 | EXC-220 / /exchange-notices/[id]/select | IMPLEMENTED | A14 | — |
| APP-EXC-170 | 선정 결과 통보 | EXC-220 / /exchange-notices/[id]/result | IMPLEMENTED | A14 | — |
| APP-EXC-180 | 요청자 동의 | EXC-220 / /exchange-notices/[id]/consent | IMPLEMENTED | A14 | — |
| APP-EXC-190 | 교환계약 일괄수행 및 통지 | EXC-230 / /exchange-notices/[id]/contracts | IMPLEMENTED | A14 | — |
| APP-EXC-200 | 교환계약하기(제안자) | EXC-230 / /exchange-notices/[id]/contracts/sign | IMPLEMENTED | A14 | — |
| APP-EXC-210 | 교환계약하기(참여자) | EXC-230 / /exchange-notices/[id]/contracts/countersign | IMPLEMENTED | A14 | — |
| APP-EXC-220 | 다시 모집 | EXC-240 / /exchange-notices/[id]/repost | IMPLEMENTED | A14 | — |
| APP-EXC-230 | 미선정 안내 | EXC-240 / /exchange-notices/[id]/not-selected | IMPLEMENTED | A14 | — |
| APP-HOM-010 | 메인 홈 | HOM-010 / /home | IMPLEMENTED | A14 | APP-MENU-ORDERS-HOME, APP-MENU-TAB-HOME |
| APP-HOM-020 | 이용 가이드 | HOM-020 / /home/guide | IMPLEMENTED | A14 | — |
| APP-HOM-030 | 메뉴 허브 | HOM-030 / /menu | IMPLEMENTED | A14 | — |
| APP-MEM-010 | 계정 관리 | MEM-010 / /members/me | IMPLEMENTED | A14 | APP-MENU-ACCOUNT-ACCOUNT, APP-MENU-TAB-ACCOUNT |
| APP-MEM-020 | 프로필 수정 | MEM-010 / /members/me/edit | IMPLEMENTED | A14 | — |
| APP-MEM-030 | 계정 관리 · 임직원 | MEM-010 / /members/me | IMPLEMENTED | A14 | — |
| APP-MEM-040 | 배송지 관리 | MEM-010 / /members/me/delivery-addresses | IMPLEMENTED | A14 | — |
| APP-MEM-050 | 아이디·비밀번호 만들기 | MEM-010 / /members/me/local-credential | IMPLEMENTED | A14 | — |
| APP-MEM-060 | 아이디 관리 | MEM-010 / /members/me/login-id | IMPLEMENTED | A14 | — |
| APP-MEM-070 | 회원 탈퇴 | MEM-020 / /members/me/leave | IMPLEMENTED | A14 | APP-MENU-ACCOUNT-LEAVE |
| APP-MEM-080 | 아이디 변경 | MEM-010 / /members/me/login-id/change | PLANNED | A14 | — |
| APP-MHM-010 | 마이크로홈 | MHM-010 / /microhomes/me | IMPLEMENTED | A14 | APP-MENU-STORE-MICRO |
| APP-MHM-020 | 내 가게 관리 | MHM-010 / /microhomes/me/edit | IMPLEMENTED | A14 | APP-MENU-STORE-MICROEDIT |
| APP-MHM-030 | 가게 정보 가져오기 | MHM-010 / /microhomes/me/business-information | IMPLEMENTED | A14 | — |
| APP-MHM-040 | 메뉴판 사진 등록 | MHM-010 / /microhomes/me/menu | IMPLEMENTED | A14 | — |
| APP-MHM-050 | 방문자 마이크로홈 (조회·주문) | MHM-020 / /microhomes/[id] | IMPLEMENTED | A14 | — |
| APP-MHM-060 | 리뷰 전체 보기 | MHM-020 / /microhomes/[id]/reviews | IMPLEMENTED | A14 | — |
| APP-NTF-010 | 알림함 | NTF-010 / /notifications | IMPLEMENTED | A14 | APP-MENU-ACCOUNT-NOTIS |
| APP-NTF-020 | 알림 상세 | NTF-010 / /notifications/[id] | IMPLEMENTED | A14 | — |
| APP-NTF-030 | 알림 설정 | NTF-020 / /notification-settings | IMPLEMENTED | A14 | APP-MENU-ACCOUNT-NOTISETTINGS |
| APP-ONB-010 | 업체 등록 | ONB-010 / /merchant-applications/business | IMPLEMENTED | A14 | — |
| APP-ONB-020 | 약관 동의 | ONB-010 / /merchant-applications/terms | IMPLEMENTED | A14 | — |
| APP-ONB-030 | 발급대행 위임 | ONB-010 / /merchant-applications/delegation | IMPLEMENTED | A14 | — |
| APP-ONB-040 | 접수 완료 | ONB-010 / /merchant-applications/submitted | IMPLEMENTED | A14 | — |
| APP-ONB-050 | 정보 불일치 | ONB-010 / /merchant-applications/mismatch | IMPLEMENTED | A14 | — |
| APP-ONB-060 | 환영 | ONB-010 / /merchant-applications/welcome | IMPLEMENTED | A14 | — |
| APP-ONB-070 | 기가입 안내 | ONB-010 / /merchant-applications/duplicate | IMPLEMENTED | A14 | — |
| APP-ONB-080 | 온보딩 홈 | ONB-020 / /home | IMPLEMENTED | A14 | — |
| APP-ORD-010 | 주문함 | ORD-100 / /orders | IMPLEMENTED | A14 | APP-MENU-ORDERS-ORDERS, APP-MENU-TAB-ACTIVITY |
| APP-ORD-020 | 주문 상세 | ORD-100 / /orders/[id] | IMPLEMENTED | A14 | — |
| APP-ORD-030 | 온라인 주문함 | ORD-110 / /orders | IMPLEMENTED | A14 | — |
| APP-ORD-040 | 온라인 주문하기 | ORD-110 / /orders/compose | IMPLEMENTED | A14 | — |
| APP-ORD-050 | 배달 정보 | ORD-110 / /orders/compose/delivery | IMPLEMENTED | A14 | — |
| APP-ORD-060 | 받은 주문 관리 | ORD-110 / /orders/manage | IMPLEMENTED | A14 | — |
| APP-ORD-070 | 사용요청 확인 | ORD-110 / /orders/[id]/accept | IMPLEMENTED | A14 | — |
| APP-ORD-080 | 배달 진행 | ORD-110 / /orders/[id]/progress | IMPLEMENTED | A14 | — |
| APP-ORD-090 | 실시간 추적 | ORD-110 / /orders/[id]/tracking | IMPLEMENTED | A14 | — |
| APP-ORD-100 | 수령 완료 확인 | ORD-110 / /orders/[id]/confirm | IMPLEMENTED | A14 | — |
| APP-ORD-110 | 주문 취소 | ORD-110 / /orders/[id]/canceled | IMPLEMENTED | A14 | — |
| APP-ORD-120 | e02/ohist | ORD-120 / /orders | IMPLEMENTED | A14 | — |
| APP-ORD-130 | e02/f2 | ORD-120 / /orders/compose | IMPLEMENTED | A14 | — |
| APP-ORD-140 | e02/f4 | ORD-120 / /orders/compose/delivery | IMPLEMENTED | A14 | — |
| APP-ORD-150 | e02/ohub | ORD-120 / /orders/manage | IMPLEMENTED | A14 | APP-MENU-ORDERS-OHUB |
| APP-ORD-160 | e02/f5 | ORD-120 / /orders/[id]/accept | IMPLEMENTED | A14 | — |
| APP-ORD-170 | e02/f6 | ORD-120 / /orders/[id]/progress | IMPLEMENTED | A14 | — |
| APP-ORD-180 | e02/f9 | ORD-120 / /orders/[id]/tracking | IMPLEMENTED | A14 | — |
| APP-ORD-190 | e02/f7 | ORD-120 / /orders/[id]/confirm | IMPLEMENTED | A14 | — |
| APP-ORD-200 | e02/f8 | ORD-120 / /orders/[id]/canceled | IMPLEMENTED | A14 | — |
| APP-ORD-210 | e02/cancelForm | ORD-120 / /orders/[id]/cancel | IMPLEMENTED | A14 | — |
| APP-ORD-220 | e02/rejected | ORD-120 / /orders/[id]/rejected | IMPLEMENTED | A14 | — |
| APP-REG-010 | 가입방식 선택 | REG-010 / /auth/signup | IMPLEMENTED | A14 | — |
| APP-REG-020 | SNS 로그인 | REG-010 / /auth/signup/oauth | IMPLEMENTED | A14 | — |
| APP-REG-030 | SNS 가입 완료 | REG-010 / /auth/signup/oauth/complete | IMPLEMENTED | A14 | — |
| APP-REG-040 | 계정 생성 | REG-010 / /auth/signup/account | IMPLEMENTED | A14 | — |
| APP-REG-050 | 실명 인증 | REG-020 / /auth/signup/identity | IMPLEMENTED | A14 | — |
| APP-REG-060 | 실명인증 약관동의 | REG-020 / /auth/signup/identity/terms | IMPLEMENTED | A14 | — |
| APP-SEC-010 | 비밀번호 변경 | SEC-010 / /members/me/password | IMPLEMENTED | A14 | — |
| APP-SEC-020 | 로그인 기기 | SEC-010 / /members/me/devices | IMPLEMENTED | A14 | — |
| APP-SEC-030 | 로그인 설정 | SEC-010 / /members/me/login-settings | IMPLEMENTED | A14 | — |
| APP-SET-010 | 앱 설정 | SET-010 / /settings | IMPLEMENTED | A14 | APP-MENU-ACCOUNT-SETTINGS |
| APP-STF-010 | 초대 링크 | STF-020 / /staff-accounts/invite | IMPLEMENTED | A14 | — |
| APP-STF-020 | 첫 로그인 | STF-020 / /staff-accounts/invite/login | IMPLEMENTED | A14 | — |
| APP-STF-030 | 비밀번호 설정 | STF-020 / /staff-accounts/invite/password | IMPLEMENTED | A14 | — |
| APP-STF-040 | 직원 홈 | STF-030 / /home | IMPLEMENTED | A14 | — |
| APP-STF-050 | 임직원 계정 | STF-010 / /staff-accounts | IMPLEMENTED | A14 | APP-MENU-STORE-STAFF |
| APP-STF-060 | 서브계정 만들기 | STF-010 / /staff-accounts/create | IMPLEMENTED | A14 | — |
| APP-STF-070 | 계정 발급·전달 | STF-010 / /staff-accounts/issue | IMPLEMENTED | A14 | — |
| APP-STF-080 | 서브계정 관리 | STF-010 / /staff-accounts/manage | IMPLEMENTED | A14 | — |
| APP-SUB-010 | 내 멤버십 | SUB-010 / /subscriptions | IMPLEMENTED | A14 | APP-MENU-ACCOUNT-MEMBERSHIP |
| APP-SUB-020 | 멤버십 결제 | SUB-010 / /subscriptions/pay | IMPLEMENTED | A14 | — |
| APP-SUB-030 | 멤버십 해지 | SUB-010 / /subscriptions/cancel | IMPLEMENTED | A14 | — |
| APP-SUP-010 | 고객센터 | SUP-010 / /support | IMPLEMENTED | A14 | — |
| APP-SUP-020 | 공지사항 | SUP-020 / /support/notices | IMPLEMENTED | A14 | APP-MENU-SUPPORT-NOTICES |
| APP-SUP-030 | 공지 상세 | SUP-020 / /support/notices/[id] | IMPLEMENTED | A14 | — |
| APP-SUP-040 | 자주 묻는 질문 | SUP-030 / /support/faq | IMPLEMENTED | A14 | APP-MENU-SUPPORT-FAQ |
| APP-SUP-050 | 1:1 문의 | SUP-040 / /inquiries | IMPLEMENTED | A14 | APP-MENU-SUPPORT-QNALIST |
| APP-SUP-060 | 문의하기 | SUP-040 / /inquiries/compose | IMPLEMENTED | A14 | — |
| APP-SUP-070 | 문의 상세 | SUP-040 / /inquiries/[id] | IMPLEMENTED | A14 | — |
| APP-SUP-080 | 골목 매거진 | SUP-050 / /support/magazine | IMPLEMENTED | A14 | APP-MENU-SUPPORT-BLOG |
| APP-SUP-090 | 매거진 글 | SUP-050 / /support/magazine/[id] | IMPLEMENTED | A14 | — |
| APP-SUP-100 | 내 신고함 | SUP-070 / /reports | IMPLEMENTED | A14 | — |
| APP-SUP-110 | 신고하기 | SUP-070 / /reports/compose | IMPLEMENTED | A14 | — |
| APP-SUP-120 | 이벤트 | SUP-060 / /support/events | IMPLEMENTED | A14 | APP-MENU-SUPPORT-EVENTS |
| APP-SUP-130 | 이벤트 상세 | SUP-060 / /support/events/[id] | IMPLEMENTED | A14 | — |
| APP-SYS-010 | 로딩 상태 전시 | SYS-010 / /loading-showcase | PLANNED | A14 | — |
| APP-SYS-020 | 골목사장 화면 허브 | SYS-020 / / | PLANNED | A14 | — |
| APP-TAX-010 | 세금계산서함 | TAX-010 / /tax-invoices | IMPLEMENTED | A14 | APP-MENU-ASSET-TAX |
| APP-TAX-020 | 계산서 상세 | TAX-010 / /tax-invoices/[id] | IMPLEMENTED | A14 | — |
| APP-TAX-030 | 수정 발행 | TAX-020 / /tax-invoices/[id]/amend | IMPLEMENTED | A14 | — |
| APP-TAX-040 | 수정·취소 발행 | TAX-020 / /tax-invoices/[id]/cancel | IMPLEMENTED | A14 | — |
| APP-TAX-050 | 신고지원 | TAX-030 / /tax-reports | IMPLEMENTED | A14 | — |
| APP-TRX-010 | 매장결제 | TRX-020 / /transactions | IMPLEMENTED | A14 | APP-MENU-PAYMENT-EHUB |
| APP-TRX-020 | 수령 관리함 | TRX-020 / /transactions/manage | IMPLEMENTED | A14 | — |
| APP-TRX-030 | 이의제기 | TRX-090 / /disputes/file | IMPLEMENTED | A14 | — |
| APP-TRX-040 | 매장결제 요청하기 | TRX-020 / /transactions/new | IMPLEMENTED | A14 | APP-MENU-PAYMENT-PAYMENTREQUEST |
| APP-TRX-050 | 결제요청 확인 | TRX-020 / /transactions/payment-requests/[id] | IMPLEMENTED | A14 | — |
| APP-TRX-060 | e02/ecomplete | TRX-030 / /orders/[id]/completed | IMPLEMENTED | A14 | — |
| APP-TRX-070 | e02/dispute | TRX-090 / /disputes/file | IMPLEMENTED | A14 | — |
| APP-VCH-010 | 내 바우처 | VCH-010 / /vouchers | IMPLEMENTED | A14 | APP-MENU-ASSET-VOUCHERS, APP-MENU-TAB-TICKET |
| APP-VCH-020 | 바우처 상세·결제 | VCH-010 / /vouchers/[id] | IMPLEMENTED | A14 | — |
| APP-VCH-030 | 사용권한 관리 | VCH-020 / /vouchers/[id]/permissions | IMPLEMENTED | A14 | — |
| APP-VCH-040 | 내 바우처 (직원) | VCH-010 / /vouchers/assigned | IMPLEMENTED | A14 | — |
| BO-CNT-010 | 공지 작성·게시 | S-01 / /s-01 | IMPLEMENTED | A08 | BO-MENU-CNT-NOTICE |
| BO-CNT-020 | FAQ 관리 | S-10 / /s-10 | IMPLEMENTED | A08 | BO-MENU-CNT-FAQ |
| BO-CUS-010 | 문의 처리 현황 | C-01 / /c-01 | IMPLEMENTED | A09 | BO-MENU-CUS-STATUS |
| BO-CUS-020 | 문의 찾기 | C-10 / /c-10 | IMPLEMENTED | A09 | BO-MENU-CUS-SEARCH |
| BO-CUS-030 | 문의 확인·답변 | C-11 / /c-11 | IMPLEMENTED | A09 | BO-MENU-CUS-ANSWER |
| BO-CUS-040 | 분쟁·이의 목록 | C-20 / /c-20 | IMPLEMENTED | A09 | BO-MENU-CUS-DISPUTES |
| BO-CUS-050 | 분쟁·이의 처리 | C-21 / /c-21 | IMPLEMENTED | A09 | BO-MENU-CUS-RESOLVE |
| BO-CUS-060 | 매장 정보·후기 | C-30 / /c-30 | IMPLEMENTED | A09 | BO-MENU-CUS-REVIEWS |
| BO-CUS-070 | 매장 정보 확인 | C-31 / /c-31 | IMPLEMENTED | A09 | BO-MENU-CUS-STORE |
| BO-CUS-080 | 신고 확인·처리 | C-40 / /c-40 | IMPLEMENTED | A09 | BO-MENU-CUS-REPORT |
| BO-DSH-010 | 운영 현황 대시보드 | D-01 / /d-01 | IMPLEMENTED | A08 | BO-MENU-DSH-OVERVIEW |
| BO-ISS-010 | 이슈 현황 | I-01 / /i-01 | IMPLEMENTED | A08 | BO-MENU-ISS-STATUS |
| BO-ISS-020 | 이슈 상세 | I-20 / /i-20 | IMPLEMENTED | A08 | BO-MENU-ISS-DETAIL |
| BO-ISS-030 | 이슈 등록 | I-30 / /i-30 | IMPLEMENTED | A08 | BO-MENU-ISS-CREATE |
| BO-MBR-010 | 회원 찾기 | M-01 / /m-01 | IMPLEMENTED | A08 | BO-MENU-MBR-SEARCH |
| BO-MBR-020 | 회원 정보·처리 | M-02 / /m-02 | IMPLEMENTED | A08 | BO-MENU-MBR-DETAIL |
| BO-MBR-030 | 가입 신청 | M-10 / /m-10 | IMPLEMENTED | A08 | BO-MENU-MBR-APPLICATIONS |
| BO-MBR-040 | 가입 신청 확인 | M-11 / /m-11 | IMPLEMENTED | A08 | BO-MENU-MBR-APPLICATION |
| BO-MBR-050 | 가입 정보 확인 | M-20 / /m-20 | IMPLEMENTED | A08 | BO-MENU-MBR-PROFILE |
| BO-MBR-060 | 골목가입추천 | M-40 / /m-40 | IMPLEMENTED | A08 | BO-MENU-MBR-REFERRALS |
| BO-MBR-070 | 탈퇴·정보 정리 | M-30 / /m-30 | IMPLEMENTED | A08 | BO-MENU-MBR-WITHDRAWAL |
| BO-MBR-080 | 업체정보 확인 | M-50 / /m-50 | IMPLEMENTED | A08 | BO-MENU-MBR-BUSINESS |
| BO-POL-010 | 공통코드 조회 | K-01 / /k-01 | IMPLEMENTED | A08 | BO-MENU-POL-CODES |
| BO-POL-020 | 사유코드 관리 | K-10 / /k-10 | PLANNED | A08 | BO-MENU-POL-REASONS |
| BO-POL-030 | 업종코드 관리 | K-20 / /k-20 | IMPLEMENTED | A08 | BO-MENU-POL-INDUSTRIES |
| BO-POL-040 | 알림 템플릿 관리 | K-30 / /k-30 | IMPLEMENTED | A08 | BO-MENU-POL-TEMPLATES |
| BO-POL-050 | 멤버십관리 | K-40 / /k-40 | IMPLEMENTED | A08 | BO-MENU-POL-MEMBERSHIP |
| BO-POL-060 | 약관 관리 | K-50 / /k-50 | IMPLEMENTED | A08 | BO-MENU-POL-TERMS |
| BO-POL-070 | 기능 정책·긴급 차단 | K-60 / /k-60 | IMPLEMENTED | A08 | BO-MENU-POL-FEATURES |
| BO-SEC-010 | 관리자 계정 관리 | Z-01 / /z-01 | IMPLEMENTED | A09 | BO-MENU-SEC-ADMINS |
| BO-SEC-020 | 권한 관리 안내 | Z-02 / /z-02 | PLANNED | A09 | BO-MENU-SEC-PERMISSIONS |
| BO-SEC-030 | 보안 기록 확인 | Z-10 / /z-10 | IMPLEMENTED | A09 | BO-MENU-SEC-AUDIT |
| BO-SEC-040 | 접속 관리 | Z-20 / /z-20 | IMPLEMENTED | A09 | BO-MENU-SEC-ACCESS |
| BO-SEC-050 | 작업 이력 확인 | Z-30 / /z-30 | IMPLEMENTED | A09 | BO-MENU-SEC-HISTORY |
| BO-SHL-010 | login | — / /login | IMPLEMENTED | A02 | — |
| BO-SHL-020 | guide | — / /guide | IMPLEMENTED | A02 | BO-MENU-SHL-GUIDE |
| BO-SHL-030 | SetupGate | — / 주소 없음 | IMPLEMENTED | A02 | — |
| BO-STA-010 | 업무 통계 | A-01 / /a-01 | IMPLEMENTED | A10 | BO-MENU-STA-WORK |
| BO-STA-020 | 회원 통계 | A-10 / /a-10 | IMPLEMENTED | A10 | BO-MENU-STA-MEMBERS |
| BO-STA-030 | 거래·교환 통계 | A-20 / /a-20 | IMPLEMENTED | A10 | BO-MENU-STA-TRADES |
| BO-STA-040 | 바우처·장부 통계 | A-30 / /a-30 | IMPLEMENTED | A10 | BO-MENU-STA-VOUCHERS |
| BO-STA-050 | 고객 응대 통계 | A-40 / /a-40 | IMPLEMENTED | A10 | BO-MENU-STA-SUPPORT |
| BO-STA-060 | 알림·발송 통계 | A-50 / /a-50 | IMPLEMENTED | A10 | BO-MENU-STA-NOTIFICATIONS |
| BO-STA-070 | 기능 관심 통계 | A-60 / /a-60 | IMPLEMENTED | A10 | BO-MENU-STA-INTEREST |
| BO-SYS-010 | 연동·시스템 상태 | Y-01 / /y-01 | IMPLEMENTED | A10 | BO-MENU-SYS-STATUS |
| BO-SYS-020 | 자동 처리 확인 | Y-10 / /y-10 | IMPLEMENTED | A10 | BO-MENU-SYS-AUTOMATION |
| BO-SYS-030 | 후속 처리 확인 | Y-20 / /y-20 | IMPLEMENTED | A10 | BO-MENU-SYS-FOLLOWUP |
| BO-SYS-040 | 메시지 발송 관리 | Y-30 / /y-30 | PLANNED | A10 | BO-MENU-SYS-MESSAGES |
| BO-SYS-050 | 파일 정책·스토리지 | Y-40 / /y-40 | IMPLEMENTED | A10 | BO-MENU-SYS-STORAGE |
| BO-SYS-060 | 업체정보 배치 수신 | Y-50 / /y-50 | IMPLEMENTED | A10 | BO-MENU-SYS-BUSINESS-IMPORT |
| BO-SYS-070 | 업체 정보 보완 | Y-60 / /y-60 | IMPLEMENTED | A10 | BO-MENU-SYS-BUSINESS-ENRICH |
| BO-SYS-080 | 매장 메뉴 등록 | Y-70 / /y-70 | IMPLEMENTED | A10 | BO-MENU-SYS-STORE-MENU |
| BO-SYS-090 | 서비스 기록 파일 | Y-80 / /y-80 | IMPLEMENTED | A10 | BO-MENU-SYS-LOGS |
| BO-TAX-010 | 정산·세금 현황 | X-01 / /x-01 | IMPLEMENTED | A09 | BO-MENU-TAX-STATUS |
| BO-TAX-020 | 세금계산서 목록 | X-10 / /x-10 | IMPLEMENTED | A09 | BO-MENU-TAX-INVOICES |
| BO-TAX-030 | 세금계산서 내역 | X-11 / /x-11 | IMPLEMENTED | A09 | BO-MENU-TAX-INVOICE |
| BO-TAX-040 | 장부·잔액 확인 | X-20 / /x-20 | IMPLEMENTED | A09 | BO-MENU-TAX-LEDGER |
| BO-TAX-050 | 잔액 차이 확인 | X-21 / /x-21 | IMPLEMENTED | A09 | BO-MENU-TAX-BALANCE |
| BO-TAX-060 | 조정 내역 안내 | X-30 / /x-30 | PLANNED | A09 | BO-MENU-TAX-ADJUSTMENT |
| BO-TRD-010 | 거래 문제 현황 | T-01 / /t-01 | IMPLEMENTED | A11 | BO-MENU-TRD-STATUS |
| BO-TRD-020 | 교환 진행 조회 | T-10 / /t-10 | IMPLEMENTED | A11 | BO-MENU-TRD-EXCHANGES |
| BO-TRD-030 | 교환·계약 내역 | T-11 / /t-11 | IMPLEMENTED | A11 | BO-MENU-TRD-CONTRACT |
| BO-TRD-040 | 거래 문제 확인 | T-12 / /t-12 | IMPLEMENTED | A11 | BO-MENU-TRD-PROBLEM |
| BO-TRD-050 | 바우처 조회 | T-20 / /t-20 | IMPLEMENTED | A11 | BO-MENU-TRD-VOUCHERS |
| BO-TRD-060 | 바우처 상세 | T-21 / /t-21 | IMPLEMENTED | A11 | BO-MENU-TRD-VOUCHER |
| BO-TRD-070 | 거래·주문 조회 | T-30 / /t-30 | IMPLEMENTED | A11 | BO-MENU-TRD-ORDERS |
| BO-TRD-080 | 거래 상세 | T-31 / /t-31 | IMPLEMENTED | A11 | BO-MENU-TRD-ORDER |
| BO-TRD-090 | 요청 처리 기록 | T-90 / /t-90 | IMPLEMENTED | A11 | BO-MENU-TRD-REQUESTS |
| BO-TRD-100 | 누락·실패 보정 | R-10 / /r-10 | IMPLEMENTED | A11 | BO-MENU-TRD-REPAIR |
| BO-WRK-010 | 오늘 할 일 | W-00 / /w-00 | IMPLEMENTED | A02 | BO-MENU-WRK-TODAY |
| BO-WRK-020 | 가입 신청 처리 | W-01 / /w-01 | IMPLEMENTED | A02 | BO-MENU-WRK-SIGNUP |
| BO-WRK-030 | 고객 응대 처리 | W-02 / /w-02 | IMPLEMENTED | A02 | BO-MENU-WRK-SUPPORT |
| BO-WRK-040 | 거래 문제 해결 | W-03 / /w-03 | IMPLEMENTED | A02 | BO-MENU-WRK-TRADE |
| BO-WRK-050 | 바우처정산 | W-04 / /w-04 | IMPLEMENTED | A02 | BO-MENU-WRK-SETTLE |
| BO-WRK-060 | 가입지원 | W-05 / /w-05 | IMPLEMENTED | A02 | BO-MENU-WRK-RECRUIT |

## 진단

```json
{
  "ambiguous": [
    {
      "candidates": [
        "APP-ADM-020",
        "APP-ADM-030"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ADM-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ADM-040",
        "APP-ADM-050"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ADM-030",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ADM-060",
        "APP-ADM-070"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ADM-040",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ADM-080",
        "APP-ADM-090"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ADM-050",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ADM-100",
        "APP-ADM-110"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ADM-060",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-AUT-010",
        "APP-AUT-020",
        "APP-AUT-030",
        "APP-AUT-090",
        "APP-AUT-100"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "AUT-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-AUT-050",
        "APP-AUT-060",
        "APP-AUT-070",
        "APP-AUT-080"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "AUT-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-HOM-010",
        "APP-ONB-080"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "B.1",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-CHT-030",
        "APP-CHT-040",
        "APP-CHT-050",
        "APP-CHT-060"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "CHT-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-EXC-010",
        "APP-EXC-020",
        "APP-EXC-030",
        "APP-EXC-040"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "EXC-110",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-EXC-050",
        "APP-EXC-060",
        "APP-EXC-070",
        "APP-EXC-090"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "EXC-120",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-EXC-100",
        "APP-EXC-110"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "EXC-210",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-EXC-120",
        "APP-EXC-130",
        "APP-EXC-140",
        "APP-EXC-150",
        "APP-EXC-160",
        "APP-EXC-170",
        "APP-EXC-180"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "EXC-220",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-EXC-190",
        "APP-EXC-200",
        "APP-EXC-210"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "EXC-230",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-EXC-220",
        "APP-EXC-230"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "EXC-240",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-MEM-010",
        "APP-MEM-020",
        "APP-MEM-030",
        "APP-MEM-040",
        "APP-MEM-050",
        "APP-MEM-060",
        "APP-MEM-080"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "MEM-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-MHM-010",
        "APP-MHM-020",
        "APP-MHM-030",
        "APP-MHM-040"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "MHM-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-MHM-050",
        "APP-MHM-060"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "MHM-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-NTF-010",
        "APP-NTF-020"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "NTF-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ONB-010",
        "APP-ONB-020",
        "APP-ONB-030",
        "APP-ONB-040",
        "APP-ONB-050",
        "APP-ONB-060",
        "APP-ONB-070"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ONB-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-010",
        "APP-ORD-020"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ORD-100",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-030",
        "APP-ORD-040",
        "APP-ORD-050",
        "APP-ORD-060",
        "APP-ORD-070",
        "APP-ORD-080",
        "APP-ORD-090",
        "APP-ORD-100",
        "APP-ORD-110"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ORD-110",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-120",
        "APP-ORD-130",
        "APP-ORD-140",
        "APP-ORD-150",
        "APP-ORD-160",
        "APP-ORD-170",
        "APP-ORD-180",
        "APP-ORD-190",
        "APP-ORD-200",
        "APP-ORD-210",
        "APP-ORD-220"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "ORD-120",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-REG-010",
        "APP-REG-020",
        "APP-REG-030",
        "APP-REG-040"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "REG-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-REG-050",
        "APP-REG-060"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "REG-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-TAX-030",
        "APP-TAX-040"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "S.1.b",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-SEC-010",
        "APP-SEC-020",
        "APP-SEC-030"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "SEC-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-STF-050",
        "APP-STF-060",
        "APP-STF-070",
        "APP-STF-080"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "STF-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-STF-010",
        "APP-STF-020",
        "APP-STF-030"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "STF-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-SUB-010",
        "APP-SUB-020",
        "APP-SUB-030"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "SUB-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-SUP-020",
        "APP-SUP-030"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "SUP-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-SUP-050",
        "APP-SUP-060",
        "APP-SUP-070"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "SUP-040",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-SUP-080",
        "APP-SUP-090"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "SUP-050",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-SUP-120",
        "APP-SUP-130"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "SUP-060",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-SUP-100",
        "APP-SUP-110"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "SUP-070",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-TAX-010",
        "APP-TAX-020"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "TAX-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-TAX-030",
        "APP-TAX-040"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "TAX-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-TRX-010",
        "APP-TRX-020",
        "APP-TRX-040",
        "APP-TRX-050"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "TRX-020",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-TRX-030",
        "APP-TRX-070"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "TRX-090",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-VCH-010",
        "APP-VCH-020",
        "APP-VCH-040"
      ],
      "kind": "LEGACY",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "VCH-010",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-CHT-010",
        "APP-CHT-020",
        "APP-CHT-040"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/chat-rooms/[id]",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-TRX-030",
        "APP-TRX-070"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/disputes/file",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-HOM-010",
        "APP-ONB-080",
        "APP-STF-040"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/home",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-MEM-010",
        "APP-MEM-030"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/members/me",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-010",
        "APP-ORD-030",
        "APP-ORD-120"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-070",
        "APP-ORD-160"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/[id]/accept",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-110",
        "APP-ORD-200"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/[id]/canceled",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-100",
        "APP-ORD-190"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/[id]/confirm",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-080",
        "APP-ORD-170"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/[id]/progress",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-090",
        "APP-ORD-180"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/[id]/tracking",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-040",
        "APP-ORD-130"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/compose",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-050",
        "APP-ORD-140"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/compose/delivery",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "APP-ORD-060",
        "APP-ORD-150"
      ],
      "kind": "ROUTE",
      "reason": "원본에서 여러 화면이 공유하는 값. internal_id 또는 binding으로 식별한다.",
      "status": "AMBIGUOUS",
      "system": "APP",
      "target": null,
      "value": "/orders/manage",
      "version": "1.0.0-ui.9"
    },
    {
      "candidates": [
        "BO-ISS-010",
        "BO-ISS-020"
      ],
      "kind": "LEGACY",
      "reason": "registry의 issues 구현을 I-01/I-20과 공유. 메뉴에 없고 ScreenHost는 메뉴 미등록 ID를 404 처리하므로 유일 대상 미확정.",
      "status": "AMBIGUOUS",
      "system": "BO",
      "target": null,
      "value": "I-10",
      "version": "1.0.0-ui.9"
    }
  ],
  "coverage": {
    "denominator": 444,
    "preserved": 444,
    "preservedPercent": 100.0,
    "uniqueResolved": 443
  },
  "duplicates": 0,
  "errors": [],
  "expected": {
    "menuAreas": 12,
    "menuItems": 72,
    "registry": 69,
    "standardScreens": 75
  },
  "measured": {
    "appExcludedAliases": 53,
    "appMenus": 33,
    "appRegisteredAliases": 598,
    "appRoutes": 138,
    "appScreens": 156,
    "menuAreas": 12,
    "menuItems": 72,
    "menuWithGuide": 106,
    "registry": 69,
    "sourceUnionWithShell": 76,
    "standardScreens": 231
  },
  "planned": 22,
  "unconfirmed": [
    "I-10 unique target",
    "menu semantic item identifiers require user review",
    "environment active version and operational grants deferred to B3"
  ]
}
```

- registry69 + menu전용4 + shell3의 합집합은76개. I-10은 표준번호가 없으므로 표준화면은75개.
- SetupGate는 별도 주소 없이 인증된 라우트에서 조건부 표시. guide는 App.tsx에 있으나 menu.ts 항목이 아니므로 보조 메뉴로 등록.
- 메뉴 항목 영문은 설계 STATUS 패턴으로 제안한 정본 식별자이며 사용자 검토 전이다.
