package ai.mutuus.common.payload;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
/** Legacy configuration is not approval for raw body capture. Use safe-payload with registered rules. */
public class PayloadLoggingFilter extends OncePerRequestFilter {
 public PayloadLoggingFilter(RequestPayloadLogger input,ResponsePayloadLogger output,PayloadLoggingProperties properties) { }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  chain.doFilter(request,response);
 }
}
