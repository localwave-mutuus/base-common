/**
 * <b>demo</b> 모듈 — 라이브러리 기능 시연(케이스/보안/캐시/DB/이벤트). 로그뷰어(logs)·감사(persistence)를 의존한다.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"logs", "persistence"})
package ai.mutuus.sample.demo;
