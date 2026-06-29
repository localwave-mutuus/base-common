package ai.mutuus.common.session;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 분산 세션 자동구성의 조건부 동작 검증(실제 Redis 없이).
 * <p>세션 컨벤션 커스터마이저가 Spring Session Redis 존재/스위치/사용자 빈 조건에 따라
 * 의도대로 켜고 꺼지는지 확인한다. 실제 Redis 저장 검증은 sample-api 의 Redis Testcontainers
 * 통합 테스트에서 수행한다.
 */
class CommonSessionAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonSessionAutoConfiguration.class));

    @Test
    void 기본적으로_세션_컨벤션_커스터마이저가_등록된다() {
        runner.run(ctx -> {
            assertThat(ctx).hasBean("commonRedisSessionCustomizer");
            assertThat(ctx).hasSingleBean(SessionRepositoryCustomizer.class);
        });
    }

    @Test
    void session_enabled가_false면_비활성화된다() {
        runner.withPropertyValues("mutuus.common.session.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SessionRepositoryCustomizer.class));
    }

    @Test
    void SpringSession_Redis가_classpath에_없으면_비활성화된다() {
        runner.withClassLoader(new FilteredClassLoader(RedisSessionRepository.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SessionRepositoryCustomizer.class));
    }

    @Test
    void 소비_서비스가_동일_이름의_커스터마이저를_정의하면_그것을_사용한다() {
        SessionRepositoryCustomizer<RedisSessionRepository> custom = repo -> { };
        runner.withBean("commonRedisSessionCustomizer", SessionRepositoryCustomizer.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(SessionRepositoryCustomizer.class);
                    assertThat(ctx.getBean("commonRedisSessionCustomizer")).isSameAs(custom);
                });
    }
}
