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

    public Retry getRetry() {
        return retry;
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
