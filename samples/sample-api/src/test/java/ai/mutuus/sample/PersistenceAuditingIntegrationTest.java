package ai.mutuus.sample;

import java.time.Instant;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소비 서비스가 라이브러리의 {@link ai.mutuus.common.persistence.BaseEntity} 만 상속해도,
 * 별도 설정 없이 JPA Auditing 으로 감사 컬럼이 자동 기록되는지(H2) 검증한다.
 * <p>특히 생성/수정 <b>주체</b>가 {@code TraceContext} 의 인증 사용자에서 자동으로 채워지는지
 * (라이브러리의 {@code TraceContextAuditorAware} 동작) 확인한다.
 */
@SpringBootTest
@Import(PersistenceAuditingIntegrationTest.TestSecurityConfig.class)
class PersistenceAuditingIntegrationTest {

    @Autowired
    SampleNoteRepository repository;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void 저장시_감사_시각이_채워지고_생성_수정자는_TraceContext_사용자다() {
        TraceContext.put(HeaderNames.USER_ID, "u-audit");

        SampleNote saved = repository.saveAndFlush(new SampleNote("hello"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("u-audit");
        assertThat(saved.getUpdatedBy()).isEqualTo("u-audit");
    }

    @Test
    void 인증_사용자가_없으면_감사_주체는_비고_시각은_그대로_기록된다() {
        SampleNote saved = repository.saveAndFlush(new SampleNote("anon"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isNull();
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
