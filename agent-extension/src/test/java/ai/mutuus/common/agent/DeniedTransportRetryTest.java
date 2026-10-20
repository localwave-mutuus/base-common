package ai.mutuus.common.agent;
import org.junit.jupiter.api.Test;
class DeniedTransportRetryTest {
    @Test void http2DeniedRetry() throws Exception {
        FixedAgentHarnessTest.run(Http2DeniedRetryMain.class.getName(),"HTTP2 DENIED PASS","http2-denied-retry.log");
    }
    @Test void springDeniedRetry() throws Exception {
        FixedAgentHarnessTest.run(SpringDeniedRetryMain.class.getName(),"SPRING DENIED PASS","spring-denied-retry.log");
    }
}
