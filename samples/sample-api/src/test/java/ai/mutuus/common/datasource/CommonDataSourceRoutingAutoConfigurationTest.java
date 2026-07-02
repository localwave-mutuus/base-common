package ai.mutuus.common.datasource;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 읽기/쓰기 라우팅 메커니즘 슬라이스 테스트(전체 앱 컨텍스트 없이 autoconfig 만). write/read 두 H2 에
 * 자기 역할 마커를 심고, 라우팅 결과가 마커로 드러나는지 검증한다.
 * <ul>
 *   <li>트랜잭션 밖 → WRITE(기본)</li>
 *   <li>{@code @Transactional(readOnly=true)} → READ(트랜잭션 readOnly 플래그 기반, LazyConnection 필수)</li>
 *   <li>쓰기 트랜잭션 → WRITE</li>
 *   <li>{@link RoutingContext} 명시 지정 → 그 값 우선</li>
 *   <li>opt-in OFF(기본) → 라우팅 @Primary 미등록</li>
 * </ul>
 */
class CommonDataSourceRoutingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonDataSourceRoutingAutoConfiguration.class))
            .withUserConfiguration(TestDataSources.class);

    private static String marker(JdbcTemplate jdbc) {
        return jdbc.queryForObject("select role from routing_marker", String.class);
    }

    @Test
    void 기본_OFF면_라우팅_Primary가_등록되지_않는다() {
        runner.run(ctx -> assertThat(ctx.containsBean("dataSource")).isFalse());
    }

    @Test
    void readOnly_트랜잭션은_READ_그외는_WRITE로_라우팅된다() {
        runner.withPropertyValues("mutuus.common.datasource.routing.enabled=true").run(ctx -> {
            DataSource primary = ctx.getBean("dataSource", DataSource.class); // Lazy 라우팅 프록시
            JdbcTemplate jdbc = new JdbcTemplate(primary);
            DataSourceTransactionManager tm = new DataSourceTransactionManager(primary);

            // 1) 트랜잭션 밖 → WRITE(기본)
            assertThat(marker(jdbc)).isEqualTo("WRITE");

            // 2) readOnly 트랜잭션 → READ
            TransactionTemplate readOnly = new TransactionTemplate(tm);
            readOnly.setReadOnly(true);
            String readResult = readOnly.execute(status -> marker(jdbc));
            assertThat(readResult).isEqualTo("READ");

            // 3) 쓰기 트랜잭션 → WRITE
            TransactionTemplate write = new TransactionTemplate(tm);
            String writeResult = write.execute(status -> marker(jdbc));
            assertThat(writeResult).isEqualTo("WRITE");

            // 4) 명시 지정(RoutingContext) 우선
            RoutingContext.set(DbRole.READ);
            try {
                assertThat(marker(jdbc)).isEqualTo("READ");
            } finally {
                RoutingContext.clear();
            }
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDataSources {

        @Bean("writeDataSource")
        DataSource writeDataSource() {
            return markedH2("jdbc:h2:mem:routing-write;DB_CLOSE_DELAY=-1", "WRITE");
        }

        @Bean("readDataSource")
        DataSource readDataSource() {
            return markedH2("jdbc:h2:mem:routing-read;DB_CLOSE_DELAY=-1", "READ");
        }

        /** 역할 마커 테이블을 심은 H2 인메모리 DataSource. */
        private static DataSource markedH2(String url, String role) {
            DriverManagerDataSource ds = new DriverManagerDataSource(url, "sa", "");
            ds.setDriverClassName("org.h2.Driver");
            JdbcTemplate jdbc = new JdbcTemplate(ds);
            jdbc.execute("create table if not exists routing_marker(role varchar(10))");
            jdbc.update("delete from routing_marker");
            jdbc.update("insert into routing_marker values(?)", role);
            return ds;
        }
    }
}
