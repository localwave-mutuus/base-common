/**
 * common-persistence: JPA 공통 베이스 엔티티와 감사(audit) 자동화.
 * <p>{@link ai.mutuus.common.persistence.BaseEntity} 를 상속하면 생성/수정 시각·주체가 JPA
 * Auditing 으로 자동 기록되며, 주체는 {@link ai.mutuus.common.persistence.TraceContextAuditorAware}
 * 가 {@link ai.mutuus.common.core.TraceContext} 의 인증 사용자에서 가져온다. spring-data-jpa 가
 * classpath 에 있을 때만 활성화되는 optional 통합 패키지다.
 */
package ai.mutuus.common.persistence;
