package ai.mutuus.common.datasource;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.util.StringUtils;

/**
 * {@code mutuus.common.datasource.groups.*} 프로퍼티를 읽어, 그룹마다 읽기/쓰기 라우팅 DataSource 를
 * 빈({@code <group>DataSource})으로 등록한다. {@code primary} 그룹은 {@code @Primary} 로 표시해
 * JPA/기본 주입이 그 DataSource 를 쓰게 한다(Boot 단일 DataSource 자동구성은 물러남).
 *
 * <p>autoconfig({@link CommonDataSourceRoutingAutoConfiguration})가 {@code @Import} 하고, autoconfig 는
 * Boot {@code DataSourceAutoConfiguration} 보다 먼저 처리되므로 여기서 등록한 DataSource 빈이 우선한다.
 */
public class MultiDataSourceRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        CommonDataSourceProperties props = Binder.get(environment)
                .bind("mutuus.common.datasource", CommonDataSourceProperties.class)
                .orElseGet(CommonDataSourceProperties::new);
        if (!props.isEnabled() || props.getGroups().isEmpty()) {
            return;
        }
        String primary = props.getPrimary();
        if (!StringUtils.hasText(primary) && props.getGroups().size() == 1) {
            primary = props.getGroups().keySet().iterator().next();
        }
        validate(props, primary);
        for (Map.Entry<String, CommonDataSourceProperties.Group> entry : props.getGroups().entrySet()) {
            String name = entry.getKey();
            CommonDataSourceProperties.Group group = entry.getValue();
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(DataSource.class, () -> buildRoutingDataSource(name, group));
            AbstractBeanDefinition bd = builder.getBeanDefinition();
            bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            if (name.equals(primary)) {
                bd.setPrimary(true);
            }
            registry.registerBeanDefinition(name + "DataSource", bd);
        }
    }

    private static void validate(CommonDataSourceProperties props, String primary) {
        if (StringUtils.hasText(primary) && !props.getGroups().containsKey(primary)) {
            throw new IllegalStateException("mutuus.common.datasource.primary '" + primary
                    + "' does not match any configured datasource group");
        }
        for (Map.Entry<String, CommonDataSourceProperties.Group> entry : props.getGroups().entrySet()) {
            String name = entry.getKey();
            CommonDataSourceProperties.Group group = entry.getValue();
            if (!StringUtils.hasText(name)) {
                throw new IllegalStateException("mutuus.common.datasource group name must not be blank");
            }
            if (group == null || group.getWrite() == null) {
                throw new IllegalStateException("datasource group '" + name + "' requires a write connection");
            }
            validateConnection(name, "write", group.getWrite());
            if (group.getRead() != null) {
                validateConnection(name, "read", group.getRead());
            }
        }
    }

    private static void validateConnection(String group, String role, CommonDataSourceProperties.Conn conn) {
        if (!StringUtils.hasText(conn.getUrl())) {
            throw new IllegalStateException("datasource group '" + group + "' " + role + " connection requires url");
        }
        if (conn.getPoolMaxSize() != null && conn.getPoolMaxSize() < 1) {
            throw new IllegalStateException("datasource group '" + group + "' " + role
                    + " pool-max-size must be greater than zero");
        }
    }

    /** 그룹의 write/read 커넥션으로 라우팅 DataSource(LazyConnection 래핑)를 조립한다. */
    static DataSource buildRoutingDataSource(String group, CommonDataSourceProperties.Group g) {
        if (g == null || g.getWrite() == null) {
            throw new IllegalStateException("datasource group '" + group + "' 에 write 커넥션이 필요합니다.");
        }
        DataSource write = hikari(group + "-write", g.getWrite());
        DataSource read = (g.getRead() != null) ? hikari(group + "-read", g.getRead()) : write; // read 없으면 write 폴백
        ReadWriteRoutingDataSource routing = new ReadWriteRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(DbRole.WRITE, write);
        targets.put(DbRole.READ, read);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(write);
        routing.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(routing); // 필수 — readOnly 라우팅을 위한 커넥션 지연
    }

    private static HikariDataSource hikari(String poolName, CommonDataSourceProperties.Conn c) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName(poolName);
        ds.setJdbcUrl(c.getUrl());
        if (StringUtils.hasText(c.getUsername())) {
            ds.setUsername(c.getUsername());
        }
        if (c.getPassword() != null) {
            ds.setPassword(c.getPassword());
        }
        if (StringUtils.hasText(c.getDriverClassName())) {
            ds.setDriverClassName(c.getDriverClassName());
        }
        if (c.getPoolMaxSize() != null) {
            ds.setMaximumPoolSize(c.getPoolMaxSize());
        }
        return ds;
    }
}
