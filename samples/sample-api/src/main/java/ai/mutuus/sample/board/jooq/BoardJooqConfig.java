package ai.mutuus.sample.board.jooq;

import org.jooq.conf.RenderQuotedNames;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQ 렌더링 설정. jooq-codegen 이 만든 {@code TableField}({@code BOARD_POST.TITLE})는 <b>따옴표+테이블 한정</b>
 * 으로 렌더된다({@code "board_post"."title"}). 그런데 H2 는 <b>UPDATE 의 SET 절에서 따옴표로 감싼 테이블 한정 컬럼</b>
 * ({@code "board_post"."title" = ?})을 해석하지 못한다("Table not found"). 반면 <b>따옴표 없는</b> 식별자는
 * 대소문자 무시로 대상 테이블을 찾아 정상 동작한다(원본 동적 DAO 가 통과했던 이유 = 비한정 컬럼).
 *
 * <p>따라서 식별자를 따옴표 없이 렌더({@link RenderQuotedNames#NEVER})한다 — H2(대문자화)·PostgreSQL(소문자화)
 * 양쪽에서 이름이 정확히 매칭되고(우리 스키마엔 예약어 컬럼이 없다), SET/WHERE 한정 컬럼도 문제없이 동작한다.
 * Boot 의 jOOQ 자동구성 훅({@link DefaultConfigurationCustomizer})으로 적용하며, 이 앱의 jOOQ 는 board 전용이라
 * 영향 범위가 board 로 한정된다.
 */
@Configuration
public class BoardJooqConfig {

    @Bean
    DefaultConfigurationCustomizer boardJooqConfigurationCustomizer() {
        return config -> config.settings().setRenderQuotedNames(RenderQuotedNames.NEVER);
    }
}
