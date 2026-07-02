package ai.mutuus.common.datasource;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 읽기/쓰기 라우팅 자동 구성(<b>기본 OFF, opt-in</b>). {@code spring-jdbc} 가 classpath 에 있고
 * {@code mutuus.common.datasource.routing.enabled=true} 일 때만 동작한다.
 *
 * <p>라이브러리는 <b>라우팅 "메커니즘"만</b> 제공한다 — 소비 서비스가 정의한
 * {@code @Qualifier("writeDataSource")}/{@code @Qualifier("readDataSource")} 두 DataSource 를 받아
 * {@link ReadWriteRoutingDataSource} 로 묶고, <b>필수인 {@link LazyConnectionDataSourceProxy} 래핑</b>까지
 * 대신 해 {@code @Primary} DataSource 로 노출한다. 라우팅 판별은 {@code @Transactional(readOnly)} 플래그
 * (+ {@link RoutingContext} 명시 지정)로 이뤄진다(AOP 불필요).
 *
 * <p><b>정책은 서비스 몫</b>이다 — 어느 DB(write/read 접속 좌표)·Replica 토폴로지·커넥션 풀 사이징·
 * Flyway/jOOQ 구성은 소비 서비스가 write/read DataSource 를 배선하며 정한다. 소비자가 자체 {@code @Primary}
 * DataSource 를 정의하면 {@code @ConditionalOnMissingBean} 으로 비켜선다.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass(AbstractRoutingDataSource.class)
@ConditionalOnProperty(prefix = "mutuus.common.datasource.routing", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DataSourceRoutingProperties.class)
public class CommonDataSourceRoutingAutoConfiguration {

    /**
     * write/read DataSource 를 라우팅 + Lazy 프록시로 조립해 {@code @Primary} DataSource 로 등록한다.
     * 소비 서비스는 {@code writeDataSource}/{@code readDataSource} 두 빈만 제공하면 된다.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "dataSource")
    public DataSource dataSource(@Qualifier("writeDataSource") DataSource writeDataSource,
                                 @Qualifier("readDataSource") DataSource readDataSource) {
        ReadWriteRoutingDataSource routing = new ReadWriteRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(DbRole.WRITE, writeDataSource);
        targets.put(DbRole.READ, readDataSource);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(writeDataSource); // 기본 WRITE
        routing.afterPropertiesSet();
        // Lazy 래핑 필수 — 트랜잭션 readOnly 플래그 설정 이후로 커넥션 획득을 지연시켜 라우팅을 정확히 한다.
        return new LazyConnectionDataSourceProxy(routing);
    }
}
