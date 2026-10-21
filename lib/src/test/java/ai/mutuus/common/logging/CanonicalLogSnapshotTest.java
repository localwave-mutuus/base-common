package ai.mutuus.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CanonicalLogSnapshotTest {
    private Map<String, Object> capture(String code, Object marker) {
        var event = new LoggingEvent();
        event.setLoggerName(ActionLogger.LOGGER_NAME);
        event.setLevel(Level.WARN);
        event.setTimeStamp(System.currentTimeMillis());
        event.setMessage("action");
        event.setMDCPropertyMap(Map.of());
        event.addKeyValuePair(new KeyValuePair("event.dataset", "mutuus.action"));
        event.addKeyValuePair(new KeyValuePair("error.code", code));
        if (marker != null) event.addKeyValuePair(new KeyValuePair(ActionLogger.VALIDATED_ACTION_KEY, marker));
        return CanonicalLogSnapshot.capture(event);
    }

    @Test void validatedActionCodesAndRollbackArePreserved() {
        for (String code : new String[]{"A", "OWNER_REJECTED", "TRANSACTION_ROLLED_BACK", "A".repeat(80)}) {
            var snapshot = capture(code, ActionLogger.VALIDATED_ACTION);
            assertEquals(code, snapshot.get("error.code"));
            assertFalse(snapshot.containsKey(ActionLogger.VALIDATED_ACTION_KEY));
        }
    }

    @Test void invalidCodesAreBlockedEvenOnValidatedActions() {
        for (String code : new String[]{"owner_rejected", "OWNER REJECTED", "private@example.invalid", "A".repeat(81)})
            assertEquals("OPERATION_FAILED", capture(code, ActionLogger.VALIDATED_ACTION).get("error.code"));
    }

    @Test void ordinaryLogsAndForgedMarkersCannotEnableActionCodes() {
        for (Object marker : new Object[]{null, Boolean.TRUE, new Object()})
            for (String code : new String[]{"OWNER_REJECTED", "TRANSACTION_ROLLED_BACK", "owner_rejected", "OWNER REJECTED", "private@example.invalid"})
                assertEquals("OPERATION_FAILED", capture(code, marker).get("error.code"));
        assertEquals("HTTP_500", capture("HTTP_500", null).get("error.code"));
    }
}
