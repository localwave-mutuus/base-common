package ai.mutuus.sample.board.jdbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 게시글 <b>Spring Data JDBC</b> 애그리거트(테이블 {@code board_post}). JPA 와 달리 프록시·지연로딩 "마법"이 없어
 * 예측 가능하다. {@code @Id} 는 spring-data 의 것(jakarta 아님), 컬럼은 명시 매핑(H2 대문자 스키마와 정합).
 */
@Table("board_post")
public class BoardPostJdbc {

    @Id
    private Long id;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("author")
    private String author;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    /** 낙관적 락 버전(Spring Data JDBC 자동 관리). */
    @Version
    @Column("version")
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
