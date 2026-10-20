package ai.mutuus.common.payload;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.web.*;
import java.nio.charset.StandardCharsets;
class BoundedPayloadCaptureTest {
 @Test void utf8TeeBoundariesNeverChangeRequestOrResponse()throws Exception{
  for(int size:new int[]{8191,8192,8193}){
   byte[] bytes=("가".repeat(size/3)+"x".repeat(size%3)).getBytes(StandardCharsets.UTF_8);assertEquals(size,bytes.length);
   var request=new MockHttpServletRequest();request.setContent(bytes);var input=new BoundedPayloadRequestWrapper(request,8192);
   assertArrayEquals(bytes,input.getInputStream().readAllBytes());assertEquals(size>8192,input.overflow());assertEquals(size>8192?0:size,input.capturedBody().length);
   var response=new MockHttpServletResponse();var output=new BoundedPayloadResponseWrapper(response,8192);output.getOutputStream().write(bytes);
   assertArrayEquals(bytes,response.getContentAsByteArray());assertEquals(size>8192,output.overflow());assertEquals(size>8192?0:size,output.capturedBody().length);
  }
 }
}
