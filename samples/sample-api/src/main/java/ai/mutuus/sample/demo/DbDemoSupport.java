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
 * DB 계층 가이드(260702.002.DB_LAYER_GUIDE.md) 3~4장 구조를 <b>실제 DB 연산으로 시연</b>하는 지원 컴포넌트.
 * 앱 기본(@Primary) DataSource·JPA 감사와 분리된 <b>데모 전용 H2 자원</b>을 내부에서 따로 만든다.
 * <ul>
 *   <li>① 읽기/쓰기 라우팅 — <b>라이브러리</b> {@link ReadWriteRoutingDataSource} + {@code LazyConnectionDataSourceProxy}</li>
 *   <li>② 데이터소스별 개별 TxManager — 독립 커밋/롤백</li>
 *   <li>③ JTA/XA — Atomikos 로 여러 DB 원자적 커밋/롤백(2PC)</li>
 * </ul>
 * 개요 데모(routingDemo/multiTxDemo/xaDemo)와 DB 랩 페이지용 상세 연산(rw·xa 계열)을 함께 제공한다.
 * 자원은 지연 초기화·캐시한다. XA 초기화 실패는 잡아 안내만 반환한다.
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
        DataSource routeWriteDs = markerH2("jdbc:h2:mem:demo-route-write;DB_CLOSE_DELAY=-1", "WRITE");
        DataSource routeReadDs = markerH2("jdbc:h2:mem:demo-route-read;DB_CLOSE_DELAY=-1", "READ");

        ReadWriteRoutingDataSource routing = new ReadWriteRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(DbRole.WRITE, routeWriteDs);
        targets.put(DbRole.READ, routeReadDs);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(routeWriteDs);
        routing.afterPropertiesSet();
        this.routingPrimary = new LazyConnectionDataSourceProxy(routing); // 필수 래핑
        this.routingTm = new DataSourceTransactionManager(routingPrimary);
    }

    /** 개요: 라우팅 역할이 마커로 드러나는지(트랜잭션 밖/readOnly/쓰기/명시 override). */
    public Map<String, Object> routingDemo() {
        initRouting();
        JdbcTemplate jdbc = new JdbcTemplate(routingPrimary);
        String noTx = marker(jdbc);
        TransactionTemplate readOnly = new TransactionTemplate(routingTm);
        readOnly.setReadOnly(true);
        String readOnlyTx = readOnly.execute(s -> marker(jdbc));
        TransactionTemplate writeTx = new TransactionTemplate(routingTm);
        String writeTxRole = writeTx.execute(s -> marker(jdbc));
        RoutingContext.set(DbRole.READ);
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

        new TransactionTemplate(userTx).executeWithoutResult(s ->
                new JdbcTemplate(userDs).update("insert into t values('U')"));
        try {
            new TransactionTemplate(orderTx).executeWithoutResult(s -> {
                new JdbcTemplate(orderDs).update("insert into t values('O')");
                throw new IllegalStateException("의도적 롤백");
            });
        } catch (RuntimeException ignored) {
            // 기대된 롤백
        }

        int userCount = count(userDs, "t");
        int orderCount = count(orderDs, "t");
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
        System.setProperty("com.atomikos.icatch.enable_logging", "false");
        System.setProperty("com.atomikos.icatch.log_base_dir", dir);
        System.setProperty("com.atomikos.icatch.output_dir", dir);

        this.xa1 = atomikosDs("demoXa1", "jdbc:h2:mem:demo-xa1;DB_CLOSE_DELAY=-1");
        this.xa2 = atomikosDs("demoXa2", "jdbc:h2:mem:demo-xa2;DB_CLOSE_DELAY=-1");
        this.utm = new UserTransactionManager();
        utm.setForceShutdown(false);
        utm.init();

        try (Connection c = xa1.getConnection()) {
            c.createStatement().execute("create table if not exists t(v varchar(50))");
        }
        try (Connection c = xa2.getConnection()) {
            c.createStatement().execute("create table if not exists t(v varchar(50))");
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

    /** 개요: 롤백/커밋 경로 원자성 자가 확인. */
    public Map<String, Object> xaDemo() {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            initXa();
            new JdbcTemplate(xa1).update("delete from t");
            new JdbcTemplate(xa2).update("delete from t");

            utm.begin();
            new JdbcTemplate(xa1).update("insert into t values('A')");
            new JdbcTemplate(xa2).update("insert into t values('B')");
            utm.rollback();
            boolean atomicRollback = count(xa1, "t") == 0 && count(xa2, "t") == 0;

            utm.begin();
            new JdbcTemplate(xa1).update("insert into t values('A')");
            new JdbcTemplate(xa2).update("insert into t values('B')");
            utm.commit();
            boolean atomicCommit = count(xa1, "t") == 1 && count(xa2, "t") == 1;

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

    // --- XA 랩: 성공(커밋)/실패(롤백)를 개별 호출로 실증 ---

    /** XA 성공: 두 DB 에 insert 후 전역 커밋 → 둘 다 반영. */
    public Map<String, Object> xaCommit(String value) throws Exception {
        initXa();
        utm.begin();
        try {
            new JdbcTemplate(xa1).update("insert into t values(?)", value);
            new JdbcTemplate(xa2).update("insert into t values(?)", value);
            utm.commit();
        } catch (RuntimeException e) {
            safeRollback();
            throw e;
        }
        Map<String, Object> res = xaCounts();
        res.put("committed", true);
        res.put("note", "전역 커밋 성공 — 두 DB 모두 value 반영(원자적).");
        return res;
    }

    /** XA 실패: 두 DB 에 insert 후 전역 롤백 → 둘 다 미반영(원자적 취소). */
    public Map<String, Object> xaRollback(String value) throws Exception {
        initXa();
        int before1 = count(xa1, "t");
        int before2 = count(xa2, "t");
        utm.begin();
        new JdbcTemplate(xa1).update("insert into t values(?)", value);
        new JdbcTemplate(xa2).update("insert into t values(?)", value);
        utm.rollback();
        Map<String, Object> res = xaCounts();
        res.put("rolledBack", count(xa1, "t") == before1 && count(xa2, "t") == before2);
        res.put("note", "전역 롤백 — 두 DB 모두 value 미반영(원자적 취소). 카운트 불변.");
        return res;
    }

    /** XA 두 DB 현재 행 수 + 초기화. */
    public Map<String, Object> xaCounts() throws Exception {
        initXa();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("xaAvailable", true);
        res.put("db1Count", count(xa1, "t"));
        res.put("db2Count", count(xa2, "t"));
        return res;
    }

    /** XA 두 DB 초기화(행 삭제). */
    public Map<String, Object> xaReset() throws Exception {
        initXa();
        new JdbcTemplate(xa1).update("delete from t");
        new JdbcTemplate(xa2).update("delete from t");
        return xaCounts();
    }

    private void safeRollback() {
        try {
            utm.rollback();
        } catch (Exception ignored) {
            // best-effort
        }
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

    private static int count(DataSource ds, String table) {
        Integer n = new JdbcTemplate(ds).queryForObject("select count(*) from " + table, Integer.class);
        return n == null ? 0 : n;
    }
}
