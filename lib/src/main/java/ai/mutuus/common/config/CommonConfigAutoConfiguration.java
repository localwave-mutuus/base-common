package ai.mutuus.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 공통 설정 모듈 자동 구성.
 * <p>{@link CommonProperties} 바인딩을 활성화한다.
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
public class CommonConfigAutoConfiguration {
}
