package ai.mutuus.common.core;

/**
 * ECS(Elastic Common Schema) 필드 이름과 dataset 상수의 <b>단일 출처</b>.
 * <p>로거 호출부와 필터(MDC alias)·logback {@code includeMdcKeyName} 이 같은 이름을 써야 하므로
 * 문자열을 여기 한 곳에 모은다(드리프트 방지). ECS 는 점 표기 필드를 쓰며, Elasticsearch 가 색인 시
 * 중첩 객체로 확장한다.
 *
 * <p><b>MDC alias</b>({@code trace.id} 등)는 스칼라 keyword 라 필터가 MDC 에 싣는다.
 * 배열({@code event.category})·숫자({@code event.duration})·ip({@code client.ip}) 는 MDC 로 실을 수 없어
 * 로거 호출부가 {@code addKeyValue} 로 직접 채운다.
 */
public final class EcsFields {

    private EcsFields() {
    }

    // --- MDC alias (스칼라 keyword) ---
    public static final String TRACE_ID = "trace.id";
    public static final String SPAN_ID = "span.id";
    public static final String TRANSACTION_ID = "transaction.id";
    public static final String USER_ID = "user.id";
    public static final String SERVICE_NODE_NAME = "service.node.name";

    // --- event.* ---
    public static final String EVENT_ACTION = "event.action";
    public static final String EVENT_DATASET = "event.dataset";
    public static final String EVENT_CATEGORY = "event.category";
    public static final String EVENT_TYPE = "event.type";
    public static final String EVENT_OUTCOME = "event.outcome";
    public static final String EVENT_DURATION = "event.duration";

    // --- data_stream.* (수집기 라우팅) ---
    public static final String DATA_STREAM_DATASET = "data_stream.dataset";

    // --- http/url ---
    public static final String HTTP_REQUEST_METHOD = "http.request.method";
    public static final String URL_PATH = "url.path";
    public static final String URL_QUERY = "url.query";
    public static final String URL_FULL = "url.full";
    public static final String URL_DOMAIN = "url.domain";
    public static final String HTTP_RESPONSE_STATUS_CODE = "http.response.status_code";
    public static final String HTTP_REQUEST_MIME_TYPE = "http.request.mime_type";
    public static final String HTTP_RESPONSE_MIME_TYPE = "http.response.mime_type";
    public static final String HTTP_REQUEST_BODY_CONTENT = "http.request.body.content";
    public static final String HTTP_REQUEST_BODY_BYTES = "http.request.body.bytes";
    public static final String HTTP_RESPONSE_BODY_CONTENT = "http.response.body.content";
    public static final String HTTP_RESPONSE_BODY_BYTES = "http.response.body.bytes";

    // --- client/source ---
    public static final String CLIENT_IP = "client.ip";

    // --- error.* ---
    public static final String ERROR_TYPE = "error.type";
    public static final String ERROR_CODE = "error.code";
    public static final String ERROR_MESSAGE = "error.message";

    // --- code.* (메서드/컨트롤러) ---
    public static final String CODE_NAMESPACE = "code.namespace";
    public static final String CODE_FUNCTION = "code.function";

    // --- rule.* (보안) ---
    public static final String RULE_NAME = "rule.name";

    // --- outcome 값 ---
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";
    public static final String OUTCOME_UNKNOWN = "unknown";

    // --- dataset 값 (로그 종류별) ---
    public static final String DATASET_ACCESS = "mutuus.access";
    public static final String DATASET_ERROR = "mutuus.error";
    public static final String DATASET_SECURITY_AUDIT = "mutuus.security_audit";
    public static final String DATASET_PAYLOAD = "mutuus.payload";
    public static final String DATASET_CONTROLLER = "mutuus.controller";
    public static final String DATASET_METHOD = "mutuus.method";
    public static final String DATASET_HTTP_CLIENT = "mutuus.http_client";
    public static final String DATASET_DOMAIN_EVENT = "mutuus.domain_event";

    // --- mutuus.* 확장(비ECS 커스텀) ---
    public static final String MUTUUS_PAYLOAD_TRUNCATED = "mutuus.payload.truncated";
    public static final String MUTUUS_SECURITY_DISPOSITION = "mutuus.security.disposition";
    public static final String MUTUUS_DURATION_MS = "mutuus.duration.ms";
    public static final String MUTUUS_METHOD_ARGS = "mutuus.method.args";
    public static final String MUTUUS_METHOD_RETURN = "mutuus.method.return";
    public static final String MUTUUS_DOMAIN_EVENT_ID = "mutuus.domain_event.id";
    public static final String MUTUUS_DOMAIN_EVENT_TYPE = "mutuus.domain_event.type";
}
