package ai.mutuus.common.payload;
/** Legacy facade always denies raw input; no implicit policy upgrade. */
public class DefaultRequestPayloadLogger implements RequestPayloadLogger {
 public static final String LOGGER_NAME="ai.mutuus.common.payload";
 public DefaultRequestPayloadLogger() { }
 public DefaultRequestPayloadLogger(ai.mutuus.common.core.SensitiveDataMasker masker) { }
 public DefaultRequestPayloadLogger(ai.mutuus.common.core.SensitiveDataMasker masker,PayloadLoggingProperties properties) { }
 @Override public void onRequest(jakarta.servlet.http.HttpServletRequest request,String body) {new SafePayloadEventWriter().write(SafePayloadPolicy.Result.omitted("POLICY_DENIED"));}
}
