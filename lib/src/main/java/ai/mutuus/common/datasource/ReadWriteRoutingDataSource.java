package ai.mutuus.common.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 읽기/쓰기 분리 라우팅 DataSource. 커넥션을 실제로 얻는 시점의 역할로 write/read 대상 중 하나를 고른다.
 * <p>역할 결정 우선순위:
 * <ol>
 *   <li>{@link RoutingContext} 명시 지정이 있으면 그 값(트랜잭션 밖 강제 라우팅 등)</li>
 *   <li>없으면 현재 트랜잭션의 {@code readOnly} 플래그 — {@code @Transactional(readOnly=true)} → READ, 그 외 WRITE</li>
 * </ol>
 * <p><b>{@code LazyConnectionDataSourceProxy} 로 감싸는 것이 필수</b>다. 그래야 트랜잭션이 열려
 * {@code readOnly} 플래그가 설정된 <b>이후</b>에 커넥션이 지연 획득되어 이 판별이 정확히 동작한다
 * (프록시 없이 감싸지 않으면 트랜잭션 시작 시점에 커넥션이 먼저 붙어 readOnly 라우팅이 무력화된다 —
 * {@code CommonDataSourceRoutingAutoConfiguration} 가 이 래핑을 대신 해 준다).
 */
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        DbRole explicit = RoutingContext.peek();
        if (explicit != null) {
            return explicit;
        }
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? DbRole.READ : DbRole.WRITE;
    }
}
