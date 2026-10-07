package ai.mutuus.common.logging;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 웹 비의존 업무 writer. TX 타입이 소비 classpath에 있을 때만 완료 어댑터를 제공한다. */
@AutoConfiguration
public class CommonActionAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    ActionLogger actionLogger(ObjectProvider<ActionCompletionHandler> completion) {
        return completion.getIfAvailable() == null ? new ActionLogger() : new ActionLogger(completion.getObject());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.transaction.support.TransactionSynchronizationManager")
    static class TransactionConfiguration {
        @Bean @ConditionalOnMissingBean
        ActionCompletionHandler actionCompletionHandler() { return new TransactionActionCompletionHandler(); }
    }
}
