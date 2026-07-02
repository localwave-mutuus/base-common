package ai.mutuus.sample;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.sample.persistence.SampleNote;
import ai.mutuus.sample.persistence.SampleNoteRepository;
import ai.mutuus.sample.support.LiveInfra;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 이미 떠 있는 <b>로컬 실 PostgreSQL</b>(메모리 {@code local-infra-db-redis} 좌표)을 대상으로
 * BaseEntity 감사를 검증한다. Testcontainers 변형({@link PostgresAuditingIntegrationTest})은
 * Docker 가 있어야만 돌지만, 이 테스트는 <b>실 DB 에 자격증명으로 접속 가능할 때만</b> 돌고
 * 아니면 자동 skip 된다({@link LiveInfra#postgresReachable()}).
 *
 * <p>생성 경로(시각/주체)에 더해 <b>수정 경로</b>까지 본다: 다른 사용자로 엔티티를 수정하면
 * {@code updated_by}/{@code updated_at} 만 갱신되고 {@code created_*} 는 불변이어야 한다
 * (라이브러리의 {@code @LastModified*} + TraceContext 주체 동작을 실 DB 왕복으로 확인).
 */
@SpringBootTest
@EnabledIf(value = "ai.mutuus.sample.support.LiveInfra#postgresReachable",
        disabledReason = "로컬 실 PostgreSQL(기본 localhost:15010, cain/eva) 에 접속할 수 없음")
@Import(PostgresLiveAuditingIntegrationTest.TestSecurityConfig.class)
class PostgresLiveAuditingIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> LiveInfra.PG_URL);
        registry.add("spring.datasource.username", () -> LiveInfra.PG_USERNAME);
        registry.add("spring.datasource.password", () -> LiveInfra.PG_PASSWORD);
        // 스키마는 Flyway 가 실 PostgreSQL 에 마이그레이션하고 Hibernate 는 validate(base 상속) — 운영 패턴 그대로
        // 실 DB 로 검증한다. create-drop(스키마 드롭)은 Flyway 가 만든 테이블을 지워 schema_history 와 어긋나므로
        // 쓰지 않는다. 공유 실 DB 오염 방지는 스키마 드롭이 아니라 @AfterEach 의 "데이터 정리"로 한다.
    }

    @Autowired
    SampleNoteRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll(); // 이 테스트가 만든 행만 정리(스키마는 Flyway 관리라 유지) — 공유 실 DB 오염 방지
        TraceContext.clear();
    }

    @Test
    void 실_Postgres에서_생성_감사가_기록되고_재조회로_영속이_확인된다() {
        TraceContext.put(HeaderNames.USER_ID, "u-pg");

        SampleNote saved = repository.saveAndFlush(new SampleNote("on postgres"));
        Instant createdAt = saved.getCreatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("u-pg");
        assertThat(saved.getUpdatedBy()).isEqualTo("u-pg");

        SampleNote reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedBy()).isEqualTo("u-pg");
        // Postgres timestamptz 는 마이크로초로 반올림 저장하므로 JVM nanos 와는 동등하지 않다.
        // 같은 시각이 왕복됐음만 확인(밀리초 허용).
        assertThat(reloaded.getCreatedAt()).isCloseTo(createdAt, within(1, ChronoUnit.MILLIS));
    }

    @Test
    void 수정시_수정_주체와_시각만_갱신되고_생성_감사는_불변이다() {
        TraceContext.put(HeaderNames.USER_ID, "u-create");
        SampleNote saved = repository.saveAndFlush(new SampleNote("v1"));
        Long id = saved.getId();
        Instant createdAt = saved.getCreatedAt();
        Instant firstUpdatedAt = saved.getUpdatedAt();

        // 다른 사용자가 수정 → UPDATE 유발
        TraceContext.put(HeaderNames.USER_ID, "u-modifier");
        saved.setText("v2");
        SampleNote updated = repository.saveAndFlush(saved);

        // 생성 감사는 불변 (createdAt 은 merge 시 DB 에서 다시 읽혀 마이크로초 정밀도라 밀리초 허용)
        assertThat(updated.getCreatedBy()).isEqualTo("u-create");
        assertThat(updated.getCreatedAt()).isCloseTo(createdAt, within(1, ChronoUnit.MILLIS));
        // 수정 감사만 갱신
        assertThat(updated.getUpdatedBy()).isEqualTo("u-modifier");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);

        // 실 DB 왕복으로도 동일
        SampleNote reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getCreatedBy()).isEqualTo("u-create");
        assertThat(reloaded.getUpdatedBy()).isEqualTo("u-modifier");
        assertThat(reloaded.getText()).isEqualTo("v2");
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
