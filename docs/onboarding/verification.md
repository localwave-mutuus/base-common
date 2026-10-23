# 전달 전 확인

REQ-261023-02 · AC-23-T1-J1 · 2026-10-23

- 이전 시도의 무자격 `mvn help:effective-settings` 종료 코드 0을 재사용했다. 반복하지 않았다.
- 기존 읽기 자격은 메모 형태여서 원문을 Authorization 헤더에 넣으면 UnicodeEncodeError가 발생했다. 실행 메모리에서 유일한 토큰만 추출했고 값은 출력·저장하지 않았다. GitHub `/user` HTTP 200에서 login을 확인하고 응답 헤더의 `read:packages` 범위를 확인했다.
- 해당 login과 기존 읽기 자격으로 `check-common-platform.sh`를 1회 실행해 `ai.mutuus.common:common-platform:0.6.9:pom`을 신규 임시 캐시에 받았다(PASS, 종료 코드 0). 자격·Maven 원문 로그는 산출물에 넣지 않았다.
- CI 문서를 PM 첨부 A06 §1–7과 대조했다. local·웹 경로 없음, dev 자동 배포, 최소 시험 명령, 권한 요청, 결과 경로, 캐시·계정·MEM 설정 미확인을 반영했다.
- 제공 읽기 revision의 여섯 BOM import 판을 확인했다. A06 요약과 판 차이는 없고 BATCH batch-member import를 추가 명시했다.
- zip은 동일 이름의 상위 폴더 아래 이 디렉터리 파일을 모두 포함한다. 파일 목록 및 바이트 일치를 확인했다. 비밀 관련 키 이름·일반 설명과 실제 값을 구분해 검토했으며 실제 토큰·비밀번호·내부 IP·비밀 보관 경로는 포함하지 않았다.
- Windows 실행, 제품 빌드·시험, ABI/API 호환성, 서버 시험·배포, 실제 외부 계정 역할, 러너 캐시 판, MEM GitLab CI 설정값은 확인하지 않았다. 전체 시험·파이프라인은 실행하지 않았다.
