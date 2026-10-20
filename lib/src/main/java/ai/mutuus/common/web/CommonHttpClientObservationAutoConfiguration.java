package ai.mutuus.common.web;
import ai.mutuus.common.core.LogFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.context.annotation.*;
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.http.client.ClientHttpRequestInterceptor")
@EnableConfigurationProperties(CommonHttpProperties.class)
public class CommonHttpClientObservationAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mutuus.common.http.client-logging", name = "enabled", matchIfMissing = true)
    public HttpClientLoggingInterceptor httpClientLoggingInterceptor(
            CommonHttpProperties props,
            // 로그 포맷은 액세스 로깅과 동일 출처를 원시 프로퍼티로만 읽어 web→logging 패키지 결합을 피한다.
            @Value("${mutuus.common.logging.format:DUAL}") LogFormat format) {
        return new HttpClientLoggingInterceptor(format, props.getClientLogging().getSlowRequestThresholdMillis());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.client-logging", name = "enabled", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "false", matchIfMissing = true)
    static class HttpClientLoggingRestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpClientLoggingRestClientCustomizer")
        RestClientCustomizer httpClientLoggingRestClientCustomizer(HttpClientLoggingInterceptor interceptor) {
            return builder -> builder.requestInterceptor(interceptor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestTemplateCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.client-logging", name = "enabled", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "false", matchIfMissing = true)
    static class HttpClientLoggingRestTemplateConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpClientLoggingRestTemplateCustomizer")
        RestTemplateCustomizer httpClientLoggingRestTemplateCustomizer(HttpClientLoggingInterceptor interceptor) {
            return restTemplate -> restTemplate.getInterceptors().add(interceptor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "true")
    static class HttpRetryConfiguration {

        @Bean
        @ConditionalOnMissingBean
        HttpRetryInterceptor httpRetryInterceptor(CommonHttpProperties props, ObjectProvider<HttpClientLoggingInterceptor> observation) {
            return new HttpRetryInterceptor(props.getRetry().getMaxAttempts(), observation.getIfAvailable());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "true")
    static class HttpRetryRestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpRetryRestClientCustomizer")
        RestClientCustomizer httpRetryRestClientCustomizer(HttpRetryInterceptor interceptor) {
            return builder -> builder.requestInterceptor(interceptor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestTemplateCustomizer.class)
    @ConditionalOnProperty(prefix = "mutuus.common.http.retry", name = "enabled", havingValue = "true")
    static class HttpRetryRestTemplateConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "httpRetryRestTemplateCustomizer")
        RestTemplateCustomizer httpRetryRestTemplateCustomizer(HttpRetryInterceptor interceptor) {
            return restTemplate -> restTemplate.getInterceptors().add(interceptor);
        }
    }}
