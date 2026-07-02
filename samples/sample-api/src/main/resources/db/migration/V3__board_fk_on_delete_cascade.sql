-- 게시글 삭제 시 자식(좋아요/댓글)을 DB 레벨에서 함께 정리하도록 FK 에 ON DELETE CASCADE 를 부여한다.
-- (V1 에서 만든 FK 를 교체 — 이미 적용된 마이그레이션은 절대 수정하지 않고 새 버전으로 진화시키는 것이 Flyway 원칙.)
-- 좋아요/댓글은 게시글에 종속된(owned) 자식이라 부모 삭제 시 함께 지워지는 것이 도메인상 자연스럽다.
-- DROP/ADD CONSTRAINT + ON DELETE CASCADE 구문은 H2·PostgreSQL 공통 이식형이다.

alter table board_like drop constraint fk_board_like_post;
alter table board_like add constraint fk_board_like_post
    foreign key (post_id) references board_post (id) on delete cascade;

alter table board_comment drop constraint fk_board_comment_post;
alter table board_comment add constraint fk_board_comment_post
    foreign key (post_id) references board_post (id) on delete cascade;
