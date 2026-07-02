package ai.mutuus.sample.board.jooq;

import java.time.Instant;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * 게시글 jOOQ DAO — {@link DSLContext} 로 타입세이프 SQL 을 구성한다(코드젠 없는 동적 API; 운영에선
 * jooq-codegen 으로 완전한 컴파일 타임 타입 안전성 확보 가능). JSONB·CTE·window·upsert 같은 PG 특화 쿼리에 강점.
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
        Long id = dsl.insertInto(BOARD)
                .columns(TITLE, CONTENT, AUTHOR, CREATED_AT)
                .values(title, content, author, Instant.now())
                .returning(ID)
                .fetchOne()
                .get(ID);
        return findById(id);
    }

    public List<BoardPostRow> findAll() {
        return dsl.select(ID, TITLE, CONTENT, AUTHOR, CREATED_AT).from(BOARD).orderBy(ID.asc()).fetch(this::toRow);
    }

    public BoardPostRow findById(Long id) {
        Record r = dsl.select(ID, TITLE, CONTENT, AUTHOR, CREATED_AT).from(BOARD).where(ID.eq(id)).fetchOne();
        return r == null ? null : toRow(r);
    }

    public List<BoardPostRow> findByAuthor(String author) {
        return dsl.select(ID, TITLE, CONTENT, AUTHOR, CREATED_AT).from(BOARD)
                .where(AUTHOR.eq(author)).orderBy(ID.desc()).fetch(this::toRow);
    }

    public int update(Long id, String title, String content, String author) {
        return dsl.update(BOARD).set(TITLE, title).set(CONTENT, content).set(AUTHOR, author)
                .where(ID.eq(id)).execute();
    }

    public int delete(Long id) {
        return dsl.deleteFrom(BOARD).where(ID.eq(id)).execute();
    }

    private BoardPostRow toRow(Record r) {
        return new BoardPostRow(r.get(ID), r.get(TITLE), r.get(CONTENT), r.get(AUTHOR), r.get(CREATED_AT));
    }
}
