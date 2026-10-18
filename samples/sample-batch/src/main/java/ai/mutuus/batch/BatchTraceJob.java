package ai.mutuus.batch;

import java.util.Locale;

import ai.mutuus.common.logging.JobPairLogger;
import ai.mutuus.common.i18n.MessageResolver;
import org.springframework.stereotype.Component;

/**
 * 비웹 배치가 공통 작업 범위와 i18n을 쓰는 예시.
 * <p>배포 agent가 SDK를 공급한다. 추적 ID를 직접 생성하지 않는다.
 */
@Component
public class BatchTraceJob {

    private final MessageResolver messages;

    public BatchTraceJob(MessageResolver messages) {
        this.messages = messages;
    }

    /** 독립 작업 범위를 닫고 로케일별 i18n 메시지를 돌려준다. */
    public String run(Locale locale) {
        try (var scope = new JobPairLogger().begin("batch.trace.example", null)) {
            String message = messages.get(locale, "error.not.found");
            scope.success();
            return message;
        }
    }
}
