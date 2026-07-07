package ai.mutuus.sample.board.jooq;

import org.jooq.conf.RenderQuotedNames;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQ 렌더링 설정. PostgreSQL 표준인 소문자 snake_case 스키마를 기준으로, 생성 코드의 식별자를
 * 따옴표 없이 렌더({@link RenderQuotedNames#NEVER})한다. 샘플 스키마는 예약어 컬럼을 쓰지 않고 Flyway
 * 마이그레이션도 소문자 이름을 사용하므로, quoted identifier에 묶이지 않는 쪽이 운영 SQL과 더 가깝다.
 *
 * <p>Boot 의 jOOQ 자동구성 훅({@link DefaultConfigurationCustomizer})으로 적용하며, 이 앱의 jOOQ 는 board
 * 전용이라 영향 범위가 board 컨텍스트로 한정된다.
 */
@Configuration
public class BoardJooqConfig {

    @Bean
    DefaultConfigurationCustomizer boardJooqConfigurationCustomizer() {
        return config -> config.settings().setRenderQuotedNames(RenderQuotedNames.NEVER);
    }
}
