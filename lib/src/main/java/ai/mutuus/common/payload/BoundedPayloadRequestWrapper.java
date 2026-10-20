package ai.mutuus.common.payload;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.Charset;
/** Tee never preconsumes input; overflow discards the whole capture. */
final class BoundedPayloadRequestWrapper extends HttpServletRequestWrapper {
 private final int limit;private final ByteArrayOutputStream capture=new ByteArrayOutputStream();
 private boolean overflow;private ServletInputStream stream;private BufferedReader reader;
 BoundedPayloadRequestWrapper(HttpServletRequest r,int limit){super(r);this.limit=limit;}
 private ServletInputStream input()throws IOException{
  if(stream==null){var delegate=super.getInputStream();stream=new ServletInputStream(){
   public boolean isFinished(){return delegate.isFinished();} public boolean isReady(){return delegate.isReady();}
   public void setReadListener(ReadListener l){delegate.setReadListener(l);}
   public int read()throws IOException{int b=delegate.read();if(b>=0){if(!overflow && capture.size()<limit)capture.write(b);else{overflow=true;capture.reset();}}return b;}
   public int read(byte[] b,int o,int n)throws IOException{int count=delegate.read(b,o,n);if(count>0&&!overflow){if(count>limit-capture.size()){overflow=true;capture.reset();}else capture.write(b,o,count);}return count;}
  };}return stream;
 }
 public ServletInputStream getInputStream()throws IOException{if(reader!=null)throw new IllegalStateException("Reader selected");return input();}
 public BufferedReader getReader()throws IOException{if(stream!=null&&reader==null)throw new IllegalStateException("Stream selected");if(reader==null)reader=new BufferedReader(new InputStreamReader(input(),Charset.forName(getCharacterEncoding()==null?"UTF-8":getCharacterEncoding())));return reader;}
 boolean complete(){return stream!=null?stream.isFinished():getContentLengthLong()==0;}
 byte[] capturedBody(){return capture.toByteArray();}boolean overflow(){return overflow;}
}
