package ai.mutuus.common.logging;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그 민감정보 마스킹 설정. {@code mutuus.common.logging.masking.*}. <b>기본 OFF(opt-in)</b>.
 * <p>켜면 payload/method 로깅이 본문·인자·리턴을 로그에 남기기 전에 카드/주민번호 등 PII 를 마스킹한다.
 */
@ConfigurationProperties(prefix = "mutuus.common.logging.masking")
public class MaskingProperties {

    /** 마스킹 활성화 여부(기본 false). */
    private boolean enabled = false;

    /** 기본 내장 패턴(카드번호·주민등록번호) 사용 여부. */
    private boolean useDefaultPatterns = true;

    /** 추가 정규식 패턴(마지막 4자만 남기고 마스킹). */
    private List<String> patterns = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseDefaultPatterns() {
        return useDefaultPatterns;
    }

    public void setUseDefaultPatterns(boolean useDefaultPatterns) {
        this.useDefaultPatterns = useDefaultPatterns;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }
}
