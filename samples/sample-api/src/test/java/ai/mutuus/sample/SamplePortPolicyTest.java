package ai.mutuus.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the sample port convention without opening sockets.
 */
class SamplePortPolicyTest {

    @Test
    void default_sample_port_is_8090_and_test_ports_are_8095_to_8099() throws IOException {
        String app = Files.readString(path("samples/sample-api/src/main/resources/application.yml",
                "src/main/resources/application.yml"));
        String ai = Files.readString(path("samples/sample-api/src/main/resources/application-ai.yml",
                "src/main/resources/application-ai.yml"));

        assertThat(app)
                .contains("port: ${SAMPLE_API_PORT:8090}")
                .contains("app: 8090")
                .contains("test-range: 8095-8099");
        assertThat(ai)
                .contains("port: ${SAMPLE_API_TEST_PORT:8095}")
                .contains("app: 8090")
                .contains("test-range: 8095-8099");
    }

    private static Path path(String fromRoot, String fromSample) {
        Path root = Path.of(fromRoot);
        return Files.exists(root) ? root : Path.of(fromSample);
    }
}
