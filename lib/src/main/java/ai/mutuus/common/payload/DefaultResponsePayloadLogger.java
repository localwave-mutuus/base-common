package ai.mutuus.common.payload;
/** Legacy facade always denies raw input; no implicit policy upgrade. */
public class DefaultResponsePayloadLogger implements ResponsePayloadLogger {
 public static final String LOGGER_NAME="ai.mutuus.common.payload";
 public DefaultResponsePayloadLogger() { }
 public DefaultResponsePayloadLogger(ai.mutuus.common.core.SensitiveDataMasker masker) { }
 @Override public void onResponse(jakarta.servlet.http.HttpServletRequest request,int status,String body) {new SafePayloadEventWriter().write(SafePayloadPolicy.Result.omitted("POLICY_DENIED"));}
}
