package ai.mutuus.common.datasource;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로퍼티 기반 다중 DataSource + 읽기/쓰기 라우팅 슬라이스 테스트(전체 앱 없이 autoconfig 만).
 * groups.g1(write/read)·g2(write only)를 YAML 프로퍼티로 정의 → 그룹별 {@code <group>DataSource} 빈이
 * 등록되고, g1 이 {@code @Primary}, readOnly→READ / 쓰기→WRITE 로 라우팅되는지 검증한다.
 */
class CommonDataSourceRoutingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonDataSourceRoutingAutoConfiguration.class));

    private static void seed(String url, String role) {
        JdbcTemplate jdbc = new JdbcTemplate(h2(url));
        jdbc.execute("create table if not exists routing_marker(role varchar(10))");
        jdbc.update("delete from routing_marker");
        jdbc.update("insert into routing_marker values(?)", role);
    }

    private static DriverManagerDataSource h2(String url) {
        DriverManagerDataSource ds = new DriverManagerDataSource(url, "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }

    private static String marker(JdbcTemplate jdbc) {
        return jdbc.queryForObject("select role from routing_marker", String.class);
    }

    @Test
    void 기본_OFF면_어떤_DataSource도_등록되지_않는다() {
        runner.run(ctx -> {
            assertThat(ctx.containsBean("g1DataSource")).isFalse();
            assertThat(ctx.containsBean("g2DataSource")).isFalse();
        });
    }

    @Test
    void 프로퍼티_그룹마다_라우팅_DataSource가_등록되고_primary가_적용된다() {
        // write/read 물리 저장소에 역할 마커를 심어 라우팅 결과를 관찰한다.
        seed("jdbc:h2:mem:t-g1-w;DB_CLOSE_DELAY=-1", "WRITE");
        seed("jdbc:h2:mem:t-g1-r;DB_CLOSE_DELAY=-1", "READ");
        seed("jdbc:h2:mem:t-g2-w;DB_CLOSE_DELAY=-1", "WRITE");

        runner.withPropertyValues(
                "mutuus.common.datasource.enabled=true",
                "mutuus.common.datasource.primary=g1",
                "mutuus.common.datasource.groups.g1.write.url=jdbc:h2:mem:t-g1-w;DB_CLOSE_DELAY=-1",
                "mutuus.common.datasource.groups.g1.write.username=sa",
                "mutuus.common.datasource.groups.g1.write.driver-class-name=org.h2.Driver",
                "mutuus.common.datasource.groups.g1.read.url=jdbc:h2:mem:t-g1-r;DB_CLOSE_DELAY=-1",
                "mutuus.common.datasource.groups.g1.read.username=sa",
                "mutuus.common.datasource.groups.g1.read.driver-class-name=org.h2.Driver",
                "mutuus.common.datasource.groups.g2.write.url=jdbc:h2:mem:t-g2-w;DB_CLOSE_DELAY=-1",
                "mutuus.common.datasource.groups.g2.write.username=sa",
                "mutuus.common.datasource.groups.g2.write.driver-class-name=org.h2.Driver"
        ).run(ctx -> {
            assertThat(ctx.containsBean("g1DataSource")).isTrue();
            assertThat(ctx.containsBean("g2DataSource")).isTrue();

            // g1 이 primary → 타입으로 단건 조회 시 g1
            DataSource primary = ctx.getBean(DataSource.class);
            DataSource g1 = ctx.getBean("g1DataSource", DataSource.class);
            assertThat(primary).isSameAs(g1);

            JdbcTemplate jdbc = new JdbcTemplate(g1);
            DataSourceTransactionManager tm = new DataSourceTransactionManager(g1);

            assertThat(marker(jdbc)).isEqualTo("WRITE"); // 트랜잭션 밖 → WRITE

            TransactionTemplate readOnly = new TransactionTemplate(tm);
            readOnly.setReadOnly(true);
            String readResult = readOnly.execute(status -> marker(jdbc));
            assertThat(readResult).isEqualTo("READ"); // readOnly → READ

            // g2 는 read 미지정 → write 폴백(항상 WRITE)
            JdbcTemplate g2 = new JdbcTemplate(ctx.getBean("g2DataSource", DataSource.class));
            DataSourceTransactionManager g2tm =
                    new DataSourceTransactionManager(ctx.getBean("g2DataSource", DataSource.class));
            TransactionTemplate g2ro = new TransactionTemplate(g2tm);
            g2ro.setReadOnly(true);
            String g2Read = g2ro.execute(status -> marker(g2));
            assertThat(g2Read).isEqualTo("WRITE"); // read 폴백
        });
    }
}
