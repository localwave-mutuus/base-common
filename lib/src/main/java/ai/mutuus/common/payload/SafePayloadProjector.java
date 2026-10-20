package ai.mutuus.common.payload;
import java.util.Map;
@FunctionalInterface public interface SafePayloadProjector { Map<String,Object> project(ApprovedInput input); }
