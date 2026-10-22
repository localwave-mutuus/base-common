package ai.mutuus.common.logging;

import ai.mutuus.common.core.*;
import ai.mutuus.common.uicatalog.UiCodeAliases;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.assertThat;

class ScreenCodeCompatibilityTest {
    @Test void appScreenAndEventSurviveWithoutCatalogAlias() throws Exception {
        var json = log("APP", Map.of(HeaderNames.SCREEN_ID,"HOM-010",
                HeaderNames.BUSINESS_SCREEN_NO,"HOM-010", HeaderNames.INTERNAL_SCREEN_ID,"APP-HOME",
                HeaderNames.SCREEN_CATALOG_VERSION,"app-2026.10", HeaderNames.EVENT_ID,"abcdef0123456789"));
        assertThat(json.at("/golmok/screen/id").asText()).isEqualTo("HOM-010");
        assertThat(json.at("/golmok/screen/business_no").asText()).isEqualTo("HOM-010");
        assertThat(json.at("/golmok/screen/internal_id").asText()).isEqualTo("APP-HOME");
        assertThat(json.at("/golmok/screen/catalog_version").asText()).isEqualTo("app-2026.10");
        assertThat(json.at("/golmok/ui/event/id").asText()).isEqualTo("abcdef0123456789");
    }
    @Test void memberScreenSurvivesWithoutCatalogAlias() throws Exception {
        var json = log("MEM", Map.of(HeaderNames.SCREEN_ID,"MEM-MYP-010",
                HeaderNames.BUSINESS_SCREEN_NO,"MYP-010",HeaderNames.INTERNAL_SCREEN_ID,"member-home",
                HeaderNames.SCREEN_CATALOG_VERSION,"mem-1"));
        assertThat(json.at("/golmok/screen/id").asText()).isEqualTo("MEM-MYP-010");
        assertThat(json.at("/golmok/screen/business_no").asText()).isEqualTo("MYP-010");
        assertThat(json.at("/golmok/screen/internal_id").asText()).isEqualTo("member-home");
        assertThat(json.at("/golmok/screen/catalog_version").asText()).isEqualTo("mem-1");
    }
    @Test void bo066FieldsRemainIdenticalAndOriginalAliasIsAdded() throws Exception {
        // v0.6.6 ScreenMetadata resolves /i-01 to BO-ISS-010 with these six fields.
        var json = log("BO", Map.of(HeaderNames.SCREEN_ID,"/i-01",
                HeaderNames.SCREEN_CATALOG_VERSION,UiCodeAliases.CATALOG_VERSION,
                HeaderNames.BUSINESS_SCREEN_NO,"ISS-010",HeaderNames.UI_ELEMENT_ID,"BO-ISS-010-BTN-SEARCH",
                HeaderNames.UI_ACTION,"press",HeaderNames.EVENT_ID,"abcdef0123456789"));
        Map.of("/golmok/screen/id","BO-ISS-010", "/golmok/screen/catalog_version",UiCodeAliases.CATALOG_VERSION,
                "/golmok/screen/business_no","ISS-010", "/golmok/ui/element/id","BO-ISS-010-BTN-SEARCH",
                "/golmok/ui/action","press", "/golmok/ui/event/id","abcdef0123456789")
                .forEach((path,value) -> assertThat(json.at(path).asText()).isEqualTo(value));
        assertThat(json.at("/golmok/screen/alias").asText()).isEqualTo("/i-01");
    }
    @Test void eventOnlyNeedsValidNonzeroLowercaseHex() {
        for (String screen : new String[]{"/unknown?private", ""}) {
            var headers = new java.util.HashMap<String,String>(); headers.put(HeaderNames.SCREEN_ID,screen);
            headers.put(HeaderNames.EVENT_ID,"abcdef0123456789");
            assertThat(ScreenMetadata.fromHeaders("APP", headers::get)).containsEntry(HeaderNames.EVENT_ID,"abcdef0123456789");
            for (String invalid : new String[]{"0000000000000000","ABCDEF0123456789","abc","abcdef0123456789\n"}) {
                headers.put(HeaderNames.EVENT_ID,invalid);
                assertThat(ScreenMetadata.fromHeaders("APP", headers::get)).doesNotContainKey(HeaderNames.EVENT_ID);
            }
        }
    }
    private JsonNode log(String system, Map<String,String> headers) throws Exception {
        String previous = System.getProperty("mutuus.ui.system"); System.setProperty("mutuus.ui.system",system);
        Logger logger = (Logger)LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>(); capture.start();
        var attachment = TelemetryTestEvents.attach(logger,capture);
        var encoder = new CommonJsonEncoder(); encoder.setContext(logger.getLoggerContext()); encoder.start();
        try {
            var request = new MockHttpServletRequest("GET","/screen"); headers.forEach(request::addHeader);
            new AccessLogFilter(new AccessLogger(),new CommonLoggingProperties()).doFilter(request,new MockHttpServletResponse(),(req,res)->{});
            assertThat(capture.list).hasSize(1);
            return new ObjectMapper().readTree(encoder.encode(capture.list.getFirst()));
        } finally {
            encoder.stop(); attachment.close();
            if(previous==null)System.clearProperty("mutuus.ui.system");else System.setProperty("mutuus.ui.system",previous);
        }
    }
}
