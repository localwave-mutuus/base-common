package ai.mutuus.sample.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import ai.mutuus.common.datasource.DbRole;
import ai.mutuus.common.datasource.RoutingContext;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * "하나의 API 가 여러 DB(각 read/write 분리)에 접속" 을 <b>프로퍼티 기반 라이브러리 라우팅</b>으로 실증한다.
 * ai 프로파일이 {@code mutuus.common.datasource.groups.{db1,db2,db3}} 를 YAML 로 정의하면, 라이브러리가
 * 그룹별 라우팅 DataSource({@code db1DataSource} 등)를 등록한다 — 여기서는 그 빈을 <b>지연 조회</b>로 받아
 * 각 DB 에 쓰기(→write 저장소)/읽기(→read 저장소) 라우팅을 수행한다(그룹 미정의 프로파일에선 조회만 실패).
 */
@Component
public class DbMultiSupport {

    private final ApplicationContext ctx;
    private final Set<String> ready = ConcurrentHashMap.newKeySet();

    public DbMultiSupport(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    private DataSource ds(String db) {
        String bean = db + "DataSource";
        if (!ctx.containsBean(bean)) {
            throw new IllegalStateException("DataSource 빈 '" + bean + "' 없음 — mutuus.common.datasource.groups."
                    + db + " 를 정의해야 한다(ai 프로파일에 구성됨).");
        }
        DataSource ds = ctx.getBean(bean, DataSource.class);
        ensureTable(db, ds);
        return ds;
    }

    /** write 저장소는 비우고, read(Replica) 저장소는 씨앗을 심어 두 물리 저장소가 다름을 보인다. */
    private void ensureTable(String db, DataSource ds) {
        if (ready.contains(db)) {
            return;
        }
        RoutingContext.set(DbRole.WRITE);
        try {
            JdbcTemplate w = new JdbcTemplate(ds);
            w.execute("create table if not exists item(id int auto_increment primary key, name varchar(100))");
            w.update("delete from item");
        } finally {
            RoutingContext.clear();
        }
        RoutingContext.set(DbRole.READ);
        try {
            JdbcTemplate r = new JdbcTemplate(ds);
            r.execute("create table if not exists item(id int auto_increment primary key, name varchar(100))");
            r.update("delete from item");
            r.update("insert into item(name) values(?)", db + "-replica-seed");
        } finally {
            RoutingContext.clear();
        }
        ready.add(db);
    }

    /** 지정 DB 에 쓰기(쓰기 트랜잭션 → write 저장소로 라우팅). */
    public Map<String, Object> write(String db, String name) {
        DataSource ds = ds(db);
        new TransactionTemplate(new DataSourceTransactionManager(ds)).executeWithoutResult(s ->
                new JdbcTemplate(ds).update("insert into item(name) values(?)", name));
        return status(db, "DB[" + db + "] write 저장소에 insert(name=" + name + ") — 쓰기 트랜잭션 → write 라우팅.");
    }

    /** 지정 DB 에서 읽기(@Transactional(readOnly) → read 저장소로 라우팅). */
    public Map<String, Object> read(String db) {
        DataSource ds = ds(db);
        TransactionTemplate ro = new TransactionTemplate(new DataSourceTransactionManager(ds));
        ro.setReadOnly(true);
        List<String> names = ro.execute(s ->
                new JdbcTemplate(ds).queryForList("select name from item order by id", String.class));
        Map<String, Object> res = status(db, "DB[" + db + "] readOnly 조회 → read(Replica) 저장소로 라우팅.");
        res.put("readNames", names);
        return res;
    }

    /** 한 DB 의 write/read 저장소 카운트. */
    public Map<String, Object> status(String db, String note) {
        DataSource ds = ds(db);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("db", db);
        res.put("writeStoreCount", storeCount(ds, DbRole.WRITE));
        res.put("readStoreCount", storeCount(ds, DbRole.READ));
        if (note != null) {
            res.put("note", note);
        }
        return res;
    }

    /** 3개 DB 전체 상태(요구사항: 하나의 API 가 3개 DB 각 read/write). */
    public Map<String, Object> statusAll() {
        Map<String, Object> res = new LinkedHashMap<>();
        for (String db : List.of("db1", "db2", "db3")) {
            res.put(db, status(db, null));
        }
        res.put("note", "하나의 API 가 3개 DB(db1/db2/db3)에 접속 — 각 DB 는 write/read 저장소로 분리 라우팅. "
                + "모두 YAML(mutuus.common.datasource.groups.*)만으로 구성(소비자 Java 배선 0).");
        return res;
    }

    private int storeCount(DataSource ds, DbRole role) {
        RoutingContext.set(role);
        try {
            Integer n = new JdbcTemplate(ds).queryForObject("select count(*) from item", Integer.class);
            return n == null ? 0 : n;
        } finally {
            RoutingContext.clear();
        }
    }
}
