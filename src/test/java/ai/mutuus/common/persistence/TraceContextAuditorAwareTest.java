package ai.mutuus.common.persistence;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 감사 주체 제공자가 TraceContext 의 인증 사용자에서 값을 가져오는지 검증.
 */
class TraceContextAuditorAwareTest {

    private final TraceContextAuditorAware auditorAware = new TraceContextAuditorAware();

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void TraceContext에_사용자가_있으면_그_값을_감사주체로_반환한다() {
        TraceContext.put(HeaderNames.USER_ID, "u-42");
        assertThat(auditorAware.getCurrentAuditor()).contains("u-42");
    }

    @Test
    void 사용자가_없으면_비어있다() {
        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }
}
