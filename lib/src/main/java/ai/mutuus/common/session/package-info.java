/**
 * common-session: 분산 세션(Redis) 컨벤션.
 * <p>Spring Session Redis 가 classpath 에 있을 때, Boot 가 만든 세션 저장소에 서비스명 기반
 * 키 네임스페이스와 기본 타임아웃 등 공통 컨벤션을 얹는 optional 통합 패키지다. 실제 Redis
 * 연결·세션 저장 자체는 Boot 자동구성에 위임한다.
 */
package ai.mutuus.common.session;
