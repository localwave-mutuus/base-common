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
 * 게시글 jOOQ DAO — {@link DSLContext} 로 타입세이프 SQL 구성(코드젠 없는 동적 API; 운영은 jooq-codegen 권장).
 * 검색/페이징은 조건 + {@code limit/offset} + 별도 count 로 표현한다(복잡·PG특화 쿼리에 강점).
 */
@Repository
public class BoardJooqDao {

    private static final Table<Record> BOARD = DSL.table(DSL.name("board_post"));
    private static final Field<Long> ID = DSL.field(DSL.name("id"), Long.class);
    private static final Field<String> TITLE = DSL.field(DSL.name("title"), String.class);
    private static final Field<String> CONTENT = DSL.field(DSL.name("content"), String.class);
    private static final Field<String> AUTHOR = DSL.field(DSL.name("author"), String.class);
    private static final Field<Instant> CREATED_AT = DSL.field(DSL.name("created_at"), Instant.class);

    private final DSLContext dsl;

    public BoardJooqDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    public BoardPostRow insert(String title, String content, String author) {
        Long id = dsl.insertInto(BOARD).columns(TITLE, CONTENT, AUTHOR, CREATED_AT)
                .values(title, content, author, Instant.now())
                .returning(ID).fetchOne().get(ID);
        return findById(id);
    }

    public BoardPostRow findById(long id) {
        Record r = dsl.select(ID, TITLE, CONTENT, AUTHOR, CREATED_AT).from(BOARD).where(ID.eq(id)).fetchOne();
        return r == null ? null : toRow(r);
    }

    public List<BoardPostRow> search(String keyword, int size, int offset) {
        return dsl.select(ID, TITLE, CONTENT, AUTHOR, CREATED_AT).from(BOARD)
                .where(condition(keyword)).orderBy(ID.desc()).limit(size).offset(offset)
                .fetch(this::toRow);
    }

    public long count(String keyword) {
        Integer n = dsl.selectCount().from(BOARD).where(condition(keyword)).fetchOne(0, Integer.class);
        return n == null ? 0 : n;
    }

    public int update(long id, String title, String content, String author) {
        return dsl.update(BOARD).set(TITLE, title).set(CONTENT, content).set(AUTHOR, author)
                .where(ID.eq(id)).execute();
    }

    public int delete(long id) {
        return dsl.deleteFrom(BOARD).where(ID.eq(id)).execute();
    }

    /** 키워드(제목/작성자 부분일치) 조건. 공백이면 전체(noCondition). */
    private Condition condition(String keyword) {
        if (keyword == null) {
            return DSL.noCondition();
        }
        String like = "%" + keyword + "%";
        return TITLE.likeIgnoreCase(like).or(AUTHOR.likeIgnoreCase(like));
    }

    private BoardPostRow toRow(Record r) {
        return new BoardPostRow(r.get(ID), r.get(TITLE), r.get(CONTENT), r.get(AUTHOR), r.get(CREATED_AT));
    }
}
