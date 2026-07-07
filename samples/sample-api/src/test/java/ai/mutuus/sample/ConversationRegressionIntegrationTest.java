package ai.mutuus.sample;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression replay for the review conversation.
 *
 * <p>The user-facing demo pages are the manual test surface of this sample.
 * This test keeps the important conversation outcomes wired into that surface
 * and executes the non-destructive cases through a random local port.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConversationRegressionIntegrationTest {

    @LocalServerPort
    int port;

    private final RestClient client = RestClient.create();

    @Test
    void demo_catalog_exposes_conversation_regression_cases_for_manual_review() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cases = client.get()
                .uri(base() + "/demo/cases")
                .retrieve()
                .body(List.class);

        assertThat(cases).isNotNull();
        assertThat(cases).anySatisfy(c -> assertThat(c.get("id")).isEqualTo("secret-policy"));
        assertThat(cases).anySatisfy(c -> {
            assertThat(c.get("id")).isEqualTo("idem-fingerprint");
            assertThat(c.get("title")).isEqualTo("멱등성 - 같은 키 다른 요청 409");
        });
        assertThat(cases).anySatisfy(c -> {
            assertThat(c.get("id")).isEqualTo("build-policy");
            assertThat(c.get("title")).isEqualTo("빌드 정책 - PostgreSQL-only jOOQ 코드젠");
        });
    }

    @Test
    void idempotency_fingerprint_demo_replays_same_key_different_request_rejection() {
        Map<String, Object> response = getApiResponse("/demo/idem/fingerprint");
        Map<String, Object> data = data(response);

        assertThat(data.get("firstStatus")).isEqualTo(200);
        assertThat(data.get("secondStatus")).isEqualTo(409);
        assertThat(data.get("secondReplayHeader")).isEqualTo("fingerprint-mismatch");
        assertThat(data.get("fingerprintRejected")).isEqualTo(true);
    }

    @Test
    void build_policy_demo_exposes_postgresql_only_codegen_decision() {
        Map<String, Object> response = getApiResponse("/demo/build-policy");
        Map<String, Object> data = data(response);

        assertThat(data.get("postgresOnlyPolicy")).isEqualTo(true);
        assertThat(data.get("defaultBuildRunsDdlDatabaseCodegen")).isEqualTo(false);
        assertThat(data.get("pgCodegenProfileConfigured")).isEqualTo(true);
        assertThat(data.get("checkedInGeneratedSources")).isEqualTo(true);
        assertThat(data.get("codegenDatabase")).isEqualTo("PostgreSQL");
        assertThat(data.get("targetDatabaseName")).isEqualTo("golmok");
    }

    @Test
    void secret_policy_demo_exposes_external_import_decision() {
        Map<String, Object> response = getApiResponse("/demo/secret-policy");
        Map<String, Object> data = data(response);

        assertThat(data.get("policyPass")).isEqualTo(true);
        assertThat(data.get("importsExternalSecret")).isEqualTo(true);
        assertThat(data.get("localConfigHasNoPlainSamplePassword")).isEqualTo(true);
        assertThat(data.get("exampleFileExists")).isEqualTo(true);
        assertThat(data.get("canonicalSecretDocExists")).isEqualTo(true);
        assertThat(data.get("commonModuleGuard")).isEqualTo("security.config.secret_in_classpath_config");
    }

    @Test
    void security_page_describes_csrf_audit_without_false_positive() {
        ResponseEntity<byte[]> response = client.get()
                .uri(base() + "/demo/security.html")
                .retrieve()
                .toEntity(byte[].class);
        String body = new String(response.getBody(), StandardCharsets.UTF_8);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(body)
                .contains("세션 저장소가 있고 CSRF 설정이 비활성인 경우에만")
                .contains("false positive");
    }

    private Map<String, Object> getApiResponse(String path) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = client.get()
                .uri(base() + path)
                .retrieve()
                .body(Map.class);
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo("OK");
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> response) {
        assertThat(response.get("data")).isInstanceOf(Map.class);
        return (Map<String, Object>) response.get("data");
    }

    private String base() {
        return "http://localhost:" + port;
    }
}
