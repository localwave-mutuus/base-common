package ai.mutuus.sample.board;

import ai.mutuus.common.exception.BusinessException;

/**
 * 게시판 <b>입력 상호관계(cross-field) 비즈니스 규칙</b> 모음(DB 무관, 순수 입력 검증). 3스택 서비스가 공유해
 * 규칙의 단일 출처를 유지한다. (DB 사전검증 — 중복 제목·좋아요 선행 등 — 은 각 서비스가 리포지토리로 수행)
 */
public final class BoardRules {

    /** 공지 제목 접두. */
    public static final String NOTICE_PREFIX = "[공지]";
    /** 공지를 등록할 수 있는 지정 작성자. */
    public static final String NOTICE_AUTHOR = "admin";

    private BoardRules() {
    }

    /**
     * 공지 규칙: 제목이 {@code [공지]} 로 시작하면 작성자는 {@link #NOTICE_AUTHOR} 여야 한다(제목↔작성자 상호관계).
     * 위반 시 {@link BoardErrorCode#NOTICE_NOT_ALLOWED}.
     */
    public static void validateNotice(String title, String author) {
        if (title != null && title.startsWith(NOTICE_PREFIX) && !NOTICE_AUTHOR.equals(author)) {
            throw new BusinessException(BoardErrorCode.NOTICE_NOT_ALLOWED);
        }
    }
}
