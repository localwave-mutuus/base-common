package ai.mutuus.common.aop;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 컨트롤러 메서드 인자/리턴 로깅(AOP) 설정. {@code mutuus.common.method-logging.*}. 기본 비활성(opt-in).
 * <p>대상은 패키지/메서드명으로 좁힐 수 있다(비우면 모든 {@code @RestController} 메서드).
 */
@ConfigurationProperties(prefix = "mutuus.common.method-logging")
public class MethodLoggingProperties {

    /** 메서드 인자/리턴 로깅 활성화 여부(기본 false). */
    private boolean enabled = false;

    /** 인자/리턴 문자열 최대 길이(초과분 절단). */
    private int maxLength = 1000;

    /** 대상 패키지 접두사(예: {@code ai.mutuus.sample.web}). 비우면 제약 없음. */
    private List<String> packages = new ArrayList<>();

    /** 대상 메서드명 패턴(와일드카드 {@code *} 지원). 비우면 제약 없음. */
    private List<String> methodNames = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public List<String> getMethodNames() {
        return methodNames;
    }

    public void setMethodNames(List<String> methodNames) {
        this.methodNames = methodNames;
    }
}
