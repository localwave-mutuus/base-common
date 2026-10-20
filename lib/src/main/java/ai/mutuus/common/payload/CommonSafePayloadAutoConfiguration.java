package ai.mutuus.common.payload;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
@AutoConfiguration
@EnableConfigurationProperties(SafePayloadProperties.class)
public class CommonSafePayloadAutoConfiguration {
 @Bean @ConditionalOnMissingBean public PayloadPolicyRegistry payloadPolicyRegistry(){return new PayloadPolicyRegistry();}
 @Bean public org.springframework.beans.factory.SmartInitializingSingleton validatePayloadRegistry(PayloadPolicyRegistry registry,SafePayloadProperties props){return ()->{registry.validateAndSeal();ai.mutuus.common.logging.CommonJsonEncoder.configureSafeLimit(props.getMaxEventBytes());};}
 @Bean @ConditionalOnMissingBean public SafePayloadPolicy safePayloadPolicy(PayloadPolicyRegistry r,SafePayloadProperties p,ObjectProvider<ai.mutuus.common.core.SensitiveDataMasker> m){return new SafePayloadPolicy(r,p,m.getIfAvailable());}
 @org.springframework.context.annotation.Configuration(proxyBeanMethods=false)
 @ConditionalOnClass(name={"jakarta.servlet.Filter","org.springframework.web.servlet.HandlerMapping"})
 static class Web {
  @Bean @ConditionalOnProperty(prefix="mutuus.common.safe-payload",name="enabled",havingValue="true")
  public SafePayloadFilter safePayloadFilter(SafePayloadPolicy policy,SafePayloadProperties props){return new SafePayloadFilter(policy,props);}
 }
}
