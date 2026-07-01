package ai.mutuus.common.web;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link HttpRetryInterceptor} 재시도 로직 검증(멱등만·IOException·최대시도). */
class HttpRetryInterceptorTest {

    @Test
    void GET는_IOException시_재시도하고_성공하면_응답을_반환한다() throws IOException {
        var exec = mock(ClientHttpRequestExecution.class);
        var resp = mock(ClientHttpResponse.class);
        when(exec.execute(any(), any())).thenThrow(new IOException("conn")).thenReturn(resp);
        var req = mock(HttpRequest.class);
        when(req.getMethod()).thenReturn(HttpMethod.GET);

        ClientHttpResponse result = new HttpRetryInterceptor(2).intercept(req, new byte[0], exec);

        assertThat(result).isSameAs(resp);
        verify(exec, times(2)).execute(any(), any());
    }

    @Test
    void POST는_재시도하지_않고_바로_예외를_던진다() throws IOException {
        var exec = mock(ClientHttpRequestExecution.class);
        when(exec.execute(any(), any())).thenThrow(new IOException("conn"));
        var req = mock(HttpRequest.class);
        when(req.getMethod()).thenReturn(HttpMethod.POST);

        assertThatThrownBy(() -> new HttpRetryInterceptor(3).intercept(req, new byte[0], exec))
                .isInstanceOf(IOException.class);
        verify(exec, times(1)).execute(any(), any());
    }

    @Test
    void 최대시도_소진시_마지막_예외를_던진다() throws IOException {
        var exec = mock(ClientHttpRequestExecution.class);
        when(exec.execute(any(), any())).thenThrow(new IOException("conn"));
        var req = mock(HttpRequest.class);
        when(req.getMethod()).thenReturn(HttpMethod.GET);

        assertThatThrownBy(() -> new HttpRetryInterceptor(3).intercept(req, new byte[0], exec))
                .isInstanceOf(IOException.class);
        verify(exec, times(3)).execute(any(), any());
    }
}
