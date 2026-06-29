package ai.mutuus.common.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 영속성 자동구성의 조건부 동작 검증(실제 JPA 부트스트랩 없이).
 * <p>JPA Auditing 활성화 자체는 {@code EntityManagerFactory} 가 있어야 하므로
 * sample-api 의 H2 통합 테스트에서 검증하고, 여기서는 감사 주체 빈 등록과
 * 조건부 on/off 만 확인한다(EMF 부재 → 감사 활성화 분기는 건너뜀).
 */
class CommonPersistenceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonPersistenceAutoConfiguration.class));

    @Test
    void JPA가_있으면_TraceContext_기반_AuditorAware가_등록된다() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(AuditorAware.class);
            assertThat(ctx.getBean(AuditorAware.class)).isInstanceOf(TraceContextAuditorAware.class);
        });
    }

    @Test
    void auditing_enabled가_false면_자동구성이_비활성화된다() {
        runner.withPropertyValues("mutuus.common.persistence.auditing-enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditorAware.class));
    }

    @Test
    void spring_data_jpa가_classpath에_없으면_자동구성_전체가_비활성화된다() {
        runner.withClassLoader(new FilteredClassLoader(AuditingEntityListener.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditorAware.class));
    }

    @Test
    void 소비_서비스가_자체_AuditorAware를_정의하면_그것을_사용한다() {
        AuditorAware<String> custom = () -> java.util.Optional.of("system");
        runner.withBean("customAuditor", AuditorAware.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(AuditorAware.class)).isSameAs(custom));
    }
}
