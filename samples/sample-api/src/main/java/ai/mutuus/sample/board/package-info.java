/**
 * <b>board</b> 모듈 — 게시판 3-스택(JPA/jOOQ/JDBC) 바운디드 컨텍스트. 영속 감사(persistence)만 의존한다.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"persistence"})
package ai.mutuus.sample.board;
