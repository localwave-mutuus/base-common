package ai.mutuus.sample.board.jooq;

import static ai.mutuus.sample.board.jooq.gen.Tables.BOARD_COMMENT;
import static ai.mutuus.sample.board.jooq.gen.Tables.BOARD_LIKE;
import static ai.mutuus.sample.board.jooq.gen.Tables.BOARD_POST;

import java.time.Instant;
import java.util.List;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * 게시글/좋아요/댓글 jOOQ DAO — <b>jooq-codegen</b> 이 Flyway 마이그레이션에서 생성한 타입세이프 테이블
 * ({@link ai.mutuus.sample.board.jooq.gen.Tables})로 SQL 을 구성한다. 문자열 {@code DSL.name(...)} 대신
 * 생성 상수({@code BOARD_POST.TITLE} 등)를 써 컬럼 오타/타입 오류가 컴파일 타임에 잡힌다(운영 권장 방식).
 * 검색은 이스케이프된 키워드 + {@code escape '\'} 로 와일드카드 오남용을 막는다(키워드는 서비스에서 escapeLike 처리).
 */
@Repository
public class BoardJooqDao {

    private static final char LIKE_ESCAPE = '\\';

    private final DSLContext dsl;

    public BoardJooqDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ----- 게시글 -----

    public BoardPostRow insert(String title, String content, String author) {
        Instant now = Instant.now();
        Record key = dsl.insertInto(BOARD_POST)
                .columns(BOARD_POST.TITLE, BOARD_POST.CONTENT, BOARD_POST.AUTHOR,
                        BOARD_POST.CREATED_AT, BOARD_POST.UPDATED_AT, BOARD_POST.VERSION)
                .values(title, content, author, now, now, 0L) // 신규 글의 초기 버전 0
                .returning(BOARD_POST.ID).fetchOne();
        if (key == null) { // 방어: 일부 DB/드라이버에서 returning 실패 가능
            throw new IllegalStateException("board_post insert 후 생성 키를 얻지 못했습니다.");
        }
        return findById(key.get(BOARD_POST.ID));
    }

    public BoardPostRow findById(long id) {
        Record r = dsl.select(BOARD_POST.ID, BOARD_POST.TITLE, BOARD_POST.CONTENT, BOARD_POST.AUTHOR,
                        BOARD_POST.CREATED_AT, BOARD_POST.UPDATED_AT, BOARD_POST.VERSION)
                .from(BOARD_POST).where(BOARD_POST.ID.eq(id)).fetchOne();
        return r == null ? null : toRow(r);
    }

    public boolean postExists(long id) {
        return dsl.fetchExists(dsl.selectOne().from(BOARD_POST).where(BOARD_POST.ID.eq(id)));
    }

    public boolean existsByAuthorAndTitle(String author, String title) {
        return dsl.fetchExists(dsl.selectOne().from(BOARD_POST)
                .where(BOARD_POST.AUTHOR.eq(author)).and(BOARD_POST.TITLE.eq(title)));
    }

    public boolean existsByAuthorAndTitleExcept(String author, String title, long exceptId) {
        return dsl.fetchExists(dsl.selectOne().from(BOARD_POST)
                .where(BOARD_POST.AUTHOR.eq(author)).and(BOARD_POST.TITLE.eq(title)).and(BOARD_POST.ID.ne(exceptId)));
    }

    public List<BoardPostRow> search(String keyword, int size, int offset) {
        return dsl.select(BOARD_POST.ID, BOARD_POST.TITLE, BOARD_POST.CONTENT, BOARD_POST.AUTHOR,
                        BOARD_POST.CREATED_AT, BOARD_POST.UPDATED_AT, BOARD_POST.VERSION)
                .from(BOARD_POST)
                .where(searchCondition(keyword)).orderBy(BOARD_POST.ID.desc()).limit(size).offset(offset)
                .fetch(this::toRow);
    }

    public long count(String keyword) {
        Integer n = dsl.selectCount().from(BOARD_POST).where(searchCondition(keyword)).fetchOne(0, Integer.class);
        return n == null ? 0 : n;
    }

    /**
     * 게시글 수정. {@code expectedVersion} 이 주어지면 <b>원자적 낙관적 락</b> — {@code WHERE id=? AND version=?} 로
     * 조건부 갱신하고 {@code version} 을 1 증가시킨다(그 사이 변경됐으면 매칭 0건 → 반영 행 수 0으로 회신).
     * null 이면 버전 조건 없이 갱신한다. @return 실제 갱신된 행 수.
     */
    public int update(long id, String title, String content, String author, Long expectedVersion) {
        Condition where = BOARD_POST.ID.eq(id);
        if (expectedVersion != null) {
            where = where.and(BOARD_POST.VERSION.eq(expectedVersion));
        }
        return dsl.update(BOARD_POST)
                .set(BOARD_POST.TITLE, title).set(BOARD_POST.CONTENT, content).set(BOARD_POST.AUTHOR, author)
                .set(BOARD_POST.UPDATED_AT, Instant.now()).set(BOARD_POST.VERSION, BOARD_POST.VERSION.plus(1))
                .where(where).execute();
    }

    public int delete(long id) {
        return dsl.deleteFrom(BOARD_POST).where(BOARD_POST.ID.eq(id)).execute();
    }

    private Condition searchCondition(String keyword) {
        if (keyword == null) {
            return DSL.noCondition();
        }
        String pattern = "%" + keyword + "%"; // keyword 는 이미 escapeLike 처리됨
        return BOARD_POST.TITLE.likeIgnoreCase(pattern, LIKE_ESCAPE)
                .or(BOARD_POST.AUTHOR.likeIgnoreCase(pattern, LIKE_ESCAPE));
    }

    private BoardPostRow toRow(Record r) {
        return new BoardPostRow(r.get(BOARD_POST.ID), r.get(BOARD_POST.TITLE), r.get(BOARD_POST.CONTENT),
                r.get(BOARD_POST.AUTHOR), r.get(BOARD_POST.CREATED_AT), r.get(BOARD_POST.UPDATED_AT),
                r.get(BOARD_POST.VERSION));
    }

    // ----- 좋아요 -----

    public boolean likeExists(long postId, String author) {
        return dsl.fetchExists(dsl.selectOne().from(BOARD_LIKE)
                .where(BOARD_LIKE.POST_ID.eq(postId)).and(BOARD_LIKE.AUTHOR.eq(author)));
    }

    public void insertLike(long postId, String author) {
        dsl.insertInto(BOARD_LIKE).columns(BOARD_LIKE.POST_ID, BOARD_LIKE.AUTHOR, BOARD_LIKE.CREATED_AT)
                .values(postId, author, Instant.now()).execute();
    }

    public long countLikes(long postId) {
        Integer n = dsl.selectCount().from(BOARD_LIKE).where(BOARD_LIKE.POST_ID.eq(postId)).fetchOne(0, Integer.class);
        return n == null ? 0 : n;
    }

    // ----- 댓글 -----

    public CommentRow insertComment(long postId, String author, String content) {
        Record key = dsl.insertInto(BOARD_COMMENT)
                .columns(BOARD_COMMENT.POST_ID, BOARD_COMMENT.AUTHOR, BOARD_COMMENT.CONTENT, BOARD_COMMENT.CREATED_AT)
                .values(postId, author, content, Instant.now())
                .returning(BOARD_COMMENT.ID).fetchOne();
        if (key == null) {
            throw new IllegalStateException("board_comment insert 후 생성 키를 얻지 못했습니다.");
        }
        return dsl.select(BOARD_COMMENT.ID, BOARD_COMMENT.POST_ID, BOARD_COMMENT.AUTHOR,
                        BOARD_COMMENT.CONTENT, BOARD_COMMENT.CREATED_AT)
                .from(BOARD_COMMENT).where(BOARD_COMMENT.ID.eq(key.get(BOARD_COMMENT.ID)))
                .fetchOne(this::toComment);
    }

    public List<CommentRow> findComments(long postId) {
        return dsl.select(BOARD_COMMENT.ID, BOARD_COMMENT.POST_ID, BOARD_COMMENT.AUTHOR,
                        BOARD_COMMENT.CONTENT, BOARD_COMMENT.CREATED_AT)
                .from(BOARD_COMMENT)
                .where(BOARD_COMMENT.POST_ID.eq(postId)).orderBy(BOARD_COMMENT.ID.asc()).fetch(this::toComment);
    }

    private CommentRow toComment(Record r) {
        return new CommentRow(r.get(BOARD_COMMENT.ID), r.get(BOARD_COMMENT.POST_ID), r.get(BOARD_COMMENT.AUTHOR),
                r.get(BOARD_COMMENT.CONTENT), r.get(BOARD_COMMENT.CREATED_AT));
    }
}
