package ai.mutuus.sample.demo;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import ai.mutuus.common.datasource.DbRole;
import ai.mutuus.common.datasource.ReadWriteRoutingDataSource;
import ai.mutuus.common.datasource.RoutingContext;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DB 계층 가이드(260702.002.DB_LAYER_GUIDE.md) 3~4장의 세 구조를 <b>자기완결적으로 시연</b>하는 지원 컴포넌트.
 * 앱의 기본(@Primary) DataSource·JPA 감사에는 전혀 손대지 않도록 <b>데모 전용 H2 자원을 내부에서 따로</b> 만든다.
 * <ul>
 *   <li>① 읽기/쓰기 라우팅 — <b>라이브러리</b> {@link ReadWriteRoutingDataSource} + {@code LazyConnectionDataSourceProxy}</li>
 *   <li>② 데이터소스별 개별 TxManager — 독립 커밋/롤백(원자성 없음)</li>
 *   <li>③ JTA/XA — Atomikos 로 여러 DB 원자적 커밋/롤백(2PC)</li>
 * </ul>
 * 자원은 지연 초기화·캐시하고 호출마다 데이터를 리셋해 결정적으로 관찰한다. ③ 초기화 실패는 잡아 안내만 반환(응답 200 유지).
 */
@Component
public class DbDemoSupport {

    // ---------- ① 읽기/쓰기 라우팅 (라이브러리 메커니즘) ----------
    private DataSource routingPrimary;
    private DataSourceTransactionManager routingTm;

    private synchronized void initRouting() {
        if (routingPrimary != null) {
            return;
        }
        DataSource write = markerH2("jdbc:h2:mem:demo-route-write;DB_CLOSE_DELAY=-1", "WRITE");
        DataSource read = markerH2("jdbc:h2:mem:demo-route-read;DB_CLOSE_DELAY=-1", "READ");
        ReadWriteRoutingDataSource routing = new ReadWriteRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(DbRole.WRITE, write);
        targets.put(DbRole.READ, read);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(write);
        routing.afterPropertiesSet();
        this.routingPrimary = new LazyConnectionDataSourceProxy(routing); // 필수 래핑
        this.routingTm = new DataSourceTransactionManager(routingPrimary);
    }

    public Map<String, Object> routingDemo() {
        initRouting();
        JdbcTemplate jdbc = new JdbcTemplate(routingPrimary);

        String noTx = marker(jdbc); // 트랜잭션 밖 → WRITE

        TransactionTemplate readOnly = new TransactionTemplate(routingTm);
        readOnly.setReadOnly(true);
        String readOnlyTx = readOnly.execute(s -> marker(jdbc)); // → READ

        TransactionTemplate writeTx = new TransactionTemplate(routingTm);
        String writeTxRole = writeTx.execute(s -> marker(jdbc)); // → WRITE

        RoutingContext.set(DbRole.READ); // 명시 override
        String explicit;
        try {
            explicit = marker(jdbc);
        } finally {
            RoutingContext.clear();
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("noTx", noTx);
        res.put("readOnlyTx", readOnlyTx);
        res.put("writeTx", writeTxRole);
        res.put("explicitRead", explicit);
        res.put("routingWorks", "WRITE".equals(noTx) && "READ".equals(readOnlyTx)
                && "WRITE".equals(writeTxRole) && "READ".equals(explicit));
        res.put("note", "@Transactional(readOnly) → READ, 그 외 → WRITE, RoutingContext 로 명시 override. "
                + "라이브러리 CommonDataSourceRoutingAutoConfiguration(opt-in) 이 이 배관을 @Primary 로 조립해 준다.");
        return res;
    }

    private static String marker(JdbcTemplate jdbc) {
        return jdbc.queryForObject("select role from routing_marker", String.class);
    }

    private static DataSource markerH2(String url, String role) {
        DriverManagerDataSource ds = h2(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table if not exists routing_marker(role varchar(10))");
        jdbc.update("delete from routing_marker");
        jdbc.update("insert into routing_marker values(?)", role);
        return ds;
    }

    // ---------- ② 데이터소스별 개별 TxManager (독립 커밋/롤백) ----------
    public Map<String, Object> multiTxDemo() {
        DataSource userDs = tableH2("jdbc:h2:mem:demo-user;DB_CLOSE_DELAY=-1");
        DataSource orderDs = tableH2("jdbc:h2:mem:demo-order;DB_CLOSE_DELAY=-1");
        new JdbcTemplate(userDs).update("delete from t");
        new JdbcTemplate(orderDs).update("delete from t");

        DataSourceTransactionManager userTx = new DataSourceTransactionManager(userDs);
        DataSourceTransactionManager orderTx = new DataSourceTransactionManager(orderDs);

        // userTx: 정상 커밋
        new TransactionTemplate(userTx).executeWithoutResult(s ->
                new JdbcTemplate(userDs).update("insert into t values('U')"));

        // orderTx: 예외로 롤백 — userTx 와 독립이므로 user 커밋에 영향 없음
        try {
            new TransactionTemplate(orderTx).executeWithoutResult(s -> {
                new JdbcTemplate(orderDs).update("insert into t values('O')");
                throw new IllegalStateException("의도적 롤백");
            });
        } catch (RuntimeException ignored) {
            // 기대된 롤백
        }

        int userCount = count(userDs);
        int orderCount = count(orderDs);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("userCommitted", userCount == 1);
        res.put("orderRolledBack", orderCount == 0);
        res.put("independent", userCount == 1 && orderCount == 0);
        res.put("note", "DB별 TxManager 는 서로 독립 — order 롤백이 user 커밋에 영향 없음. "
                + "두 DB 원자성은 없음(원자성 필요 시 ③). 서비스 레벨 구성(라이브러리 미수용).");
        return res;
    }

    // ---------- ③ JTA/XA (Atomikos, 여러 DB 원자적 커밋/롤백) ----------
    private volatile boolean xaReady;
    private UserTransactionManager utm;
    private AtomikosDataSourceBean xa1;
    private AtomikosDataSourceBean xa2;

    private synchronized void initXa() throws Exception {
        if (xaReady) {
            return;
        }
        String dir = System.getProperty("java.io.tmpdir") + File.separator + "atomikos-demo";
        new File(dir).mkdirs();
        System.setProperty("com.atomikos.icatch.enable_logging", "false"); // 데모: 복구 로그 비활성
        System.setProperty("com.atomikos.icatch.log_base_dir", dir);
        System.setProperty("com.atomikos.icatch.output_dir", dir);

        this.xa1 = atomikosDs("demoXa1", "jdbc:h2:mem:demo-xa1;DB_CLOSE_DELAY=-1");
        this.xa2 = atomikosDs("demoXa2", "jdbc:h2:mem:demo-xa2;DB_CLOSE_DELAY=-1");
        this.utm = new UserTransactionManager();
        utm.setForceShutdown(false);
        utm.init();

        try (Connection c = xa1.getConnection()) {
            c.createStatement().execute("create table if not exists t(v varchar(10))");
        }
        try (Connection c = xa2.getConnection()) {
            c.createStatement().execute("create table if not exists t(v varchar(10))");
        }
        this.xaReady = true;
    }

    private static AtomikosDataSourceBean atomikosDs(String name, String url) throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL(url);
        h2.setUser("sa");
        h2.setPassword("");
        AtomikosDataSourceBean bean = new AtomikosDataSourceBean();
        bean.setUniqueResourceName(name);
        bean.setXaDataSource(h2);
        bean.setMinPoolSize(1);
        bean.setMaxPoolSize(3);
        bean.init();
        return bean;
    }

    public Map<String, Object> xaDemo() {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            initXa();
            new JdbcTemplate(xa1).update("delete from t"); // 로컬 모드 리셋
            new JdbcTemplate(xa2).update("delete from t");

            // (a) 롤백 경로: 두 DB 모두 넣고 rollback → 둘 다 비어야 원자적
            utm.begin();
            new JdbcTemplate(xa1).update("insert into t values('A')");
            new JdbcTemplate(xa2).update("insert into t values('B')");
            utm.rollback();
            boolean atomicRollback = count(xa1) == 0 && count(xa2) == 0;

            // (b) 커밋 경로: 두 DB 모두 넣고 commit → 둘 다 있어야 원자적
            utm.begin();
            new JdbcTemplate(xa1).update("insert into t values('A')");
            new JdbcTemplate(xa2).update("insert into t values('B')");
            utm.commit();
            boolean atomicCommit = count(xa1) == 1 && count(xa2) == 1;

            res.put("xaAvailable", true);
            res.put("atomicRollback", atomicRollback);
            res.put("atomicCommit", atomicCommit);
            res.put("atomic", atomicRollback && atomicCommit);
            res.put("note", "JTA/XA(2PC): 하나의 전역 트랜잭션으로 두 DB 를 원자적으로 커밋/롤백. "
                    + "2PC 오버헤드가 크므로 원자성 필수인 소수에만. 서비스 레벨 구성(라이브러리 미수용).");
        } catch (Throwable t) {
            res.put("xaAvailable", false);
            res.put("atomic", false);
            res.put("error", t.getClass().getSimpleName() + ": " + t.getMessage());
            res.put("note", "XA 초기화/실행 실패 — 데모 환경에서 Atomikos/H2 XA 구성 확인 필요.");
        }
        return res;
    }

    // ---------- 공용 헬퍼 ----------
    private static DriverManagerDataSource h2(String url) {
        DriverManagerDataSource ds = new DriverManagerDataSource(url, "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }

    private static DataSource tableH2(String url) {
        DriverManagerDataSource ds = h2(url);
        new JdbcTemplate(ds).execute("create table if not exists t(v varchar(10))");
        return ds;
    }

    private static int count(DataSource ds) {
        Integer n = new JdbcTemplate(ds).queryForObject("select count(*) from t", Integer.class);
        return n == null ? 0 : n;
    }
}
