package ai.mutuus.common.logging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.Status;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 Joran include, 단일 fanout, 비동기 파일 종료까지 두 공개 리소스 경로를 소비자에서 검증한다. */
class OtelLogbackResourceContractTest {
    @TempDir
    Path temp;

    @Test
    void 정본과_호환_alias는_각각_순환없이_한개의_파일이벤트를_쓴다() throws Exception {
        for (String resource : List.of("common-platform/logback/common.xml", "logback-common.xml")) {
            Path directory = Files.createDirectory(temp.resolve(resource.startsWith("common-platform") ? "canonical" : "alias"));
            LoggerContext context = new LoggerContext();
            context.setName("resource-contract-" + directory.getFileName());
            context.setMDCAdapter(new LogbackMDCAdapter());
            context.putProperty("mutuus.logDirectory", directory.toString());
            context.putProperty("mutuus.logFileBase", "resource-contract");
            context.putProperty("mutuus.logRollInterval", "PT1H");
            context.putProperty("mutuus.logMaxFileSize", "100MB");
            context.start();
            try {
                String wrapper = "<configuration><include resource=\"" + resource + "\"/></configuration>";
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                configurator.doConfigure(new ByteArrayInputStream(wrapper.getBytes(StandardCharsets.UTF_8)));
                Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
                List<Appender<ILoggingEvent>> roots = new ArrayList<>();
                root.iteratorForAppenders().forEachRemaining(roots::add);
                assertThat(roots).hasSize(1);
                assertThat(roots.getFirst()).isInstanceOf(SnapshotAppender.class);
                SnapshotAppender snapshot = (SnapshotAppender) roots.getFirst();
                assertThat(snapshot.isStarted()).isTrue();
                List<Appender<ILoggingEvent>> delegates = new ArrayList<>();
                snapshot.iteratorForAppenders().forEachRemaining(delegates::add);
                assertThat(delegates).extracting(Appender::getName)
                        .containsExactlyInAnyOrder("JSON_CONSOLE", "ASYNC_JSON_FILE");
                assertThat(snapshot.getAppender("JSON_CONSOLE")).isInstanceOf(ConsoleAppender.class);
                var async = (AsyncAppender) snapshot.getAppender("ASYNC_JSON_FILE");
                List<Appender<ILoggingEvent>> files = new ArrayList<>();
                async.iteratorForAppenders().forEachRemaining(files::add);
                assertThat(files).hasSize(1);
                assertThat(files.getFirst()).isInstanceOf(ClosingRollingFileAppender.class);
                var file = (ClosingRollingFileAppender<?>) files.getFirst();
                assertThat(file.getEncoder()).isInstanceOf(CommonJsonEncoder.class);
                assertThat(file.isStarted()).isTrue();
                context.getLogger("contract.resource").info("resource-contract-event");
            } finally {
                // AsyncAppender maxFlushTime=0과 최종 파일 공개 경로를 실제로 통과한다.
                context.stop();
            }
            assertThat(context.getStatusManager().getCopyOfStatusList().stream()
                    .filter(status -> status.getLevel() >= Status.ERROR).map(Status::toString).toList()).isEmpty();
            List<String> lines = new ArrayList<>();
            try (var paths = Files.list(directory)) {
                for (Path path : paths.filter(path -> path.getFileName().toString().endsWith(".log")
                        || path.getFileName().toString().endsWith(".log.gz")).toList()) {
                    try (InputStream raw = Files.newInputStream(path);
                         InputStream input = path.toString().endsWith(".gz") ? new GZIPInputStream(raw) : raw) {
                        new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                                .filter(line -> !line.isBlank()).forEach(lines::add);
                    }
                }
            }
            assertThat(lines).hasSize(1);
            Map<String, String> fields = new LinkedHashMap<>();
            try (JsonParser parser = new JsonFactory().createParser(lines.getFirst())) {
                assertThat(parser.nextToken()).isEqualTo(JsonToken.START_OBJECT);
                flatten(parser, "", fields);
                assertThat(parser.nextToken()).isNull();
            }
            assertThat(fields).containsEntry("message", "resource-contract-event")
                    .containsEntry("log.level", "info").containsEntry("log.logger", "contract.resource")
                    .containsEntry("golmok.auth.state", "anonymous");
            assertThat(fields.get("@timestamp")).isNotBlank();
            assertThat(UUID.fromString(fields.get("event.id")).toString()).isEqualTo(fields.get("event.id"));
            assertThat(fields).doesNotContainKeys("logger_name", "level_value", "transaction.id");
        }
    }

    private static void flatten(JsonParser parser, String prefix, Map<String, String> fields) throws Exception {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            assertThat(parser.currentToken()).isEqualTo(JsonToken.FIELD_NAME);
            String name = prefix + parser.currentName();
            JsonToken value = parser.nextToken();
            if (value == JsonToken.START_OBJECT) flatten(parser, name + ".", fields);
            else if (value.isScalarValue()) {
                assertThat(fields).doesNotContainKey(name);
                fields.put(name, parser.getValueAsString());
            } else parser.skipChildren();
        }
    }
}