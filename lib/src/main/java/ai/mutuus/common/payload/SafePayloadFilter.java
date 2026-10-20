package ai.mutuus.common.payload;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
@Order(Ordered.HIGHEST_PRECEDENCE+20)
public final class SafePayloadFilter extends OncePerRequestFilter {
 private final SafePayloadPolicy policy;private final SafePayloadProperties props;
 SafePayloadFilter(SafePayloadPolicy policy,SafePayloadProperties props){this.policy=policy;this.props=props;}
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
  var input=new BoundedPayloadRequestWrapper(request,props.getMaxBodyBytes());
  var output=new BoundedPayloadResponseWrapper(response,props.getMaxBodyBytes());boolean failure=false;
  try{chain.doFilter(input,output);}catch(IOException|ServletException|RuntimeException|Error ex){failure=true;throw ex;}
  finally{
   output.flushWriter();failure=failure||response.getStatus()>=400;
   // Async lifetime belongs to the completion boundary; never capture partial async output.
   if(!request.isAsyncStarted() && (failure || props.getMode()==SafePayloadProperties.Mode.ALL)){
    Object route=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    var writer=new SafePayloadEventWriter();
    writer.write(!json(request.getContentType())?SafePayloadPolicy.Result.omitted("POLICY_DENIED"):
     !input.overflow() && !input.complete()?SafePayloadPolicy.Result.omitted("PARSE_ERROR"):
     policy.http(request.getMethod(),route instanceof String s?s:"/{unmatched}",HttpPayloadRule.Direction.INPUT,input.capturedBody(),input.overflow(),failure));
    writer.write(!json(output.getContentType())?SafePayloadPolicy.Result.omitted("POLICY_DENIED"):
     policy.http(request.getMethod(),route instanceof String s?s:"/{unmatched}",HttpPayloadRule.Direction.OUTPUT,output.capturedBody(),output.overflow(),failure));
   }
  }
 }
 private static boolean json(String contentType){return contentType!=null && contentType.split(";",2)[0].trim().equalsIgnoreCase("application/json");}
}
