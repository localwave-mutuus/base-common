package ai.mutuus.common.intercept;

import java.util.List;

import ai.mutuus.common.core.EcsFields;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link ControllerEntryHandler} 기본 구현 — 진입한 컨트롤러 메서드를 구조화 JSON 한 줄로 로깅한다.
 * <p>로거 이름 {@code ai.mutuus.common.controller}. 추적ID/appCode 등은 MDC 로 자동 포함된다.
 * 출력 형식을 바꾸려면 이 클래스를 갱신하거나 소비 측에서 {@link ControllerEntryHandler} 빈을 재정의한다.
 */
public class DefaultControllerEntryHandler implements ControllerEntryHandler {

    public static final String LOGGER_NAME = "ai.mutuus.common.controller";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public void onEntry(HandlerMethod handlerMethod, HttpServletRequest request) {
        // 진입한 컨트롤러의 식별정보를 구조화 필드로 남긴다(추적ID/appCode 는 MDC 자동 포함).
        String controller = handlerMethod.getBeanType().getSimpleName();
        String method = handlerMethod.getMethod().getName();
        log.atInfo()
                .addKeyValue("event", "controller.entry")
                .addKeyValue("controller", controller)                                  // 클래스명
                .addKeyValue("method", method)                                          // 메서드명
                .addKeyValue("package", handlerMethod.getBeanType().getPackageName())   // 패키지
                .addKeyValue("httpMethod", request.getMethod())
                .addKeyValue("httpPath", request.getRequestURI())
                // ECS(dual)
                .addKeyValue(EcsFields.EVENT_DATASET, EcsFields.DATASET_CONTROLLER)
                .addKeyValue(EcsFields.DATA_STREAM_DATASET, EcsFields.DATASET_CONTROLLER)
                .addKeyValue(EcsFields.EVENT_ACTION, "controller.entry")
                .addKeyValue(EcsFields.EVENT_CATEGORY, List.of("web"))
                .addKeyValue(EcsFields.EVENT_TYPE, List.of("info"))
                .addKeyValue(EcsFields.CODE_NAMESPACE, handlerMethod.getBeanType().getName())
                .addKeyValue(EcsFields.CODE_FUNCTION, method)
                .addKeyValue(EcsFields.HTTP_REQUEST_METHOD, request.getMethod())
                .addKeyValue(EcsFields.URL_PATH, request.getRequestURI())
                .log("controller method entry");
    }
}
