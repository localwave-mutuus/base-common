package ai.mutuus.common.intercept;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 컨트롤러 메서드 진입 인터셉트 설정. {@code mutuus.common.controller-entry.*}. 기본 비활성(opt-in).
 * <p>대상은 <b>패키지 / 메서드명 / URL</b> 세 축으로 지정한다. 비어 있는 축은 제약하지 않으며,
 * 지정된 축은 그 중 하나라도 맞으면 통과(축 간 AND, 축 내 OR). 셋 다 비우면 모든 컨트롤러가 대상.
 */
@ConfigurationProperties(prefix = "mutuus.common.controller-entry")
public class ControllerEntryProperties {

    /** 진입 인터셉트 활성화 여부(기본 false). */
    private boolean enabled = false;

    /** 대상 패키지 접두사(예: {@code ai.mutuus.sample.web}). 컨트롤러 클래스의 패키지로 매칭. */
    private List<String> packages = new ArrayList<>();

    /** 대상 메서드명 패턴(와일드카드 {@code *} 지원, 예: {@code get*}, {@code audit}). */
    private List<String> methodNames = new ArrayList<>();

    /** 대상 URL Ant 패턴(예: {@code /api/**}, {@code /demo/audit}). */
    private List<String> urlPatterns = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }
}
