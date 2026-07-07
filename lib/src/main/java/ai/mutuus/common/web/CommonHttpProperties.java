package ai.mutuus.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 아웃바운드 HTTP 클라이언트 공통 설정. {@code mutuus.common.http.*}.
 * <p>타임아웃은 Boot 표준 {@code spring.http.client.*}(라이브러리가 컨벤션 기본값을 EPP 로 주입)로 관리하고,
 * 여기서는 <b>재시도</b>만 다룬다(기본 OFF — 재시도는 멱등성/중복 위험이 있어 명시적 opt-in).
 */
@ConfigurationProperties(prefix = "mutuus.common.http")
public class CommonHttpProperties {

    private final Retry retry = new Retry();

    /** 아웃바운드 호출 로깅({@code mutuus.common.http.client-logging.*}). */
    private final ClientLogging clientLogging = new ClientLogging();

    public Retry getRetry() {
        return retry;
    }

    public ClientLogging getClientLogging() {
        return clientLogging;
    }

    /**
     * 아웃바운드 호출 완료 로깅 설정. 하위 서비스 호출(RestClient/RestTemplate)마다 {@code http.client.completed}
     * 이벤트를 {@code mutuus.http_client} dataset 으로 남긴다(e2e 호출 구간 관측). 기본 ON.
     */
    public static class ClientLogging {

        /** 아웃바운드 호출 로깅 활성화(기본 true). 소음이 크면 false 로 끄거나 로거 레벨로 제어. */
        private boolean enabled = true;

        /** 느린 호출 경고 임계값(ms). 0이면 비활성. 초과 시 완료 로그가 WARN 으로 기록된다. */
        private long slowRequestThresholdMillis = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getSlowRequestThresholdMillis() {
            return slowRequestThresholdMillis;
        }

        public void setSlowRequestThresholdMillis(long slowRequestThresholdMillis) {
            this.slowRequestThresholdMillis = slowRequestThresholdMillis;
        }
    }

    public static class Retry {

        /** 아웃바운드 재시도 활성화(기본 false). 멱등 메서드(GET/HEAD/OPTIONS)의 IOException 만 재시도. */
        private boolean enabled = false;

        /** 최대 시도 횟수(초기 시도 포함). 예: 2 = 최초 1회 + 재시도 1회. */
        private int maxAttempts = 2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
