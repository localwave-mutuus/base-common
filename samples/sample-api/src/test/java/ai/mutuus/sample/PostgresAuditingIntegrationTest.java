package ai.mutuus.sample;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.sample.persistence.SampleNote;
import ai.mutuus.sample.persistence.SampleNoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * H2 가 아닌 <b>실제 PostgreSQL</b>(Testcontainers)에서도 BaseEntity 감사가 동작하는지 검증한다.
 * <p>스키마 자동생성·{@code Instant}→{@code timestamptz} 매핑·TraceContext 기반 주체 기록을
 * 운영 DB 엔진에 가깝게 확인한다. <b>Docker 가 없으면 클래스 전체가 자동 skip</b> 되므로
 * (로컬 무도커 환경에서 빌드를 깨지 않음), CI/개발 환경에서만 실연동으로 실행된다.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(PostgresAuditingIntegrationTest.TestSecurityConfig.class)
class PostgresAuditingIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    SampleNoteRepository repository;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void 실제_Postgres에서도_감사_시각과_주체가_자동_기록된다() {
        TraceContext.put(HeaderNames.USER_ID, "u-pg");

        SampleNote saved = repository.saveAndFlush(new SampleNote("on postgres"));
        Instant createdAt = saved.getCreatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("u-pg");
        assertThat(saved.getUpdatedBy()).isEqualTo("u-pg");

        // 재조회해도 동일하게 영속되어 있어야 한다(실 DB 왕복).
        SampleNote reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedBy()).isEqualTo("u-pg");
        // Postgres timestamptz 는 마이크로초로 반올림하므로 JVM nanos 와 정확히 같지 않다(밀리초 허용).
        assertThat(reloaded.getCreatedAt()).isCloseTo(createdAt, within(1, ChronoUnit.MILLIS));
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!"valid-token".equals(token)) {
                    throw new BadJwtException("invalid token");
                }
                return Jwt.withTokenValue(token).header("alg", "none").subject("user-1")
                        .claim("roles", List.of("USER"))
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            };
        }
    }
}
