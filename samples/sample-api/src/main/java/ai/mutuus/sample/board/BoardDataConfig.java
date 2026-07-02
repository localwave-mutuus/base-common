package ai.mutuus.sample.board;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Data JPA 와 Spring Data JDBC 를 <b>한 애플리케이션에서 공존</b>시키기 위한 리포지토리 스코핑.
 * 각 모듈이 서로의 패키지를 스캔해 충돌하지 않도록 basePackages 로 분리한다.
 * <ul>
 *   <li>JPA: {@code sample.persistence}(기존 감사 데모) + {@code sample.board.jpa}</li>
 *   <li>JDBC: {@code sample.board.jdbc}</li>
 * </ul>
 * (jOOQ 는 리포지토리 스캔이 아니라 {@code DSLContext} 직접 사용이라 스코핑 불필요)
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackages = {"ai.mutuus.sample.persistence", "ai.mutuus.sample.board.jpa"})
@EnableJdbcRepositories(basePackages = "ai.mutuus.sample.board.jdbc")
public class BoardDataConfig {
}
