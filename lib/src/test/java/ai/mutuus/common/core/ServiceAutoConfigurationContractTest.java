package ai.mutuus.common.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.context.annotation.*;
import ai.mutuus.common.aop.*;
import ai.mutuus.common.payload.*;
class ServiceAutoConfigurationContractTest {
 static final class Unrelated { public String run(){return "ok";} }
 @Configuration(proxyBeanMethods=false) static class Beans {@Bean Unrelated unrelated(){return new Unrelated();}}
 @Test void disabledByDefaultAndEnabledDoesNotProxyUnrelatedFinalBeans(){
  var runner=new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CommonSafePayloadAutoConfiguration.class,CommonServiceMethodLoggingAutoConfiguration.class)).withUserConfiguration(Beans.class);
  runner.run(c->{assertNull(c.getStartupFailure());assertEquals(0,c.getBeansOfType(ServiceMethodLoggingAspect.class).size());});
  runner.withPropertyValues("mutuus.common.service-logging.enabled=true").run(c->{assertNull(c.getStartupFailure());assertNotNull(c.getBean(ServiceMethodLoggingAspect.class));assertEquals("ok",c.getBean(Unrelated.class).run());});
 }
}
