package ai.mutuus.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * common-platform 을 의존성으로만 추가한 <b>비(非)웹</b> 소비 서비스.
 * <p>web/security starter 가 없으므로 라이브러리의 web/security 자동구성은 비활성이고
 * (optional 의존성 비전이 — 설계의 핵심 원칙), core/config/i18n/async 등 비웹 기능만
 * 활성화된다. 통합 테스트가 이를 검증한다.
 */
@SpringBootApplication
public class SampleBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleBatchApplication.class, args);
    }
}
