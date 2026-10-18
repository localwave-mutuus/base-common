package ai.mutuus.common.payload;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayloadLoggingFilterTest {
    private final PayloadLoggingProperties props = new PayloadLoggingProperties();
    private final List<String> requests = new ArrayList<>();
    private final List<String> responses = new ArrayList<>();
    private final List<Integer> statuses = new ArrayList<>();
    private final PayloadLoggingFilter filter = new PayloadLoggingFilter(
            (request, body) -> requests.add(body),
            (request, status, body) -> { statuses.add(status); responses.add(body); }, props);

    @Test
    void 기본은_비활성이며_기존_ALL모드를_유지하고_4KiB상한을_강제한다() {
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getMode()).isEqualTo(PayloadLoggingProperties.Mode.ALL);
        assertThat(props.getMaxBodyBytes()).isEqualTo(4096);
        assertThatIllegalArgumentException().isThrownBy(() -> props.setMaxBodyBytes(4097));
        assertThatIllegalArgumentException().isThrownBy(() -> props.setMaxBodyBytes(0));
    }

    @Test
    void error_only는_성공본문을_기록하지_않고_응답을_보존한다() throws Exception {
        props.setMode(PayloadLoggingProperties.Mode.ERROR_ONLY);
        var response = new MockHttpServletResponse();
        filter.doFilter(request("입력"), response, (req, res) -> {
            assertThat(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("입력");
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("성공");
        });
        assertThat(requests).isEmpty();
        assertThat(responses).isEmpty();
        assertThat(response.getContentAsString()).isEqualTo("성공");
    }

    @Test
    void error_only는_4xx와5xx에서만_본문을_남기고_UTF8_상한을_지킨다() throws Exception {
        props.setMode(PayloadLoggingProperties.Mode.ERROR_ONLY);
        for (int status : List.of(400, 503)) {
            var response = new MockHttpServletResponse();
            String body = "가".repeat(2000);
            filter.doFilter(request(body), response, (req, res) -> {
                assertThat(req.getInputStream().readAllBytes()).hasSize(6000);
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(status);
                res.setContentType("text/plain;charset=UTF-8");
                res.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            });
            assertThat(response.getContentAsString()).isEqualTo(body);
        }
        assertThat(statuses).containsExactly(400, 503);
        assertThat(requests).allSatisfy(body -> assertThat(body.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(4096));
        assertThat(responses).allSatisfy(body -> assertThat(body.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(4096));
    }

    @Test
    void error_only는_전파되는_예외도_기록하고_예외를_유지한다() {
        props.setMode(PayloadLoggingProperties.Mode.ERROR_ONLY);
        var failure = new IllegalStateException("expected");
        assertThatThrownBy(() -> filter.doFilter(request("입력"), new MockHttpServletResponse(), (req, res) -> {
            req.getInputStream().readAllBytes();
            throw failure;
        })).isSameAs(failure);
        assertThat(requests).containsExactly("입력");
        assertThat(statuses).containsExactly(500);
    }

    @Test
    void ALL모드는_진입로그를_유지하면서_작은_바이트상한도_지킨다() throws Exception {
        props.setMaxBodyBytes(2);
        filter.doFilter(request("가나다"), new MockHttpServletResponse(), (req, res) -> {
            assertThat(requests).hasSize(1);
            assertThat(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("가나다");
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("가나다");
        });
        assertThat(requests.getFirst().getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(2);
        assertThat(responses.getFirst().getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(2);
    }

    @Test
    void error_only의_관측은_writer를_commit하지_않고_resetBuffer를_따른다() throws Exception {
        props.setMode(PayloadLoggingProperties.Mode.ERROR_ONLY);
        var response = new MockHttpServletResponse();
        filter.doFilter(request("입력"), response, (req, res) -> {
            res.setContentType("text/plain;charset=UTF-8");
            var writer = res.getWriter();
            writer.write("버릴본문");
            res.resetBuffer();
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(500);
            writer.write("최종본문");
        });
        assertThat(response.isCommitted()).isFalse();
        assertThat(response.getContentAsString()).isEqualTo("최종본문");
        assertThat(responses).containsExactly("최종본문");
    }

    @Test
    void 명시한_헤더만_남기고_인증과쿠키는_목록에_넣어도_제외한다() {
        props.setAllowedHeaders(List.of("X-Trace-Id", "Authorization", "cOoKiE", "Proxy-Authorization", "Set-Cookie"));
        var request = request("입력");
        request.addHeader("X-Trace-Id", "trace-ok");
        request.addHeader("X-Unlisted", "private");
        request.addHeader("Authorization", "Bearer private");
        request.addHeader("Cookie", "private");
        request.addHeader("Proxy-Authorization", "private");
        request.addHeader("Set-Cookie", "private");
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultRequestPayloadLogger.LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        try (var capture = ai.mutuus.common.logging.TelemetryTestEvents.attach(logger, appender)) {
            new DefaultRequestPayloadLogger(null, props).onRequest(request, "입력");
            var snapshot = ai.mutuus.common.logging.TelemetryTestEvents.fields(appender.list.getFirst());
            Object headers = snapshot.get("http.request.headers");
            assertThat(headers).isEqualTo(Map.of("X-Trace-Id", "trace-ok"));
            assertThat(snapshot.toString()).doesNotContain("Bearer private", "private", "Authorization", "Cookie");
        }
    }

    private MockHttpServletRequest request(String body) {
        var request = new MockHttpServletRequest("POST", "/api/orders");
        request.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
