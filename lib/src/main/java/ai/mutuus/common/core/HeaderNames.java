package ai.mutuus.common.core;

/**
 * API 간 전파되는 공통 커스텀 헤더 이름.
 * <p>e2e 정보 계층: 추적ID, 화면ID, 이벤트ID, 단말 수준, 사용자/로케일 등을
 * 인입 시점에 추출하여 MDC에 적재하고, 하위 API 호출 시 자동 부착한다.
 */
public final class HeaderNames {

    private HeaderNames() {
    }

    /** 트랜잭션 글로벌 추적 ID (UUID). 최초 진입 게이트웨이/필터에서 생성. */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 단일 API 호출 구간 식별 ID. */
    public static final String SPAN_ID = "X-Span-Id";

    /** 화면 ID — 메뉴 구조 및 화면 식별. */
    public static final String SCREEN_ID = "X-Screen-Id";

    /** 이벤트 ID — 화면 내 이벤트 발생 버튼/액션 식별. */
    public static final String EVENT_ID = "X-Event-Id";

    /** 단말기 수준 식별 (예: WEB / ANDROID / IOS / KIOSK). */
    public static final String DEVICE_LEVEL = "X-Device-Level";

    /** 단말기 고유 식별자. */
    public static final String DEVICE_ID = "X-Device-Id";

    /** 인증된 사용자 ID (인증 MSA 연계 결과). */
    public static final String USER_ID = "X-User-Id";

    /** 사용자 로케일 (다국어 처리용, 예: ko-KR). */
    public static final String LOCALE = "X-Locale";

    /** 어플리케이션코드 (숫자/영문 4자리) — 호출 주체 애플리케이션 식별. */
    public static final String APP_CODE = "X-App-Code";

    /** 인스턴스구분코드 (숫자/영문 6자리) — 호출 주체 인스턴스 식별. */
    public static final String INSTANCE_ID = "X-Instance-Id";

    /** 전파 대상 헤더 전체 목록 (인터셉터에서 순회). */
    public static final String[] PROPAGATED = {
            TRACE_ID, SPAN_ID, SCREEN_ID, EVENT_ID, DEVICE_LEVEL, DEVICE_ID, USER_ID, LOCALE,
            APP_CODE, INSTANCE_ID
    };

    /**
     * <b>식별/민감</b> 헤더 — 사용자·단말·내부 토폴로지를 드러낸다. 신뢰 호스트가 아닌 아웃바운드 호출에는
     * 부착하지 않는다(외부 유출 방지). 상관용 {@link #TRACE_ID}/{@link #SPAN_ID}/{@link #LOCALE} 는 제외(항상 전파 허용).
     */
    public static final java.util.Set<String> IDENTITY = java.util.Set.of(
            USER_ID, DEVICE_LEVEL, DEVICE_ID, SCREEN_ID, EVENT_ID, APP_CODE, INSTANCE_ID);

    /**
     * 인입 {@code X-User-Id}(신뢰 불가 주장값)를 담는 <b>요청 속성</b> 이름. TraceFilter(web)가 세팅하고
     * AuthenticatedUserContextFilter(security)가 인증 주체와 비교(스푸핑 탐지)한다 — 두 패키지가 공유하는
     * 상수라 무의존 {@code core} 에 둔다.
     */
    public static final String CLAIMED_USER_ATTR = "ai.mutuus.common.claimedUserId";
}
