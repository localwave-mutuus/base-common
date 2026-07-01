package ai.mutuus.sample;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * <b>api/web 요청 유입 시 전체 로깅 구간</b>을 케이스별로 실제 트리거해, 각 케이스에서 남는
 * 로그 이벤트를 <b>실제 호출 순서대로</b> 수집·출력한다(케이스명 - 순서 - 로그증적 - 발생 로거).
 * <p>네 로거({@code ai.mutuus.common.access/payload/controller/method})는 모두 부모 로거
 * {@code ai.mutuus.common} 의 자식이라, 부모에 ListAppender 하나만 붙이면 additivity 로 네 로거의
 * 이벤트가 <b>한 리스트에 실제 발생 순서대로</b> 모인다. 평소 OFF 인 payload/controller-entry/method-logging
 * 을 모두 켜서 최대 구간을 관찰한다.
 */
@SpringBootTest(properties = {
        "mutuus.common.payload-logging.enabled=true",   // 입출력 본문 로깅 ON
        "mutuus.common.controller-entry.enabled=true",  // 컨트롤러 진입 인터셉트 ON (URL 제약 없음 = 전 컨트롤러)
        "mutuus.common.method-logging.enabled=true",    // 메서드 인자/리턴 AOP 로깅 ON (test 스코프 aop 필요)
        "mutuus.common.logging.slow-request-threshold-millis=1000",
        "mutuus.sample.mock-jwt=true"                   // 데모 JwtDecoder: 토큰문자열 = subject
})
@AutoConfigureMockMvc
class LoggingCaseMatrixIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private ListAppender<ILoggingEvent> appender;
    private Logger parent;

    @BeforeEach
    void attach() {
        parent = (Logger) LoggerFactory.getLogger("ai.mutuus.common");
        appender = new ListAppender<>();
        appender.start();
        parent.addAppender(appender);
    }

    @AfterEach
    void detach() {
        parent.detachAppender(appender);
    }

    @Test
    void 정상_GET_공개엔드포인트() throws Exception {
        List<String> order = run("C1  정상 GET (공개, 본문 없음)", get("/api/public/hello"));
        // 정상 경로 7건 + 계층 순서(엔트리 인터셉터 → AOP 진입 → AOP 종료) 회귀 가드.
        // method.enter/exit 가 있어야 method-logging 자동구성이 imports 에 등록돼 활성화됐음을 e2e 로 보장한다.
        assertThat(order).containsSubsequence(
                "request.received", "request.payload", "controller.entry",
                "method.enter", "method.exit", "response.payload", "request.completed");
    }

    @Test
    void 정상_POST_본문있음() throws Exception {
        List<String> order = run("C2  정상 POST (요청/응답 본문 있음)",
                post("/api/public/echo").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"));
        assertThat(order).containsSubsequence("controller.entry", "method.enter", "method.exit");
    }

    @Test
    void 비즈니스예외_404() throws Exception {
        run("C3  비즈니스 예외 (404, error.business WARN)", get("/api/public/boom"));
    }

    @Test
    void 검증실패_400() throws Exception {
        List<String> order = run("C4  @Valid 검증 실패 (400, error.business WARN)",
                post("/api/public/echo").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"\"}"));
        // 검증은 인자 결정 단계에서 실패 → controller.entry 는 남고 method.enter 는 남지 않는다.
        assertThat(order).contains("controller.entry", "error.business").doesNotContain("method.enter");
    }

    @Test
    void 잘못된포맷_400() throws Exception {
        run("C5  잘못된 JSON 포맷 (400 MALFORMED, error.business WARN)",
                post("/api/public/echo").contentType(MediaType.APPLICATION_JSON).content("{bad json"));
    }

    @Test
    void 서버예외_500() throws Exception {
        run("C6  미처리 서버 예외 (500, error.server ERROR)", get("/demo/error/server"));
    }

    @Test
    void 네트워크_502() throws Exception {
        run("C7  아웃바운드 네트워크 실패 (502, error.server ERROR)", get("/demo/error/network"));
    }

    @Test
    void 인증실패_401() throws Exception {
        List<String> order = run("C8  미인증 접근 (401, auth.failure WARN)", get("/api/secure/me"));
        // 보안이 DispatcherServlet 전에 차단 → 컨트롤러/메서드 계층 로그 전무, auth.failure 는 남는다.
        assertThat(order).contains("auth.failure").doesNotContain("controller.entry", "method.enter");
    }

    @Test
    void 인증성공_200() throws Exception {
        run("C9  인증 성공 (200, userId=주체 반영)",
                get("/api/secure/me").header("Authorization", "Bearer alice"));
    }

    @Test
    void 느린요청_WARN() throws Exception {
        run("C10 느린 요청 (200, request.completed WARN)", get("/demo/logging/slow"));
    }

    @Test
    void 쿼리_단일_배열_파라미터() throws Exception {
        List<String> order = run("C11 쿼리 파라미터 (단일 q + 배열 ids)",
                get("/demo/params/query?q=hello&ids=a&ids=b"));
        // method.enter args 에 단일값(hello)과 배열([a, b])이 함께 남는다: args=[hello, [a, b]]
        assertThat(order).contains("method.enter");
        assertThat(kvOf("method.enter", "args")).contains("hello").contains("[a, b]");
        // 쿼리스트링은 request.received 의 httpQuery 로 남는다
        assertThat(kvOf("request.received", "httpQuery")).contains("q=hello").contains("ids=a");
    }

    @Test
    void 본문_단일_배열_파라미터() throws Exception {
        List<String> order = run("C12 본문 파라미터 (단일 tag + 배열 tags)",
                post("/demo/params/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tag\":\"primary\",\"tags\":[\"a\",\"b\",\"c\"]}"));
        assertThat(order).containsSubsequence("request.payload", "method.enter", "method.exit", "response.payload");
        // 요청 본문 payload 에 단일 필드 + 배열 필드가 남는다
        assertThat(kvOf("request.payload", "requestBody"))
                .contains("\"tag\":\"primary\"").contains("\"tags\":[\"a\",\"b\",\"c\"]");
        // 역직렬화된 메서드 인자: args=[ParamsRequest[tag=primary, tags=[a, b, c]]]
        assertThat(kvOf("method.enter", "args")).contains("tag=primary").contains("tags=[a, b, c]");
    }

    /** 마지막 실행에서 특정 event 의 필드값을 읽는다(케이스별 값 단언용). */
    private String kvOf(String event, String key) {
        return appender.list.stream()
                .filter(e -> event.equals(kv(e, "event")))
                .map(e -> kv(e, key))
                .filter(v -> v != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("이벤트/필드 없음: " + event + "." + key));
    }

    /**
     * 요청을 수행하고, 부모 로거에 모인 이벤트를 실제 순서대로 콘솔에 표로 출력한다.
     * @return 발생 순서대로의 event 이름 목록(케이스별 추가 단언용).
     */
    private List<String> run(String caseName, MockHttpServletRequestBuilder request) throws Exception {
        appender.list.clear();
        mockMvc.perform(request);

        List<ILoggingEvent> events = new ArrayList<>(appender.list).stream()
                .filter(e -> kv(e, "event") != null)   // 프레임워크 잡음 제외 — 우리 구조화 이벤트만
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══ LOGCASE ").append(caseName).append('\n');
        sb.append(String.format("║ %-3s %-9s %-6s %-18s %-8s %s%n",
                "#", "로거", "레벨", "event", "status", "주요필드"));
        int i = 1;
        for (ILoggingEvent e : events) {
            String logger = shortLogger(e.getLoggerName());
            String event = kv(e, "event");
            String level = e.getLevel().toString();
            String status = firstNonNull(kv(e, "httpStatus"), "");
            String extra = extra(e, event);
            sb.append(String.format("║ %-3d %-9s %-6s %-18s %-8s %s%n",
                    i++, logger, level, event, status, extra));
        }
        sb.append("╚══ END ").append(caseName).append(" (총 ").append(events.size()).append("건)\n");
        System.out.println(sb);

        // 모든 케이스는 최소한 수신/완료 액세스 로그를 남긴다(누락 회귀 가드)
        List<String> order = events.stream().map(e -> kv(e, "event")).toList();
        assertThat(order).contains("request.received", "request.completed");
        return order;
    }

    /** event 종류별로 대표 필드 하나를 골라 요약 표기. */
    private String extra(ILoggingEvent e, String event) {
        return switch (event) {
            case "request.received" -> "method=" + kv(e, "httpMethod") + " path=" + kv(e, "httpPath");
            case "request.completed" -> "durationMs=" + kv(e, "durationMs")
                    + (kv(e, "slow") != null ? " slow=true" : "")
                    + (kv(e, "userId") != null ? " userId=" + kv(e, "userId") : "");
            case "request.payload" -> "body=" + kv(e, "requestBody");
            case "response.payload" -> "body=" + kv(e, "responseBody");
            case "controller.entry" -> kv(e, "controller") + "#" + kv(e, "method");
            case "method.enter" -> kv(e, "class") + "#" + kv(e, "method") + " args=" + kv(e, "args");
            case "method.exit" -> kv(e, "class") + "#" + kv(e, "method") + " return=" + kv(e, "return");
            case "auth.failure", "auth.denied" -> "reason=" + kv(e, "reason");
            case "error.business" -> "errorCode=" + kv(e, "errorCode") + " detail=" + kv(e, "detail");
            case "error.server" -> "exception=" + kv(e, "exception");
            default -> "";
        };
    }

    private String shortLogger(String name) {
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private String kv(ILoggingEvent event, String key) {
        List<KeyValuePair> pairs = event.getKeyValuePairs();
        if (pairs == null) {
            return null;
        }
        for (KeyValuePair pair : pairs) {
            if (pair.key.equals(key)) {
                return String.valueOf(pair.value);
            }
        }
        return null;
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
