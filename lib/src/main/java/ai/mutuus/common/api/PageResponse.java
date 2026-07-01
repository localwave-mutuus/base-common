package ai.mutuus.common.api;

import java.util.List;

/**
 * 표준 페이징 응답 DTO. 목록 API 가 페이지 메타(번호/크기/총건수/총페이지/처음·마지막 여부)를
 * 일관된 형태로 반환하게 한다.
 * <p><b>순수 DTO</b> — Spring Data 에 의존하지 않으므로 JPA/비JPA·수동 페이징 어디서나 쓸 수 있다.
 * Spring Data {@code Page} 를 그대로 쓰는 소비자는 {@code ai.mutuus.common.persistence.PageResponses#from}
 * 어댑터(spring-data 존재 시)로 변환하고, 그 외에는 {@link #of(List, int, int, long)} 로 만든다.
 * <p>보통 {@link ApiResponse} 의 {@code data} 로 실려 나간다(성공 응답 자동 래핑과도 결합).
 *
 * @param <T> 페이지 항목 타입
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        int numberOfElements) {

    /**
     * 목록·페이지 좌표·총건수로부터 페이지 메타를 계산해 만든다.
     *
     * @param content       현재 페이지 항목
     * @param page          0-based 페이지 번호
     * @param size          페이지 크기
     * @param totalElements 전체 항목 수
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (size <= 0) ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        boolean first = page <= 0;
        boolean last = (totalPages == 0) || (page >= totalPages - 1);
        int numberOfElements = (content == null) ? 0 : content.size();
        return new PageResponse<>(content, page, size, totalElements, totalPages, first, last, numberOfElements);
    }
}
