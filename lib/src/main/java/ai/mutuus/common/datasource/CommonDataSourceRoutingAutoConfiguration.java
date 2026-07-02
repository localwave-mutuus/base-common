package ai.mutuus.common.datasource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 프로퍼티 기반 다중 DataSource + 읽기/쓰기 라우팅 자동 구성(<b>기본 OFF, opt-in</b>).
 * {@code spring-jdbc} 가 classpath 에 있고 {@code mutuus.common.datasource.enabled=true} 일 때만 동작한다.
 *
 * <p>소비 서비스는 <b>YAML 만으로</b> 여러 논리 DB(그룹)를 정의하고 각 그룹의 write/read 커넥션을 준다
 * ({@link CommonDataSourceProperties}). 실제 빈 등록은 {@link MultiDataSourceRegistrar} 가 하며, 그룹마다
 * 라우팅 DataSource({@code <group>DataSource})를, {@code primary} 그룹은 {@code @Primary} 로 등록한다.
 * Boot {@code DataSourceAutoConfiguration} 보다 먼저 처리되어({@code beforeName}) 이 DataSource 들이 우선한다.
 *
 * <p><b>메커니즘만</b> 제공한다 — 커넥션/풀/라우팅까지. JPA 엔티티↔DB 매핑·트랜잭션 경계·XA 등 정책은 서비스 몫.
 * 라우팅 판별은 {@code @Transactional(readOnly)} 플래그(+ {@link RoutingContext} 명시 override)로 이뤄진다(AOP 불필요).
 */
@AutoConfiguration(beforeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass(AbstractRoutingDataSource.class)
@ConditionalOnProperty(prefix = "mutuus.common.datasource", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CommonDataSourceProperties.class)
@Import(MultiDataSourceRegistrar.class)
public class CommonDataSourceRoutingAutoConfiguration {
}
