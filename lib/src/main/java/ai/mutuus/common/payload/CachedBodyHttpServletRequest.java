package ai.mutuus.common.payload;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

/**
 * 요청 본문을 메모리에 캐싱해 <b>여러 번 읽을 수 있게</b> 하는 래퍼.
 * <p>필터가 진입 시점에 본문을 로깅하면서도(읽으면서) 컨트롤러가 동일 본문을 다시 읽을 수 있도록 한다.
 * {@link PayloadLoggingFilter} 가 본문 로깅 대상일 때만 사용한다.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 생성 시점에 원본 스트림을 한 번 모두 읽어 바이트로 보관한다.
        // 이후 getInputStream()/getReader() 는 이 바이트에서 새 스트림을 만들어 몇 번이든 다시 읽게 한다.
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /** 캐싱된 원본 바이트(로깅용). */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = getCharacterEncoding() != null
                ? Charset.forName(getCharacterEncoding()) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cachedBody), charset));
    }

    /** 캐싱된 바이트를 다시 읽기 위한 ServletInputStream. */
    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        private CachedBodyServletInputStream(byte[] body) {
            this.buffer = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("비동기 읽기는 지원하지 않는다(캐싱 본문).");
        }
    }
}
