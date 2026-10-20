package ai.mutuus.common.payload;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("mutuus.common.safe-payload")
public class SafePayloadProperties {
 public enum Mode { ERROR_ONLY, ALL }
 private boolean enabled;private Mode mode=Mode.ERROR_ONLY;
 private int maxBodyBytes=8192,maxEventBytes=32768,maxDepth=4,maxFields=32;
 public boolean isEnabled(){return enabled;}public void setEnabled(boolean v){enabled=v;}
 public Mode getMode(){return mode;}public void setMode(Mode v){mode=java.util.Objects.requireNonNull(v);}
 public int getMaxBodyBytes(){return maxBodyBytes;}public void setMaxBodyBytes(int v){maxBodyBytes=bound(v,8192);}
 public int getMaxEventBytes(){return maxEventBytes;}public void setMaxEventBytes(int v){if(v<2048)throw new IllegalArgumentException("Required envelope needs 2048 bytes");maxEventBytes=bound(v,32768);}
 public int getMaxDepth(){return maxDepth;}public void setMaxDepth(int v){maxDepth=bound(v,4);}
 public int getMaxFields(){return maxFields;}public void setMaxFields(int v){maxFields=bound(v,32);}
 private int bound(int v,int max){if(v<1 || v>max)throw new IllegalArgumentException("Safe payload limit exceeded");return v;}
}
