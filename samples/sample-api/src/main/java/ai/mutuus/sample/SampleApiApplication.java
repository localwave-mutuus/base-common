package ai.mutuus.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * common-platform 라이브러리를 의존성으로만 추가한 소비 서비스.
 * <p>별도 설정 없이 자동구성(추적/예외/i18n/보안)이 활성화되는지 통합 테스트로 검증한다.
 */
@SpringBootApplication
public class SampleApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApiApplication.class, args);
    }
}
