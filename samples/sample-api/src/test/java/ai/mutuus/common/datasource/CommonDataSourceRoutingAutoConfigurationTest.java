package ai.mutuus.common.datasource;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice tests for property based multi DataSource routing.
 *
 * <p>The tests intentionally use PostgreSQL JDBC URLs and avoid opening database
 * connections. That keeps the common module validation aligned with production
 * PostgreSQL without requiring Docker, H2, a fixed local port, or shared live
 * infrastructure.
 */
class CommonDataSourceRoutingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonDataSourceRoutingAutoConfiguration.class));

    @Test
    void disabled_by_default_registers_no_datasource() {
        runner.run(ctx -> {
            assertThat(ctx.containsBean("g1DataSource")).isFalse();
            assertThat(ctx.containsBean("g2DataSource")).isFalse();
        });
    }

    @Test
    void postgresql_properties_register_routing_datasources_without_opening_connections() {
        runner.withPropertyValues(
                "mutuus.common.datasource.enabled=true",
                "mutuus.common.datasource.primary=g1",
                "mutuus.common.datasource.groups.g1.write.url=jdbc:postgresql://write.example.test:5432/app",
                "mutuus.common.datasource.groups.g1.write.username=app",
                "mutuus.common.datasource.groups.g1.write.driver-class-name=org.postgresql.Driver",
                "mutuus.common.datasource.groups.g1.read.url=jdbc:postgresql://read.example.test:5432/app",
                "mutuus.common.datasource.groups.g1.read.username=app",
                "mutuus.common.datasource.groups.g1.read.driver-class-name=org.postgresql.Driver",
                "mutuus.common.datasource.groups.g2.write.url=jdbc:postgresql://write.example.test:5432/audit",
                "mutuus.common.datasource.groups.g2.write.username=app",
                "mutuus.common.datasource.groups.g2.write.driver-class-name=org.postgresql.Driver"
        ).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.containsBean("g1DataSource")).isTrue();
            assertThat(ctx.containsBean("g2DataSource")).isTrue();

            DataSource primary = ctx.getBean(DataSource.class);
            DataSource g1 = ctx.getBean("g1DataSource", DataSource.class);
            assertThat(primary).isSameAs(g1);
        });
    }

    @Test
    void routing_key_uses_read_only_transaction_and_explicit_context_without_database_connection() {
        TestRoutingDataSource routing = new TestRoutingDataSource();

        assertThat(routing.lookupKey()).isEqualTo(DbRole.WRITE);

        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(routing.lookupKey()).isEqualTo(DbRole.READ);
            RoutingContext.set(DbRole.WRITE);
            assertThat(routing.lookupKey()).isEqualTo(DbRole.WRITE);
        } finally {
            RoutingContext.clear();
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        }
    }

    @Test
    void invalid_primary_group_fails_fast() {
        runner.withPropertyValues(
                "mutuus.common.datasource.enabled=true",
                "mutuus.common.datasource.primary=missing",
                "mutuus.common.datasource.groups.g1.write.url=jdbc:postgresql://write.example.test:5432/app",
                "mutuus.common.datasource.groups.g1.write.driver-class-name=org.postgresql.Driver"
        ).run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void missing_write_url_fails_fast() {
        runner.withPropertyValues(
                "mutuus.common.datasource.enabled=true",
                "mutuus.common.datasource.groups.g1.write.username=app",
                "mutuus.common.datasource.groups.g1.write.driver-class-name=org.postgresql.Driver"
        ).run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void hikari_missing_disables_datasource_routing_autoconfiguration() {
        runner.withClassLoader(new FilteredClassLoader(HikariDataSource.class))
                .withPropertyValues(
                        "mutuus.common.datasource.enabled=true",
                        "mutuus.common.datasource.groups.g1.write.url=jdbc:postgresql://write.example.test:5432/app",
                        "mutuus.common.datasource.groups.g1.write.username=app",
                        "mutuus.common.datasource.groups.g1.write.driver-class-name=org.postgresql.Driver"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.containsBean("g1DataSource")).isFalse();
                });
    }

    static class TestRoutingDataSource extends ReadWriteRoutingDataSource {
        Object lookupKey() {
            return determineCurrentLookupKey();
        }
    }
}
