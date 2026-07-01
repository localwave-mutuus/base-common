package ai.mutuus.common.response;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 성공 응답 자동 래핑 자동 구성(웹 MVC 전용, <b>기본 비활성</b>).
 * <p>{@code mutuus.common.response-wrapper.enabled=true} 일 때만 {@link ApiResponseWrapperAdvice}
 * 를 등록한다. 소비 서비스가 자체 {@code ResponseBodyAdvice}/래퍼를 정의하면 {@code @ConditionalOnMissingBean}
 * 으로 비켜선다.
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(ResponseWrapperProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.response-wrapper", name = "enabled", havingValue = "true")
public class CommonResponseWrapperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiResponseWrapperAdvice apiResponseWrapperAdvice(ResponseWrapperProperties props) {
        return new ApiResponseWrapperAdvice(props);
    }
}
