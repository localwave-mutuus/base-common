package ai.mutuus.common.payload;

import ai.mutuus.common.core.SensitiveDataMasker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 컨트롤러 입출력 본문 로깅 자동 구성(웹 환경 전용, <b>기본 비활성</b>).
 * <p>{@code mutuus.common.payload-logging.enabled=true} 일 때만 활성화된다(본문은 PII·비용 우려).
 * 진입(입력)·종료(리턴) 출력 함수를 각각 {@link RequestPayloadLogger}/{@link ResponsePayloadLogger}
 * 빈으로 등록하며, 둘 다 {@code @ConditionalOnMissingBean} 이라 소비 서비스가 자체 빈으로 교체/갱신할 수 있다.
 * <p>등록: {@code META-INF/spring/...AutoConfiguration.imports}.
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(PayloadLoggingProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.payload-logging", name = "enabled", havingValue = "true")
public class CommonPayloadLoggingAutoConfiguration {

    /** 입력 파라미터 출력 함수(기본 구현). 소비자가 재정의하면 비활성. */
    @Bean
    @ConditionalOnMissingBean
    public RequestPayloadLogger requestPayloadLogger(ObjectProvider<SensitiveDataMasker> masker) {
        return new DefaultRequestPayloadLogger(masker.getIfAvailable());
    }

    /** 리턴값 출력 함수(기본 구현). 소비자가 재정의하면 비활성. */
    @Bean
    @ConditionalOnMissingBean
    public ResponsePayloadLogger responsePayloadLogger(ObjectProvider<SensitiveDataMasker> masker) {
        return new DefaultResponsePayloadLogger(masker.getIfAvailable());
    }

    /** 모든 인입 요청을 감싸는 입출력 로깅 필터. AccessLogFilter(+10) 바로 안쪽(+20). */
    @Bean
    public FilterRegistrationBean<PayloadLoggingFilter> payloadLoggingFilterRegistration(
            RequestPayloadLogger requestPayloadLogger, ResponsePayloadLogger responsePayloadLogger,
            PayloadLoggingProperties props) {
        FilterRegistrationBean<PayloadLoggingFilter> reg = new FilterRegistrationBean<>(
                new PayloadLoggingFilter(requestPayloadLogger, responsePayloadLogger, props));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
