package ai.mutuus.common.observability;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * 공통 관측 자동 구성.
 * <p>Micrometer Tracing/OTel 및 Prometheus 는 spring-boot-actuator 자동구성에 위임하며,
 * 여기서는 공통 태그(서비스명 등) 커스터마이징 지점을 제공한다.
 */
@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
public class CommonObservabilityAutoConfiguration {

    /**
     * 공통 관측 커스터마이저 등록 지점(예: low-cardinality 태그 추가).
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
