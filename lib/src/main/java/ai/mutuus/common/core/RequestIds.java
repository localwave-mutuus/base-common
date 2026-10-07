package ai.mutuus.common.core;

/** HTTP pair 로거가 발급한 동일 UUID. 헤더나 인증 subject로 추정하지 않는다. */
public final class RequestIds {
    public static final String CONTEXT_KEY = "requestId";
    public static final String HTTP_ATTRIBUTE = RequestIds.class.getName() + ".requestId";

    private RequestIds() { }

    /** 비동기 작업에는 TraceContextPropagation 또는 결과 snapshot으로 전달한다. */
    public static String current() {
        return TraceContext.get(CONTEXT_KEY).orElse(null);
    }
}
