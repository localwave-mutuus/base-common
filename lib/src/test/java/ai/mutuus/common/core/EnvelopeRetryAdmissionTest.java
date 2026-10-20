package ai.mutuus.common.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.opentelemetry.api.*;
import ai.mutuus.common.logging.JobPairLogger;
import ch.qos.logback.classic.*;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;
class EnvelopeRetryAdmissionTest {
 @Test void reusedAttemptIsInvalidAndNoClaimNeverStartsExecutionPair(){
  C3TelemetryFixture.init();var ingress=GlobalOpenTelemetry.getTracer("test").spanBuilder("ingress").startSpan();String stored;
  try(var ignored=ingress.makeCurrent()){stored=TelemetryEnvelope.captureCurrentVerified("single","attempt").toStorageString();}finally{ingress.end();}
  try(var consumer=TelemetryEnvelope.resumeStored(stored,1,"batch","batch","attempt")){assertEquals("ENVELOPE_INVALID",consumer.reason());}
  var logger=(Logger)LoggerFactory.getLogger(JobPairLogger.LOGGER_NAME);var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
  int spans=C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size();
  try{var jobs=new JobPairLogger();jobs.admission("OFF");jobs.admission("NO_CLAIM");jobs.admission("CLAIM_FAILED");assertEquals(3,appender.list.size());for(var event:appender.list){assertTrue(event.getKeyValuePairs().stream().noneMatch(p->p.key.equals("execution.id")));assertTrue(event.getKeyValuePairs().stream().anyMatch(p->p.key.equals("event.action")&&p.value.equals("job.admission")));}assertEquals(spans,C3TelemetryFixture.EXPORTER.getFinishedSpanItems().size());}
  finally{logger.detachAppender(appender);appender.stop();}
 }
}
