package ai.mutuus.common.web;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.LogFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 아웃바운드 호출(RestClient/RestTemplate) 완료를 로깅해 <b>서비스 간 호출 구간(e2e)</b>을 관측한다.
 * <p>inbound access 로그만으로는 "서비스 A가 서비스 B를 부른 구간"이 안 보이므로, 각 하위 호출마다
 * {@code http.client.completed} 이벤트를 {@code mutuus.http_client} dataset 으로 남긴다. 같은 요청에서
 * 파생된 호출은 {@code trace.id}(MDC) 로 access·error 로그와 함께 묶인다. span 은 호출 구간마다 새로 발급된다
 * ({@link HeaderPropagationInterceptor}).
 *
 * <p><b>보안</b>: URL 은 쿼리스트링을 제외하고 {@code url.full}(scheme://host[:port]/path) 로만 남긴다
 * (쿼리에 담길 수 있는 토큰/비밀 유출 방지). 본문은 남기지 않는다.
 * <p><b>레벨</b>: 실패(예외/5xx)·느린 호출은 WARN, 그 외 INFO. {@link LogFormat} 에 따라 legacy/ECS/dual.
 */
public class HttpClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    public static final String LOGGER_NAME = "ai.mutuus.common.http.client";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private static final List<String> CAT_NETWORK = List.of("network");
    private static final List<String> TYPE_CONNECTION = List.of("connection");

    private final LogFormat format;
    private final long slowThresholdMillis;

    public HttpClientLoggingInterceptor() {
        this(LogFormat.DUAL, 0);
    }

    public HttpClientLoggingInterceptor(LogFormat format, long slowThresholdMillis) {
        this.format = format == null ? LogFormat.DUAL : format;
        this.slowThresholdMillis = slowThresholdMillis;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String method = request.getMethod().name();
        URI uri = request.getURI();
        long start = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            long nanos = System.nanoTime() - start;
            int status = statusOf(response);
            write(method, uri, status, nanos, null);
            return response;
        } catch (IOException | RuntimeException ex) {
            long nanos = System.nanoTime() - start;
            write(method, uri, -1, nanos, ex);
            throw ex;
        }
    }

    private void write(String method, URI uri, int status, long nanos, Throwable ex) {
        long durationMs = nanos / 1_000_000L;
        boolean slow = slowThresholdMillis > 0 && durationMs >= slowThresholdMillis;
        boolean failure = ex != null || status >= 500;
        LoggingEventBuilder ev = (failure || slow) ? log.atWarn() : log.atInfo();

        String urlNoQuery = sanitize(uri);
        if (format.legacy()) {
            ev.addKeyValue("event", "http.client.completed")
                    .addKeyValue("httpMethod", method)
                    .addKeyValue("url", urlNoQuery)
                    .addKeyValue("durationMs", durationMs);
            if (status > 0) {
                ev.addKeyValue("httpStatus", status);
            }
            if (slow) {
                ev.addKeyValue("slow", true);
            }
            if (ex != null) {
                ev.addKeyValue("exception", ex.getClass().getName());
            }
        }
        if (format.ecs()) {
            ev.addKeyValue(EcsFields.EVENT_DATASET, EcsFields.DATASET_HTTP_CLIENT)
                    .addKeyValue(EcsFields.DATA_STREAM_DATASET, EcsFields.DATASET_HTTP_CLIENT)
                    .addKeyValue(EcsFields.EVENT_ACTION, "http.client.completed")
                    .addKeyValue(EcsFields.EVENT_CATEGORY, CAT_NETWORK)
                    .addKeyValue(EcsFields.EVENT_TYPE, TYPE_CONNECTION)
                    .addKeyValue(EcsFields.EVENT_OUTCOME,
                            failure ? EcsFields.OUTCOME_FAILURE : EcsFields.OUTCOME_SUCCESS)
                    .addKeyValue(EcsFields.HTTP_REQUEST_METHOD, method)
                    .addKeyValue(EcsFields.URL_FULL, urlNoQuery)
                    .addKeyValue(EcsFields.EVENT_DURATION, nanos);
            if (uri.getHost() != null) {
                ev.addKeyValue(EcsFields.URL_DOMAIN, uri.getHost());
            }
            if (status > 0) {
                ev.addKeyValue(EcsFields.HTTP_RESPONSE_STATUS_CODE, status);
            }
            if (ex != null) {
                ev.addKeyValue(EcsFields.ERROR_TYPE, ex.getClass().getName());
            }
        }
        ev.log("outbound HTTP call completed");
    }

    /** 응답 상태코드 추출(스트림 접근 실패 시 -1). */
    private static int statusOf(ClientHttpResponse response) {
        try {
            return response.getStatusCode().value();
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    /** 쿼리스트링을 제외한 URL(scheme://host[:port]/path) — 쿼리에 담길 수 있는 비밀 유출 방지. */
    private static String sanitize(URI uri) {
        StringBuilder sb = new StringBuilder();
        if (uri.getScheme() != null) {
            sb.append(uri.getScheme()).append("://");
        }
        if (uri.getHost() != null) {
            sb.append(uri.getHost());
            if (uri.getPort() > 0) {
                sb.append(':').append(uri.getPort());
            }
        }
        if (uri.getRawPath() != null) {
            sb.append(uri.getRawPath());
        }
        return sb.length() == 0 ? String.valueOf(uri) : sb.toString();
    }
}
