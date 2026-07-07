package ai.mutuus.sample;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Build policy guardrails for the sample consumer.
 *
 * <p>These tests do not start a server or open a database connection. They keep
 * the PostgreSQL-only migration decisions from regressing silently in Maven
 * configuration.
 */
class BuildPolicyTest {

    @Test
    void default_build_does_not_run_jooq_ddl_database_codegen() throws Exception {
        Document pom = pom();
        NodeList defaultJooqPlugins = nodes(pom,
                "/*[local-name()='project']/*[local-name()='build']/*[local-name()='plugins']"
                        + "/*[local-name()='plugin'][*[local-name()='artifactId']='jooq-codegen-maven']");

        assertThat(defaultJooqPlugins.getLength()).isZero();

        String pomText = Files.readString(sampleApiRoot().resolve("pom.xml"));
        assertThat(pomText).doesNotContain("org.jooq.meta.extensions.ddl.DDLDatabase");
        assertThat(pomText).doesNotContain("jooq-meta-extensions");
    }

    @Test
    void postgres_codegen_is_explicit_profile_and_uses_environment_credentials() throws Exception {
        Document pom = pom();

        assertThat(text(pom, profile("/*[local-name()='properties']/*[local-name()='jooq.codegen.url']")))
                .isEqualTo("${env.JOOQ_CODEGEN_URL}");
        assertThat(text(pom, profile("/*[local-name()='properties']/*[local-name()='jooq.codegen.username']")))
                .isEqualTo("${env.JOOQ_CODEGEN_USERNAME}");
        assertThat(text(pom, profile("/*[local-name()='properties']/*[local-name()='jooq.codegen.password']")))
                .isEqualTo("${env.JOOQ_CODEGEN_PASSWORD}");
        assertThat(text(pom, profile("//*[local-name()='database']/*[local-name()='name']")))
                .isEqualTo("org.jooq.meta.postgres.PostgresDatabase");
        assertThat(text(pom, profile("//*[local-name()='target']/*[local-name()='directory']")))
                .isEqualTo("src/main/java");
    }

    @Test
    void generated_jooq_sources_are_checked_in_for_database_free_default_compile() {
        Path generated = sampleApiRoot()
                .resolve("src/main/java/ai/mutuus/sample/board/jooq/gen/Tables.java");

        assertThat(generated).exists().isRegularFile();
    }

    private static Document pom() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(sampleApiRoot().resolve("pom.xml").toFile());
    }

    private static NodeList nodes(Document document, String expression) throws Exception {
        return (NodeList) XPathFactory.newInstance().newXPath()
                .evaluate(expression, document, XPathConstants.NODESET);
    }

    private static String text(Document document, String expression) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(expression, document);
    }

    private static String profile(String suffix) {
        return "/*[local-name()='project']/*[local-name()='profiles']/*[local-name()='profile']"
                + "[*[local-name()='id']='pg-codegen']" + suffix;
    }

    private static Path sampleApiRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("pom.xml"))
                && Files.exists(cwd.resolve("src/main/java/ai/mutuus/sample/SampleApiApplication.java"))) {
            return cwd;
        }
        Path nested = cwd.resolve("samples/sample-api");
        if (Files.exists(nested.resolve("pom.xml"))) {
            return nested;
        }
        throw new IllegalStateException("Cannot locate samples/sample-api");
    }
}
