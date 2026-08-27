/**
 * Offline Secret Manager v2 inline 암호문을 Spring Boot Config Data 이후에 검증·복호화하는
 * opt-in 부트스트랩 기능이다.
 *
 * <p>현재 허용 대상은 {@code spring.datasource.password} 하나이며, 웹/배치 의존성 없이
 * 기존 단일 common-platform JAR 안에서 동작한다.
 */
package ai.mutuus.common.secret;
