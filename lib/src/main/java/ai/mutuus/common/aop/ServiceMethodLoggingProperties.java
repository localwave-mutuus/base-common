package ai.mutuus.common.aop;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("mutuus.common.service-logging")
public class ServiceMethodLoggingProperties {
 private boolean enabled, payloadEnabled;
 private boolean requireAnnotation = true;
 private List<String> includePackages=List.of(), includeMethods=List.of(), excludeMethods=List.of();
 public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
 public boolean isPayloadEnabled(){return payloadEnabled;} public void setPayloadEnabled(boolean v){payloadEnabled=v;}
 public boolean isRequireAnnotation(){return requireAnnotation;} public void setRequireAnnotation(boolean v){requireAnnotation=v;}
 public List<String> getIncludePackages(){return includePackages;} public void setIncludePackages(List<String> v){includePackages=List.copyOf(v);}
 public List<String> getIncludeMethods(){return includeMethods;} public void setIncludeMethods(List<String> v){includeMethods=List.copyOf(v);}
 public List<String> getExcludeMethods(){return excludeMethods;} public void setExcludeMethods(List<String> v){excludeMethods=List.copyOf(v);}
}
