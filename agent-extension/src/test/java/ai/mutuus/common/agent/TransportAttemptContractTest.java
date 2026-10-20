package ai.mutuus.common.agent;
import org.junit.jupiter.api.Test;
class TransportAttemptContractTest {
    @Test void bothRedirectDirectionsAndHiddenRetryRequireDestinationPolicyAndAttemptCompletions() throws Exception {
        FixedAgentHarnessTest.run(TransportAttemptContractMain.class.getName(),"TRANSPORT CONTRACT PASS","transport-attempt-contract.log");
    }
    @Test void previouslyPresentHeadersMustNotSurviveUnapprovedRedirect() throws Exception {
        FixedAgentHarnessTest.run(ResidualHeaderRedirectMain.class.getName(),"RESIDUAL CONTRACT PASS","residual-header-contract.log");
    }
}
