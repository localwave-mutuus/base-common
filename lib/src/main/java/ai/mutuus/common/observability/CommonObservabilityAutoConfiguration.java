package ai.mutuus.common.observability;

import ai.mutuus.common.config.CommonProperties;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 공통 관측 자동 구성.
 * <p>Micrometer Tracing/OTel 및 Prometheus 는 spring-boot-actuator 자동구성에 위임하고,
 * 여기서는 <b>모든 관측(Observation)에 공통 저(低)카디널리티 태그</b>를 부여한다:
 * {@code service.name = <service-name>}(= {@code mutuus.common.service-name}). 이로써 메트릭/추적이
 * 서비스 단위로 일관되게 분류된다. Boot 의 관측 자동구성이 컨텍스트의 {@link ObservationFilter}
 * 빈을 레지스트리에 자동 적용한다.
 * <p>토글: {@code mutuus.common.observability.common-tags-enabled=false}. 소비 서비스가 동일
 * 이름의 빈을 정의하면 그 값이 우선한다.
 */
@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
public class CommonObservabilityAutoConfiguration {

    /** 모든 관측에 {@code service.name} 저카디널리티 태그를 부여하는 공통 필터. */
    @Bean
    @ConditionalOnMissingBean(name = "commonServiceTagObservationFilter")
    @ConditionalOnProperty(prefix = "mutuus.common.observability", name = "common-tags-enabled",
            matchIfMissing = true)
    public ObservationFilter commonServiceTagObservationFilter(ObjectProvider<CommonProperties> commonProperties) {
        String serviceName = commonProperties.getIfAvailable(CommonProperties::new).getServiceName();
        return context -> context.addLowCardinalityKeyValue(KeyValue.of("service.name", serviceName));
    }

    /**
     * 공통 관측 커스터마이저 등록 지점(추가 태그/컨벤션 확장용).
     * 실제 태깅 정책은 프로젝트별로 확장한다.
     */
    @Bean
    @ConditionalOnBean(ObservationRegistry.class)
    public CommonObservationMarker commonObservationMarker() {
        return new CommonObservationMarker();
    }

    /** 자동구성 적용 여부 확인용 마커 빈. */
    public static final class CommonObservationMarker {
    }
}
