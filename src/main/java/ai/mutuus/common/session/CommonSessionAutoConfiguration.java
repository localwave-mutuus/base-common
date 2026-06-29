package ai.mutuus.common.session;

import ai.mutuus.common.config.CommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.util.StringUtils;

/**
 * 공통 분산 세션 자동 구성. Spring Session Redis 가 classpath 에 있을 때만 동작한다.
 * <p>Spring Boot 가 만든 {@link RedisSessionRepository} 에 <b>컨벤션 기본값</b>을 주입한다:
 * 키 네임스페이스는 서비스명 기반({@code <service-name>:session})으로 잡아 여러 서비스가 같은
 * Redis 를 공유해도 충돌하지 않게 하고, 기본 세션 타임아웃을 적용한다. 실제 Redis 연결/세션
 * 저장소 자체는 Boot 의 세션 자동구성에 위임한다(이 클래스는 컨벤션만 얹는다).
 * <p>{@code mutuus.common.session.enabled=false} 로 끌 수 있고, 소비 서비스가 자체 커스터마이저를
 * 정의하면 그 값이 우선한다.
 */
@AutoConfiguration
@ConditionalOnClass(RedisSessionRepository.class)
@ConditionalOnProperty(prefix = "mutuus.common.session", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(CommonSessionProperties.class)
public class CommonSessionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "commonRedisSessionCustomizer")
    SessionRepositoryCustomizer<RedisSessionRepository> commonRedisSessionCustomizer(
            CommonSessionProperties props, ObjectProvider<CommonProperties> commonProperties) {
        String serviceName = commonProperties.getIfAvailable(CommonProperties::new).getServiceName();
        String namespace = StringUtils.hasText(props.getNamespace())
                ? props.getNamespace() : serviceName + ":session";
        return repository -> {
            repository.setRedisKeyNamespace(namespace);
            repository.setDefaultMaxInactiveInterval(props.getTimeout());
        };
    }
}
