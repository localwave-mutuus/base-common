# 인증 실패 오류 코드 — 완료

- 기준 커밋: `d500946bb7dd23cc52b8c4a7b84a24b0da096631`
- 착수 시 기존 변경 없음. 첨부된 앞 패치의 로거 3개와 최소 시험을 반영했다.
- CanonicalLogSnapshot의 기존 코드 허용 정규식에 아래 다섯 코드를 명시적으로 추가했다. 범용 허용이나 ActionLogger 검증 마커 우회는 없다.

## 코드 보존 근거

| 코드 | 로그 분류 근거 | 최종 JSON 확인 |
|---|---|---|
| AUTH_TOKEN_MISSING | AuthenticationCredentialsNotFoundException 또는 Authorization 헤더 없는 InsufficientAuthenticationException(entry point) | 헤더 없는 entry point에서 두 인증 이벤트 |
| AUTH_TOKEN_INVALID | OAuth2AuthenticationException의 invalid_token | 두 인증 이벤트 |
| AUTH_TOKEN_EXPIRED | cause의 JwtValidationException 오류 설명이 표준 `Jwt expired at `으로 시작 | 두 인증 이벤트 |
| AUTHN_FAILED | 구분 근거 없는 나머지 인증 실패 | 두 인증 이벤트 |
| AUTH_PROPAGATION_BLOCKED | security.propagation.blocked | 전파 차단 이벤트 |

일반 메시지와 사용자 정의 JWT 예외는 추측하지 않는다. 기존 이벤트 이름·필드, OAuth2 예외의 security.jwt.rejected 분기, delegate 호출 및 인증·권한·헤더 전파 판단을 유지했다. 변경은 로그용 분류와 필드 추가 및 직렬화 허용 목록에 한정된다.

## 최소 검증

필요성: 앞서 실패한 JSON 치환 문제와 아직 실행되지 않은 INVALID/EXPIRED/기타 분류를 실제 SnapshotAppender → CommonJsonEncoder 경로에서 확인한다.

명령: `bash ./mvnw -pl lib -Dtest=AuthErrorCodeTest test`

결과: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS.
JSON 9건에서 다섯 코드 보존을 확인했다. 이미 통과한 같은 입력의 시험 및 전체 회귀는 실행하지 않았다.

## 세 이벤트 JSON 각 1건

```json
{"@timestamp":"2026-10-22T10:03:57.029564323Z","event":{"id":"3d7d77e8-6683-42fe-befd-064b5509691a","dataset":"mutuus.access","action":"auth.failure","outcome":"failure"},"golmok":{"schema":{"version":2},"auth":{"state":"anonymous"},"scope":"lifecycle","attributes":{"unmapped_count":0}},"message":"telemetry.event","log":{"level":"warn","severity_number":13,"logger":"ai.mutuus.common.access"},"service":{"name":"unknown-service"},"deployment":{"environment":{"name":"local"}},"url":{"path":"/protected"},"error":{"code":"AUTH_TOKEN_MISSING","type":"org.springframework.security.authentication.InsufficientAuthenticationException"}}
```

```json
{"@timestamp":"2026-10-22T10:03:57.105278597Z","event":{"id":"04b49e9d-7c65-4e6f-b860-417d52cfe040","dataset":"mutuus.security_audit","action":"security.authn.failed","category":["authentication","iam"],"type":["denied"],"outcome":"failure"},"golmok":{"schema":{"version":2},"auth":{"state":"anonymous"},"scope":"lifecycle","attributes":{"unmapped_count":0},"security":{"event":true}},"message":"telemetry.event","log":{"level":"warn","severity_number":13,"logger":"ai.mutuus.common.security.audit"},"service":{"name":"unknown-service"},"deployment":{"environment":{"name":"local"}},"mutuus":{"security":{"disposition":"denied"}},"url":{"path":"/protected"},"error":{"type":"org.springframework.security.authentication.InsufficientAuthenticationException","code":"AUTH_TOKEN_MISSING"},"client":{"ip":"127.0.0.0"}}
```

```json
{"@timestamp":"2026-10-22T10:03:57.109079255Z","event":{"id":"3b40de3c-ddbe-4a78-80b8-546cd339e86b","dataset":"mutuus.security_audit","action":"security.propagation.blocked","category":["network"],"type":["denied"],"outcome":"failure"},"golmok":{"schema":{"version":2},"auth":{"state":"anonymous"},"scope":"lifecycle","attributes":{"unmapped_count":0},"security":{"event":true,"target_host":"untrusted.invalid","dropped_headers":"Authorization"}},"message":"telemetry.event","log":{"level":"warn","severity_number":13,"logger":"ai.mutuus.common.security.audit"},"service":{"name":"unknown-service"},"deployment":{"environment":{"name":"local"}},"mutuus":{"security":{"disposition":"blocked"}},"error":{"code":"AUTH_PROPAGATION_BLOCKED"}}
```

## 제출물과 확인 범위

- `TEAM_OUT_DIR/auth-error-code.patch`: 기준 커밋 대비 완결 patch(로거 3개, CanonicalLogSnapshot, 최소 시험, 이 문서).
- `TEAM_OUT_DIR/auth-error-code-test.log`: 시험 전체 출력 및 JSON 9건.
- 확인하지 못함: 전체 회귀와 배포 환경 동작(요청 범위 밖).

## 이슈대처 후보

- 증상: 로거의 AUTH_TOKEN_MISSING 등이 최종 JSON에서 OPERATION_FAILED로 치환됨.
- 원인: CanonicalLogSnapshot의 error.code 허용 정규식에 새 코드가 없었음.
- 확인 명령: `bash ./mvnw -pl lib -Dtest=AuthErrorCodeTest test`
- 조치: 다섯 코드만 명시적으로 허용.
- 재발 방지: 새 로그 코드는 최종 snapshot/encoder 경로로 보존 여부를 검증한다.

## 교훈 후보

### 로그 코드 추가 시 최종 직렬화 허용 목록도 확인한다
#### 상황
로거 필드에는 새 인증 코드가 있었지만 최종 JSON에서 소실됐다.
#### 원인
CanonicalLogSnapshot의 코드 허용 정규식에서 제외됐다.
#### 교훈 (앞으로 이렇게 한다)
코드 생산부와 최종 직렬화 허용 목록을 함께 확인하고 JSON 경로로 검증한다.
#### 근거
첨부 실패 결과는 예상 AUTH_TOKEN_MISSING, 실제 OPERATION_FAILED였다. 이번 최소 시험은 JSON 9건의 다섯 코드 보존에 성공했다.

교훈 적용: kb://core/ops/lessons/topic-9d939aa20431030e63ab6c76.md
교훈 적용: kb://core/ops/lessons/topic-7cf3713837ef72f382ed8f96.md (제출 patch에서 시험 출력·빌드 산출물 제외)
교훈 무관: kb://core/ops/lessons/topic-a2dac7103214487eb0ecf391.md (INFRA·CICD 저장소가 아님)
