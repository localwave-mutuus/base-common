package ai.mutuus.common.agent;
import org.junit.jupiter.api.Test;
class Http2TransportContractTest {
    @Test void http2RedirectAndRefusedStreamRetry() throws Exception {
        FixedAgentHarnessTest.run(Http2TransportContractMain.class.getName(),"HTTP2 CONTRACT PASS","http2-contract.log");
    }
}
