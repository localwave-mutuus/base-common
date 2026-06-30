package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.sample.persistence.SampleNote;
import ai.mutuus.sample.persistence.SampleNoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code local} 프로파일이 환경별 설정파일({@code application-local.yml})만으로 실 PostgreSQL 에
 * 연결됨을 end-to-end 로 검증한다. {@link PostgresLiveAuditingIntegrationTest} 와 달리
 * {@code @DynamicPropertySource} 로 좌표를 주입하지 않고 <b>프로파일 파일이 직접</b> 데이터소스를
 * 물린다 — 즉 환경별 설정 분리가 실제로 작동함을 보여준다.
 * <p>로컬 실 PG 에 접속·인증 가능할 때만 실행되고 아니면 자동 skip 된다
 * ({@link ai.mutuus.sample.support.LiveInfra#postgresReachable()}).
 */
@SpringBootTest
@ActiveProfiles("local")
@EnabledIf(value = "ai.mutuus.sample.support.LiveInfra#postgresReachable",
        disabledReason = "로컬 실 PostgreSQL(기본 localhost:15010, cain/eva) 에 접속할 수 없음")
@Import(LocalProfileDatasourceTest.TestSecurityConfig.class)
class LocalProfileDatasourceTest {

    @Autowired
    Environment env;

    @Autowired
    SampleNoteRepository repository;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void local_프로파일은_환경별_설정으로_실_PostgreSQL에_연결되어_감사가_동작한다() {
        // 프로파일 파일이 로딩됐고 그 데이터소스 좌표가 적용됐는지
        assertThat(env.getActiveProfiles()).containsExactly("local");
        assertThat(env.getProperty("demo.environment")).isEqualTo("local");
        assertThat(env.getProperty("spring.datasource.url")).contains(":15010/local.test.common");

        // 그 데이터소스가 실제로 동작하는지 — 감사 저장으로 실 DB 왕복 확인
        TraceContext.put(HeaderNames.USER_ID, "u-local");
        SampleNote saved = repository.saveAndFlush(new SampleNote("via local profile"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("u-local");
        assertThat(saved.getCreatedAt()).isNotNull();
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
