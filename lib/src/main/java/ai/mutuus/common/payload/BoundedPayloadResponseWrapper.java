package ai.mutuus.common.payload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/** 실제 응답을 지연하지 않고 제한 크기까지 보관하며 초과 시 캡처 전체를 버린다. */
final class BoundedPayloadResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final int limit;
    private ServletOutputStream stream;
    private PrintWriter writer;
    private boolean drainingWriter;
    private boolean overflow;

    BoundedPayloadResponseWrapper(HttpServletResponse response, int limit) {
        super(response);
        this.limit = limit;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) throw new IllegalStateException("getWriter() already called");
        return outputStream();
    }

    private ServletOutputStream outputStream() throws IOException {
        if (stream == null) {
            ServletOutputStream delegate = super.getOutputStream();
            stream = new ServletOutputStream() {
                @Override public boolean isReady() { return delegate.isReady(); }
                @Override public void setWriteListener(WriteListener listener) { delegate.setWriteListener(listener); }
                @Override public void write(int value) throws IOException {
                    delegate.write(value);
                    if (!overflow && captured.size() < limit) captured.write(value);
                    else { overflow=true; captured.reset(); }
                }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                    delegate.write(bytes, offset, length);
                    if (!overflow && length <= limit-captured.size()) captured.write(bytes, offset, length);
                    else { overflow=true; captured.reset(); }
                }
                @Override public void flush() throws IOException {
                    if (!drainingWriter) delegate.flush();
                }
                @Override public void close() throws IOException { delegate.close(); }
            };
        }
        return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (stream != null && writer == null) throw new IllegalStateException("getOutputStream() already called");
        if (writer == null) writer = new PrintWriter(new OutputStreamWriter(outputStream(), Charset.forName(getCharacterEncoding())));
        return writer;
    }

    @Override public void flushBuffer() throws IOException {
        flushWriter();
        super.flushBuffer();
    }

    @Override public void resetBuffer() {
        flushWriter();
        super.resetBuffer();
        captured.reset();
        overflow=false;
    }

    @Override public void reset() {
        flushWriter();
        super.reset();
        captured.reset();
        overflow=false;
        stream = null;
        writer = null;
    }

    @Override public void sendError(int status) throws IOException {
        flushWriter();
        super.sendError(status);
        captured.reset();
        overflow=false;
    }

    @Override public void sendError(int status, String message) throws IOException {
        flushWriter();
        super.sendError(status, message);
        captured.reset();
        overflow=false;
    }

    void flushWriter() {
        // 인코더의 미출력 문자만 내보낸다. 관측 때문에 응답을 먼저 commit하지 않는다.
        drainingWriter = true;
        try {
            if (writer != null) writer.flush();
        } finally {
            drainingWriter = false;
        }
    }
    boolean overflow() { return overflow; }
    byte[] capturedBody() { return captured.toByteArray(); }
}
