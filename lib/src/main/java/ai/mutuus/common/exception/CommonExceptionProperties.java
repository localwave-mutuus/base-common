package ai.mutuus.common.exception;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 예외 처리 설정. {@code mutuus.common.exception.*}.
 */
@ConfigurationProperties(prefix = "mutuus.common.exception")
public class CommonExceptionProperties {

    /**
     * 오류 응답 본문에 내부 예외 클래스명({@code error.exception})을 노출할지. <b>운영 안전 기본값 false</b>
     * (프레임워크/스택 정보 노출 방지 — 예외 클래스·스택은 서버 로그에만 남는다). 개발 편의로만 true 권장.
     */
    private boolean exposeException = false;

    public boolean isExposeException() {
        return exposeException;
    }

    public void setExposeException(boolean exposeException) {
        this.exposeException = exposeException;
    }
}
