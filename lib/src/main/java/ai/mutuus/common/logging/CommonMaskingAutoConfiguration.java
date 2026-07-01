package ai.mutuus.common.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ai.mutuus.common.core.SensitiveDataMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 로그 민감정보 마스킹 자동 구성(<b>기본 OFF</b>). {@code mutuus.common.logging.masking.enabled=true} 일 때만
 * {@link SensitiveDataMasker} 빈을 등록한다. payload/method 로거가 이 빈이 있으면 본문/인자/리턴을 마스킹한다
 * (없으면 원문). 소비자가 자체 masker 를 정의하면 {@code @ConditionalOnMissingBean} 으로 비켜선다.
 */
@AutoConfiguration
@EnableConfigurationProperties(MaskingProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.logging.masking", name = "enabled", havingValue = "true")
public class CommonMaskingAutoConfiguration {

    /** 기본 내장 패턴: 카드번호(13~16 연속숫자)·주민등록번호(6+7). + 사용자 지정 패턴. */
    @Bean
    @ConditionalOnMissingBean
    public SensitiveDataMasker sensitiveDataMasker(MaskingProperties props) {
        List<Pattern> patterns = new ArrayList<>();
        if (props.isUseDefaultPatterns()) {
            patterns.add(Pattern.compile("\\d{13,16}"));       // 카드번호
            patterns.add(Pattern.compile("\\d{6}[-]?\\d{7}")); // 주민등록번호
        }
        for (String p : props.getPatterns()) {
            patterns.add(Pattern.compile(p));
        }
        return new SensitiveDataMasker(patterns);
    }
}
