package ai.mutuus.common.persistence;

import java.util.Optional;

import ai.mutuus.common.core.TraceContext;
import org.springframework.data.domain.AuditorAware;

/**
 * 감사 주체(생성/수정자)를 {@link TraceContext} 의 인증 사용자({@code X-User-Id})에서 가져온다.
 * <p>인증 확정 후 {@code AuthenticatedUserContextFilter} 가 신뢰 가능한 사용자ID 를 TraceContext 에
 * 넣으므로, {@link BaseEntity} 의 {@code createdBy}/{@code updatedBy} 가 자동으로 그 값을 갖는다.
 * 사용자 식별이 없으면(배치/익명) 빈 값을 반환해 감사 컬럼을 비운다.
 */
public class TraceContextAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(TraceContext.userId());
    }
}
