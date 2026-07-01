package ai.mutuus.sample.demo;

import java.util.List;
import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 단일/배열 입력 파라미터 데모 — <b>쿼리 방식</b>과 <b>본문 방식</b> 각각에서 단일값·배열값을 함께 받아
 * payload/method 로깅에 어떻게 남는지 관찰한다. OpenAPI 3.x(3.1) 스펙에 그대로 반영되도록
 * swagger 어노테이션({@code @Operation/@Parameter/@Schema})을 붙였다(springdoc 이 {@code /v3/api-docs} 로 노출).
 * <p>OpenAPI 관점 매핑:
 * <ul>
 *   <li>쿼리 단일 {@code q} → Parameter(in=query, schema=string)</li>
 *   <li>쿼리 배열 {@code ids} → Parameter(in=query, schema=array, style=form) — {@code ?ids=a&ids=b} 또는 {@code ?ids=a,b}</li>
 *   <li>본문 단일 {@code tag} → requestBody object 의 string property</li>
 *   <li>본문 배열 {@code tags} → requestBody object 의 array property</li>
 * </ul>
 * 패키지가 {@code ai.mutuus.sample.demo} 라 controller-entry/method-logging 타게팅에 포함된다.
 */
@RestController
@RequestMapping("/demo/params")
@Tag(name = "params-demo", description = "단일/배열 파라미터 입력 로깅 데모")
public class ParamsDemoController {

    /** 쿼리 파라미터: 단일({@code q}) + 배열({@code ids}). */
    @Operation(summary = "쿼리 파라미터(단일 q + 배열 ids)",
            description = "예: /demo/params/query?q=hello&ids=a&ids=b (또는 ids=a,b)")
    @GetMapping("/query")
    public ApiResponse<Map<String, Object>> query(
            @Parameter(description = "단일 문자열 파라미터", example = "hello")
            @RequestParam String q,
            @Parameter(description = "문자열 배열 파라미터(반복 또는 콤마 구분)", example = "a")
            @RequestParam List<String> ids) {
        return ApiResponse.ok(Map.of(
                "q", q,
                "ids", ids,
                "idsCount", ids.size()));
    }

    /** 요청 본문: 단일({@code tag}) + 배열({@code tags}). */
    @Operation(summary = "요청 본문(단일 tag + 배열 tags)")
    @PostMapping("/body")
    public ApiResponse<Map<String, Object>> body(@Valid @RequestBody ParamsRequest req) {
        return ApiResponse.ok(Map.of(
                "tag", req.tag(),
                "tags", req.tags(),
                "tagsCount", req.tags().size()));
    }

    /** 본문 DTO: 단일 문자열 필드 + 문자열 배열 필드(검증 포함 → OpenAPI required/제약에 반영). */
    public record ParamsRequest(
            @Schema(description = "단일 문자열 필드", example = "primary")
            @NotBlank String tag,
            @Schema(description = "문자열 배열 필드", example = "[\"a\",\"b\",\"c\"]")
            @NotEmpty List<@NotBlank String> tags) {
    }
}
