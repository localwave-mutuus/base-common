package ai.mutuus.sample;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import ai.mutuus.common.logging.AccessLogger;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import static org.assertj.core.api.Assertions.assertThat;

/** 외부 DB/IdP 없이 실제 Tomcat·Spring Security·소켓 RST를 한 번씩 통과한다. */
@SpringBootTest(classes = HttpPairLoggingIntegrationTest.App.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.config.name=pair-test", "spring.application.name=pair-test",
        "mutuus.common.service-short-name=app", "mutuus.common.logging.environment=local",
        "mutuus.common.session.enabled=false", "spring.main.banner-mode=off"})
class HttpPairLoggingIntegrationTest {
    @LocalServerPort int port;
    static final CountDownLatch entered = new CountDownLatch(1), disconnected = new CountDownLatch(1);

    @Test void 아홉_종료경로가_실제_소비자에서_각각_한쌍이다() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogger.LOGGER_NAME);
        List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
        AppenderBase<ILoggingEvent> capture = new AppenderBase<>() {
            @Override protected void append(ILoggingEvent event) { event.prepareForDeferredProcessing(); events.add(event); }
        };
        capture.setContext(logger.getLoggerContext()); capture.start(); logger.addAppender(capture);
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            int[] expected = {200, 400, 401, 403, 500, 500, 200, 500};
            String[] paths = {"ok", "bad", "auth", "denied", "server", "exception", "async", "timeout"};
            for (int i = 0; i < paths.length; i++) {
                var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/pair/" + paths[i])).timeout(Duration.ofSeconds(5));
                if (paths[i].equals("denied")) builder.header("Authorization", "Basic dXNlcjpwYXNz");
                assertThat(client.send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode()).as(paths[i]).isEqualTo(expected[i]);
            }
            // 서버가 요청에 진입한 뒤 RST. IOException 발생을 실제 서버에서 관찰한다.
            try (Socket socket = new Socket("127.0.0.1", port)) {
                socket.getOutputStream().write(("GET /pair/disconnect HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush(); assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
                socket.setSoLinger(true, 0);
            }
            disconnected.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (pairs(events).size() < 18 && System.nanoTime() < deadline) Thread.sleep(10);
            var pairEvents = pairs(events);
            assertThat(pairEvents).hasSize(18);
            for (String path : new String[]{"ok", "bad", "auth", "denied", "server", "exception", "async", "timeout", "disconnect"}) {
                var pair = pairEvents.stream().filter(e -> ("/pair/" + path).equals(value(e, "url.path"))).toList();
                assertThat(pair).as(path).hasSize(2);
                assertThat(value(pair.get(0), "event.action")).isEqualTo("http.request.in");
                assertThat(value(pair.get(1), "event.action")).isEqualTo("http.response.out");
                assertThat(value(pair.get(0), "requestId")).isEqualTo(value(pair.get(1), "requestId"));
                assertThat(value(pair.get(0), "trace.id")).isEqualTo(value(pair.get(1), "trace.id"));
            }
            assertThat(pairEvents.stream().filter(e -> "ASYNC_TIMEOUT".equals(value(e, "errorCode"))).count()).isEqualTo(1);
            assertThat(pairEvents.stream().filter(e -> "CLIENT_DISCONNECT".equals(value(e, "errorCode"))).count()).isEqualTo(1);
            LogstashEncoder encoder = new LogstashEncoder(); encoder.setContext(logger.getLoggerContext()); encoder.start();
            Path output = Path.of("target/wave18/http-pairs.jsonl"); Files.createDirectories(output.getParent());
            try (var stream = Files.newOutputStream(output)) { for (var event : pairEvents) stream.write(encoder.encode(event)); }
            encoder.stop();
        } finally { logger.detachAppender(capture); capture.stop(); disconnected.countDown(); }
    }
    static List<ILoggingEvent> pairs(List<ILoggingEvent> events) {
        return events.stream().filter(e -> List.of("http.request.in", "http.response.out").contains(value(e, "event.action"))).toList();
    }
    static Object value(ILoggingEvent event, String key) {
        return event.getKeyValuePairs().stream().filter(p -> p.key.equals(key)).map(p -> p.value).findFirst().orElse(null);
    }
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "ai.mutuus.common.persistence.JpaAuditingAutoConfiguration",
        "ai.mutuus.common.exception.CommonExceptionAutoConfiguration",
        "ai.mutuus.common.response.CommonResponseWrapperAutoConfiguration"})
    static class App {
        @Bean UserDetailsService users() { return new InMemoryUserDetailsManager(User.withUsername("user").password("{noop}pass").roles("USER").build()); }
        @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
            return http.csrf(c -> c.disable()).authorizeHttpRequests(a -> a
                    .requestMatchers("/pair/auth").authenticated().requestMatchers("/pair/denied").hasRole("ADMIN")
                    .anyRequest().permitAll()).httpBasic(b -> {}).build();
        }
        @Bean ServletRegistrationBean<HttpServlet> pairServlet() {
            var bean = new ServletRegistrationBean<HttpServlet>(new HttpServlet() {
                @Override protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
                    switch (req.getRequestURI()) {
                        case "/pair/bad" -> res.setStatus(400);
                        case "/pair/server" -> res.setStatus(500);
                        case "/pair/exception" -> throw new ServletException("synthetic");
                        case "/pair/async" -> { var context = req.startAsync(); context.start(context::complete); }
                        case "/pair/timeout" -> req.startAsync().setTimeout(50);
                        case "/pair/disconnect" -> {
                            entered.countDown();
                            try { if (!disconnected.await(3, TimeUnit.SECONDS)) throw new ServletException("RST not sent"); }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ServletException(e); }
                            for (int i = 0; i < 512; i++) { res.getOutputStream().write(new byte[8192]); res.flushBuffer(); }
                        }
                        default -> res.setStatus(200);
                    }
                }
            }, "/pair/*"); bean.setAsyncSupported(true); return bean;
        }
    }
}
