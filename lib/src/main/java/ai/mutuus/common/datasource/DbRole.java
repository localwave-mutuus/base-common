package ai.mutuus.common.datasource;

/**
 * 읽기/쓰기 라우팅 대상 역할. {@link ReadWriteRoutingDataSource} 의 lookup 키로 쓰인다.
 */
public enum DbRole {
    /** 쓰기(Primary). 기본값. */
    WRITE,
    /** 읽기(Replica). {@code @Transactional(readOnly=true)} 또는 명시 지정 시. */
    READ
}
