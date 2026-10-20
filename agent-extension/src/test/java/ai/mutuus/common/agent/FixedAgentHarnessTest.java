package ai.mutuus.common.agent;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.jar.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

class FixedAgentHarnessTest {
    @Test void pinnedAgentLocalWireExporterAndCompletion() throws Exception {
        run(FixedAgentMain.class.getName(), "HARNESS PASS", "fixed-agent.log");
    }
    @Test void redirectDestinationContract() throws Exception {
        run(FixedAgentRedirectMain.class.getName(), "REDIRECT PASS", "redirect-agent.log");
    }
    static void run(String main, String marker, String logName) throws Exception {
        Path agent=Path.of("target/harness/opentelemetry-javaagent-2.28.0.jar");
        String digest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(agent)));
        assertThat(digest).isEqualTo("130606aed07f101458fe42b8f453ef3a2536f6bce8a7ae64f222f5688ada2500");
        Path jar=Path.of("target/harness/extension.jar"); Files.createDirectories(jar.getParent());
        try(var out=new JarOutputStream(Files.newOutputStream(jar))) {
            add(out,Path.of("target/classes"),false);
            add(out,Path.of("target/test-classes"),true);
            out.putNextEntry(new JarEntry("META-INF/services/io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule"));
            out.write("ai.mutuus.common.agent.transport.JdkTransportModule\nai.mutuus.common.agent.transport.UrlTransportModule\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();
            out.putNextEntry(new JarEntry("META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider"));
            out.write((LineageAutoConfigurationCustomizerProvider.class.getName()+"\n"+TrustedDestinationPropagationCustomizer.class.getName()+"\n"+HarnessExporter.class.getName()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();
        }
        Path log=Path.of("target/harness",logName);
        boolean spring=main.equals(SpringTransportContractMain.class.getName()) || main.equals(SpringResidualContractMain.class.getName()) || main.equals(SpringDeniedRetryMain.class.getName());
        boolean transport=spring || main.equals(SpringJdkContractMain.class.getName()) || main.equals(TransportAttemptContractMain.class.getName())
            || main.equals(ResidualHeaderRedirectMain.class.getName())
            || main.equals(Http2TransportContractMain.class.getName()) || main.equals(Http2DeniedRetryMain.class.getName());
        var processBuilder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),
            "--add-exports=java.base/sun.net.www=ALL-UNNAMED",
            "-javaagent:"+Path.of("target/harness/opentelemetry-javaagent-2.28.0.jar").toAbsolutePath(),
            "-Dotel.javaagent.extensions="+jar.toAbsolutePath(),"-Dotel.service.name=common-c3-local-harness",
            "-Dotel.javaagent.experimental.indy="+transport,"-Dotel.instrumentation.java-http-client.enabled="+!transport,
            "-Dotel.instrumentation.mutuus-jdk-transport.enabled="+(transport && !spring),
            "-Dotel.instrumentation.http-url-connection.enabled="+!spring,
            "-Dotel.instrumentation.mutuus-url-transport.enabled="+spring,
            "-Dotel.traces.exporter=none","-Dotel.metrics.exporter=none","-Dotel.logs.exporter=none",
            "-Dotel.traces.sampler=parentbased_traceidratio","-Dotel.traces.sampler.arg=1.0","-Dotel.propagators=tracecontext,baggage",
            "-Dmutuus.common.observability.propagation.allowed-hosts=localhost",
            "-Dmutuus.common.observability.propagation.external-enabled=false",
            "-cp",System.getProperty("surefire.test.class.path"),main)
            .redirectErrorStream(true).redirectOutput(log.toFile());
        processBuilder.environment().put("PYTHONPATH",Path.of("target/h2-python").toAbsolutePath().toString());
        var process=processBuilder.start();
        boolean ended=process.waitFor(45,TimeUnit.SECONDS);
        if(!ended)process.destroyForcibly();
        assertThat(ended).as(Files.readString(log)).isTrue();
        assertThat(process.exitValue()).as(Files.readString(log)).isZero();
        assertThat(Files.readString(log)).contains(marker);
    }
    static void add(JarOutputStream out,Path root,boolean fixtureOnly) throws Exception {
        try(var paths=Files.walk(root)) {
            for(Path p:paths.filter(Files::isRegularFile).toList()) {
                String name=root.relativize(p).toString();
                if(!name.endsWith(".class") || (fixtureOnly && !name.contains("/HarnessExporter")))continue;
                out.putNextEntry(new JarEntry(name));Files.copy(p,out);out.closeEntry();
            }
        }
    }
}
