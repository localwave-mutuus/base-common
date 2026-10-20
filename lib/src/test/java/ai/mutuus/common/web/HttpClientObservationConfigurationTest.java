package ai.mutuus.common.web;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.mock.http.client.*;
import org.springframework.http.*;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
class HttpClientObservationConfigurationTest {
    @Test void nonMvcConfigurationHasSingleObservationAndRetryWrapsEachAttempt() {
        new ApplicationContextRunner().withClassLoader(new FilteredClassLoader("org.springframework.web.servlet"))
            .withConfiguration(AutoConfigurations.of(CommonHttpClientObservationAutoConfiguration.class))
            .withPropertyValues("mutuus.common.http.retry.enabled=true", "mutuus.common.http.retry.max-attempts=2")
            .run(context->{
                assertThat(context).hasSingleBean(HttpClientLoggingInterceptor.class).hasSingleBean(HttpRetryInterceptor.class);
                assertThat(context.containsBean("httpClientLoggingRestClientCustomizer")).isFalse();
                assertThat(context.containsBean("httpRetryRestClientCustomizer")).isTrue();
                var logger=(Logger)LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
                var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
                try {
                    var count=new AtomicInteger();
                    var request=new MockClientHttpRequest(HttpMethod.GET,"http://localhost/private?token=SECRET");
                    var response=context.getBean(HttpRetryInterceptor.class).intercept(request,new byte[0],(req,body)->{
                        if(count.incrementAndGet()==1)throw new java.io.IOException("SECRET");
                        return new MockClientHttpResponse(new byte[0],HttpStatus.FORBIDDEN);
                    });
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);assertThat(appender.list).hasSize(2);
                    assertThat(appender.list.getFirst().getKeyValuePairs().toString()).contains("HTTP_IO_ERROR").doesNotContain("SECRET","private");
                    assertThat(appender.list.getLast().getKeyValuePairs().toString()).contains("failure","HTTP_403");
                } finally {logger.detachAppender(appender);appender.stop();}
            });
    }
}
