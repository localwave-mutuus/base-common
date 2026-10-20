package ai.mutuus.common.agent;
import org.junit.jupiter.api.Test;
class SpringTransportContractTest {
    @Test void simpleFactoryRedirectAndHiddenRetry() throws Exception {
        FixedAgentHarnessTest.run(SpringTransportContractMain.class.getName(),"SPRING CONTRACT PASS","spring-transport-contract.log");
    }
    @Test void restClientRemovesPreviouslyPresentHeadersAndAvoidsOuterCompletion() throws Exception {
        FixedAgentHarnessTest.run(SpringResidualContractMain.class.getName(),"SPRING RESIDUAL PASS","spring-residual-contract.log");
    }
    @Test void jdkFactoryHasAgentAttemptCompletionWithoutInterceptorDuplicate() throws Exception {
        FixedAgentHarnessTest.run(SpringJdkContractMain.class.getName(),"SPRING JDK PASS","spring-jdk-contract.log");
    }
}
