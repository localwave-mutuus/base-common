package ai.mutuus.common.datasource;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프로퍼티 기반 다중 DataSource + 읽기/쓰기 라우팅 설정. {@code mutuus.common.datasource.*}. <b>기본 OFF(opt-in)</b>.
 *
 * <p>여러 DB를 <b>YAML만으로</b> 정의한다 — 각 그룹은 하나의 논리 DB이며 {@code write}/{@code read} 커넥션을
 * 가진다. 라이브러리가 그룹별로 라우팅 DataSource(빈 이름 {@code <group>DataSource})를 조립하고,
 * {@code @Transactional(readOnly)} → read, 그 외 → write 로 라우팅한다({@link ReadWriteRoutingDataSource}).
 * {@code read} 미지정 시 write 로 폴백(단일 커넥션). {@code primary} 그룹의 DataSource 가 {@code @Primary}.
 *
 * <p>커넥션/풀 "정의"만 담는다 — JPA 엔티티↔DB 매핑·트랜잭션 경계·XA 등 정책은 소비 서비스 몫이다.
 *
 * <pre>
 * mutuus.common.datasource:
 *   enabled: true
 *   primary: orders
 *   groups:
 *     orders: { write: {url: ..., username: ..., password: ...}, read: {url: ...} }
 *     users:  { write: {url: ...}, read: {url: ...} }
 *     audit:  { write: {url: ...} }        # read 생략 → write 로 폴백
 * </pre>
 */
@ConfigurationProperties(prefix = "mutuus.common.datasource")
public class CommonDataSourceProperties {

    /** 다중 DataSource 라우팅 활성화 여부(기본 false). */
    private boolean enabled = false;

    /** {@code @Primary} 로 등록할 그룹명. 미지정 + 그룹 1개면 그 그룹이 primary. */
    private String primary;

    /** 논리 DB 그룹(그룹명 → write/read 커넥션). */
    private Map<String, Group> groups = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public Map<String, Group> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Group> groups) {
        this.groups = groups;
    }

    /** 논리 DB 하나 — write/read 커넥션. */
    public static class Group {
        private Conn write;
        private Conn read;

        public Conn getWrite() {
            return write;
        }

        public void setWrite(Conn write) {
            this.write = write;
        }

        public Conn getRead() {
            return read;
        }

        public void setRead(Conn read) {
            this.read = read;
        }
    }

    /** 커넥션/풀 정의. */
    public static class Conn {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        /** HikariCP 최대 풀 크기(미지정 시 Hikari 기본). */
        private Integer poolMaxSize;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public Integer getPoolMaxSize() {
            return poolMaxSize;
        }

        public void setPoolMaxSize(Integer poolMaxSize) {
            this.poolMaxSize = poolMaxSize;
        }
    }
}
