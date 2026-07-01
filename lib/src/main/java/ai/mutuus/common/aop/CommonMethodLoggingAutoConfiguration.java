package ai.mutuus.common.aop;

import ai.mutuus.common.core.SensitiveDataMasker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

/**
 * 컨트롤러 메서드 인자/리턴 로깅(AOP) 자동 구성. <b>기본 비활성</b>.
 * <p>{@code mutuus.common.method-logging.enabled=true} 이고 AspectJ/웹이 classpath 에 있을 때만 활성화된다
 * (소비자가 {@code spring-boot-starter-aop} 를 추가해야 함 — 라이브러리에선 optional). 출력 함수
 * ({@link MethodLoggingWriter})는 {@code @ConditionalOnMissingBean} 이라 소비 서비스가 교체/갱신할 수 있다.
 */
@AutoConfiguration
@ConditionalOnClass({ ProceedingJoinPoint.class, RestController.class })
@EnableConfigurationProperties(MethodLoggingProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.method-logging", name = "enabled", havingValue = "true")
public class CommonMethodLoggingAutoConfiguration {

    /** 인자/리턴 출력 함수(기본 구현). 소비자가 재정의하면 비활성. */
    @Bean
    @ConditionalOnMissingBean
    public MethodLoggingWriter methodLoggingWriter(MethodLoggingProperties props,
                                                   ObjectProvider<SensitiveDataMasker> masker) {
        return new DefaultMethodLoggingWriter(props.getMaxLength(), masker.getIfAvailable());
    }

    @Bean
    public ControllerMethodLoggingAspect controllerMethodLoggingAspect(
            MethodLoggingWriter writer, MethodLoggingProperties props) {
        return new ControllerMethodLoggingAspect(writer, props);
    }
}
