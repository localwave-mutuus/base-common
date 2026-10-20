package ai.mutuus.common.aop;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
@AutoConfiguration(after=ai.mutuus.common.payload.CommonSafePayloadAutoConfiguration.class)
@ConditionalOnClass(org.aspectj.lang.ProceedingJoinPoint.class)
@ConditionalOnProperty(prefix="mutuus.common.service-logging",name="enabled",havingValue="true")
@EnableConfigurationProperties(ServiceMethodLoggingProperties.class)
@EnableAspectJAutoProxy
public class CommonServiceMethodLoggingAutoConfiguration {
 @Bean @ConditionalOnMissingBean public ServiceMethodLoggingWriter serviceMethodLoggingWriter(){return ServiceMethodLoggingWriter.standard();}
 @Bean public ServiceMethodLoggingAspect serviceMethodLoggingAspect(ServiceMethodLoggingProperties p,ServiceMethodLoggingWriter w,ai.mutuus.common.payload.SafePayloadPolicy policy){return new ServiceMethodLoggingAspect(p,w,policy);}
}
