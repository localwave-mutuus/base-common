package ai.mutuus.common.response;

import ai.mutuus.common.api.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 성공 응답 자동 래핑 — 컨트롤러가 반환한 평범한 객체를 표준 {@link ApiResponse} 봉투로 감싼다.
 * 컨트롤러는 {@code return dto;} 만으로 {@code {"code":"OK","data":dto,...}} 응답을 얻는다(수동 래핑 불필요).
 * <p><b>안전장치</b>(이중 래핑·직렬화 파손 방지) — 아래는 그대로 통과시킨다:
 * <ul>
 *   <li>이미 {@link ApiResponse}/{@link ProblemDetail} 인 본문(이중 래핑·오류 응답 보호)</li>
 *   <li>{@code String}/{@code byte[]}/{@link Resource}(전용 컨버터를 쓰므로 봉투로 감싸면 파손)</li>
 *   <li>JSON(application/json 계열)이 아닌 응답</li>
 *   <li>제외 경로(springdoc {@code /v3/api-docs}·swagger-ui·actuator 등 프레임워크 응답)</li>
 * </ul>
 * 기본 OFF 이며 {@code CommonResponseWrapperAutoConfiguration} 이 프로퍼티로 켤 때만 등록한다.
 */
@ControllerAdvice
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ResponseWrapperProperties props;

    public ApiResponseWrapperAdvice(ResponseWrapperProperties props) {
        this.props = props;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 실제 판별은 beforeBodyWrite 에서 한다(요청 경로·컨텐트타입·본문 실제 타입까지 봐야 안전).
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 이미 봉투이거나 표준 오류(ProblemDetail) → 그대로(이중 래핑 금지)
        if (body instanceof ApiResponse<?> || body instanceof ProblemDetail) {
            return body;
        }
        // 전용 컨버터를 쓰는 타입 → 봉투로 감싸면 직렬화가 깨진다
        if (body instanceof String || body instanceof byte[] || body instanceof Resource) {
            return body;
        }
        // JSON 응답만 래핑(그 외 미디어타입은 손대지 않음). null contentType(본문 없음) 도 통과.
        if (contentType == null || !contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return body;
        }
        // 프레임워크 응답 경로(springdoc/actuator/swagger)는 보호
        if (isExcluded(request.getURI().getPath())) {
            return body;
        }
        // 평범한 객체(또는 null) → 표준 성공 봉투로 감싼다.
        return ApiResponse.ok(body);
    }

    private boolean isExcluded(String path) {
        for (String prefix : props.getExcludePathPrefixes()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
