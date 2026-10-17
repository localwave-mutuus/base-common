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

/** 실제 응답을 지연하지 않고 본문 앞부분만 제한된 크기로 보관한다. */
final class BoundedPayloadResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final int limit;
    private ServletOutputStream stream;
    private PrintWriter writer;
    private boolean drainingWriter;

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
                    if (captured.size() < limit) captured.write(value);
                }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                    delegate.write(bytes, offset, length);
                    captured.write(bytes, offset, Math.min(length, limit - captured.size()));
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
    }

    @Override public void reset() {
        flushWriter();
        super.reset();
        captured.reset();
        stream = null;
        writer = null;
    }

    @Override public void sendError(int status) throws IOException {
        flushWriter();
        super.sendError(status);
        captured.reset();
    }

    @Override public void sendError(int status, String message) throws IOException {
        flushWriter();
        super.sendError(status, message);
        captured.reset();
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
    byte[] capturedBody() { return captured.toByteArray(); }
}
