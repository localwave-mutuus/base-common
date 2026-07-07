package ai.mutuus.sample.support;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/**
 * 로컬 <b>실 인프라</b>(PostgreSQL/Redis) 도달성 게이트 + 접속 좌표.
 *
 * <p>Testcontainers 실연동 테스트는 Docker 가 없으면 skip 되므로(이 머신엔 Docker 없음),
 * 이미 떠 있는 로컬 실 인프라를 대상으로 도는 "live" 변형 테스트가 별도로 필요하다.
 * 이 클래스의 게이트는 <b>포트 개방만이 아니라 자격증명 인증까지</b> 성공해야 {@code true} 다
 * — 그래야 인프라가 없거나 계정이 사라진 환경에서 테스트가 <b>에러가 아니라 skip</b> 된다
 * ({@code @EnabledIf} 가 컨텍스트 기동 전에 평가).
 *
 * <p>좌표/자격증명 기본값은 로컬 개발 인프라 기준이며(메모리 {@code local-infra-db-redis}),
 * 모두 시스템 프로퍼티({@code live.pg.url}, {@code live.redis.host} 등)로 override 가능하다.
 * 테스트는 이 상수들을 {@code @DynamicPropertySource} 로 Spring 에 그대로 주입해
 * 게이트와 컨텍스트가 동일한 좌표를 쓰도록 단일 출처를 유지한다.
 */
public final class LiveInfra {

    private LiveInfra() {
    }

    public static final String PG_URL =
            System.getProperty("live.pg.url", "jdbc:postgresql://localhost:15010/local.test.common");
    public static final String PG_USERNAME = System.getProperty("live.pg.username", "cain");
    public static final String PG_PASSWORD = secret("live.pg.password", "spring.datasource.password");

    public static final String REDIS_HOST = System.getProperty("live.redis.host", "localhost");
    public static final int REDIS_PORT = Integer.getInteger("live.redis.port", 16010);
    // Redis 의 cain ACL 은 이 인스턴스에서 영속 불가(재시작 시 소실)하므로, 안정적으로 영속되는
    // default 계정(requirepass)을 기본으로 쓴다. cain 으로 돌리려면 live.redis.username 로 override.
    public static final String REDIS_USERNAME = System.getProperty("live.redis.username", "default");
    public static final String REDIS_PASSWORD = secret("live.redis.password", "spring.data.redis.password");

    public static boolean liveTestsEnabled() {
        return Boolean.getBoolean("live.tests.enabled");
    }

    /** JDBC 로 실제 로그인까지 성공해야 {@code true}(포트만 열린 상태로는 부족). */
    public static boolean postgresReachable() {
        if (!liveTestsEnabled()) {
            return false;
        }
        int prev = DriverManager.getLoginTimeout();
        try {
            DriverManager.setLoginTimeout(2);
            try (Connection c = DriverManager.getConnection(PG_URL, PG_USERNAME, PG_PASSWORD)) {
                return c.isValid(2);
            }
        } catch (Throwable t) {
            return false;
        } finally {
            DriverManager.setLoginTimeout(prev);
        }
    }

    /** RESP 로 {@code AUTH}+{@code PING} 까지 성공해야 {@code true}(자격증명 확인). */
    public static boolean redisReachable() {
        if (!liveTestsEnabled()) {
            return false;
        }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(REDIS_HOST, REDIS_PORT), 2000);
            s.setSoTimeout(2000);
            OutputStream out = s.getOutputStream();
            out.write(("AUTH " + REDIS_USERNAME + " " + REDIS_PASSWORD + "\r\nPING\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            byte[] buf = new byte[64];
            int n = s.getInputStream().read(buf);
            String resp = n > 0 ? new String(buf, 0, n, StandardCharsets.US_ASCII) : "";
            // AUTH 성공이면 첫 응답이 +OK. -WRONGPASS/-NOAUTH/-ERR 면 인증 실패.
            return resp.startsWith("+OK");
        } catch (Throwable t) {
            return false;
        }
    }

    private static String secret(String systemPropertyName, String localSecretPropertyName) {
        String override = System.getProperty(systemPropertyName);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return localSecret(localSecretPropertyName);
    }

    private static String localSecret(String propertyName) {
        Path path = Path.of(System.getProperty("user.home"), ".mutuus", "sample-api", "local.yml");
        FileSystemResource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            return "";
        }
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("sample-api-local-secret", resource);
            for (PropertySource<?> source : sources) {
                Object value = source.getProperty(propertyName);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }
}
