package ai.mutuus.common.datasource;

/**
 * 읽기/쓰기 라우팅 역할을 스레드 단위로 <b>명시 지정</b>하는 컨텍스트(ThreadLocal).
 * <p>보통은 지정하지 않아도 {@code @Transactional(readOnly)} 플래그로 자동 라우팅되지만
 * ({@link ReadWriteRoutingDataSource} 참조), 트랜잭션 밖에서 강제로 Replica 를 태우고 싶을 때 등
 * 예외적으로 {@link #set(DbRole)} 로 덮어쓴다. <b>사용 후 반드시 {@link #clear()}</b>
 * (스레드풀 재사용 시 누수 방지 — {@code TraceContext} 와 동일 규율).
 */
public final class RoutingContext {

    private static final ThreadLocal<DbRole> HOLDER = new ThreadLocal<>();

    private RoutingContext() {
    }

    /** 이 스레드의 라우팅 역할을 명시 지정한다. */
    public static void set(DbRole role) {
        HOLDER.set(role);
    }

    /** 명시 지정된 역할(없으면 {@code null}). */
    public static DbRole peek() {
        return HOLDER.get();
    }

    /** 명시 지정 해제(요청/작업 종료 시 필수). */
    public static void clear() {
        HOLDER.remove();
    }
}
