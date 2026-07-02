package ai.mutuus.sample.board.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * 게시글 <b>동적 검색</b> 요청(Record). 모든 조건은 <b>선택</b>이며, 값이 있는 조건만 WHERE 에 조립된다(동적 WHERE).
 * jOOQ 의 강점(런타임에 SELECT/WHERE 를 프로그램적으로 구성)을 보여주는 샘플이다.
 * <ul>
 *   <li>{@code keyword} — 제목·작성자 부분일치(LIKE, 와일드카드 이스케이프)</li>
 *   <li>{@code authors} — 작성자 목록 정확일치(IN)</li>
 *   <li>{@code createdFrom}/{@code createdTo} — 작성일시 범위(from~to, 각각 생략 가능)</li>
 *   <li>{@code fields} — 응답에 포함할 컬럼(동적 SELECT, 허용목록 기반). 비우면 전체 컬럼</li>
 *   <li>{@code page}/{@code size} — 페이징(서비스에서 안전 범위로 정규화)</li>
 * </ul>
 * 형식 검증(길이/개수)은 여기서, <b>의미 검증</b>(허용되지 않은 필드명·범위 역전)은 서비스가 담당한다.
 */
public record BoardSearchRequest(
        @Size(max = 100) String keyword,
        @Size(max = 50) List<@Size(max = 50) String> authors,
        Instant createdFrom,
        Instant createdTo,
        @Size(max = 20) List<@Size(max = 30) String> fields,
        Integer page,
        Integer size) {
}
