package ai.mutuus.sample.board.jooq;

import java.time.Instant;
import java.util.List;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * 게시글/좋아요/댓글 jOOQ DAO — {@link DSLContext} 로 타입세이프 SQL 구성(코드젠 없는 동적 API; 운영은 jooq-codegen 권장).
 * 검색은 이스케이프된 키워드 + {@code escape '\'} 로 와일드카드 오남용을 막는다(키워드는 서비스에서 escapeLike 처리).
 */
@Repository
public class BoardJooqDao {

    private static final char LIKE_ESCAPE = '\\';

    // board_post
    private static final Table<Record> POST = DSL.table(DSL.name("board_post"));
    private static final Field<Long> P_ID = DSL.field(DSL.name("id"), Long.class);
    private static final Field<String> P_TITLE = DSL.field(DSL.name("title"), String.class);
    private static final Field<String> P_CONTENT = DSL.field(DSL.name("content"), String.class);
    private static final Field<String> P_AUTHOR = DSL.field(DSL.name("author"), String.class);
    private static final Field<Instant> P_CREATED = DSL.field(DSL.name("created_at"), Instant.class);
    private static final Field<Instant> P_UPDATED = DSL.field(DSL.name("updated_at"), Instant.class);
    // board_like
    private static final Table<Record> LIKE = DSL.table(DSL.name("board_like"));
    private static final Field<Long> L_POST = DSL.field(DSL.name("post_id"), Long.class);
    private static final Field<String> L_AUTHOR = DSL.field(DSL.name("author"), String.class);
    private static final Field<Instant> L_CREATED = DSL.field(DSL.name("created_at"), Instant.class);
    // board_comment
    private static final Table<Record> COMMENT = DSL.table(DSL.name("board_comment"));
    private static final Field<Long> C_ID = DSL.field(DSL.name("id"), Long.class);
    private static final Field<Long> C_POST = DSL.field(DSL.name("post_id"), Long.class);
    private static final Field<String> C_AUTHOR = DSL.field(DSL.name("author"), String.class);
    private static final Field<String> C_CONTENT = DSL.field(DSL.name("content"), String.class);
    private static final Field<Instant> C_CREATED = DSL.field(DSL.name("created_at"), Instant.class);

    private final DSLContext dsl;

    public BoardJooqDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ----- 게시글 -----

    public BoardPostRow insert(String title, String content, String author) {
        Instant now = Instant.now();
        Record key = dsl.insertInto(POST).columns(P_TITLE, P_CONTENT, P_AUTHOR, P_CREATED, P_UPDATED)
                .values(title, content, author, now, now)
                .returning(P_ID).fetchOne();
        if (key == null) { // 방어: 일부 DB/드라이버에서 returning 실패 가능
            throw new IllegalStateException("board_post insert 후 생성 키를 얻지 못했습니다.");
        }
        return findById(key.get(P_ID));
    }

    public BoardPostRow findById(long id) {
        Record r = dsl.select(P_ID, P_TITLE, P_CONTENT, P_AUTHOR, P_CREATED, P_UPDATED)
                .from(POST).where(P_ID.eq(id)).fetchOne();
        return r == null ? null : toRow(r);
    }

    public boolean postExists(long id) {
        return dsl.fetchExists(dsl.selectOne().from(POST).where(P_ID.eq(id)));
    }

    public boolean existsByAuthorAndTitle(String author, String title) {
        return dsl.fetchExists(dsl.selectOne().from(POST).where(P_AUTHOR.eq(author)).and(P_TITLE.eq(title)));
    }

    public boolean existsByAuthorAndTitleExcept(String author, String title, long exceptId) {
        return dsl.fetchExists(dsl.selectOne().from(POST)
                .where(P_AUTHOR.eq(author)).and(P_TITLE.eq(title)).and(P_ID.ne(exceptId)));
    }

    public List<BoardPostRow> search(String keyword, int size, int offset) {
        return dsl.select(P_ID, P_TITLE, P_CONTENT, P_AUTHOR, P_CREATED, P_UPDATED).from(POST)
                .where(searchCondition(keyword)).orderBy(P_ID.desc()).limit(size).offset(offset)
                .fetch(this::toRow);
    }

    public long count(String keyword) {
        Integer n = dsl.selectCount().from(POST).where(searchCondition(keyword)).fetchOne(0, Integer.class);
        return n == null ? 0 : n;
    }

    public int update(long id, String title, String content, String author) {
        return dsl.update(POST).set(P_TITLE, title).set(P_CONTENT, content).set(P_AUTHOR, author)
                .set(P_UPDATED, Instant.now())
                .where(P_ID.eq(id)).execute();
    }

    public int delete(long id) {
        return dsl.deleteFrom(POST).where(P_ID.eq(id)).execute();
    }

    private Condition searchCondition(String keyword) {
        if (keyword == null) {
            return DSL.noCondition();
        }
        String pattern = "%" + keyword + "%"; // keyword 는 이미 escapeLike 처리됨
        return P_TITLE.likeIgnoreCase(pattern, LIKE_ESCAPE).or(P_AUTHOR.likeIgnoreCase(pattern, LIKE_ESCAPE));
    }

    private BoardPostRow toRow(Record r) {
        return new BoardPostRow(r.get(P_ID), r.get(P_TITLE), r.get(P_CONTENT), r.get(P_AUTHOR),
                r.get(P_CREATED), r.get(P_UPDATED));
    }

    // ----- 좋아요 -----

    public boolean likeExists(long postId, String author) {
        return dsl.fetchExists(dsl.selectOne().from(LIKE).where(L_POST.eq(postId)).and(L_AUTHOR.eq(author)));
    }

    public void insertLike(long postId, String author) {
        dsl.insertInto(LIKE).columns(L_POST, L_AUTHOR, L_CREATED)
                .values(postId, author, Instant.now()).execute();
    }

    public long countLikes(long postId) {
        Integer n = dsl.selectCount().from(LIKE).where(L_POST.eq(postId)).fetchOne(0, Integer.class);
        return n == null ? 0 : n;
    }

    // ----- 댓글 -----

    public CommentRow insertComment(long postId, String author, String content) {
        Record key = dsl.insertInto(COMMENT).columns(C_POST, C_AUTHOR, C_CONTENT, C_CREATED)
                .values(postId, author, content, Instant.now())
                .returning(C_ID).fetchOne();
        if (key == null) {
            throw new IllegalStateException("board_comment insert 후 생성 키를 얻지 못했습니다.");
        }
        return dsl.select(C_ID, C_POST, C_AUTHOR, C_CONTENT, C_CREATED).from(COMMENT).where(C_ID.eq(key.get(C_ID)))
                .fetchOne(this::toComment);
    }

    public List<CommentRow> findComments(long postId) {
        return dsl.select(C_ID, C_POST, C_AUTHOR, C_CONTENT, C_CREATED).from(COMMENT)
                .where(C_POST.eq(postId)).orderBy(C_ID.asc()).fetch(this::toComment);
    }

    private CommentRow toComment(Record r) {
        return new CommentRow(r.get(C_ID), r.get(C_POST), r.get(C_AUTHOR), r.get(C_CONTENT), r.get(C_CREATED));
    }
}
