package ai.mutuus.batch;

import java.util.Locale;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.IdGenerator;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.i18n.MessageResolver;
import org.springframework.stereotype.Component;

/**
 * 비웹 배치가 라이브러리의 비웹 기능(추적 컨텍스트·ID 생성·i18n)을 그대로 쓰는 예시.
 * <p>웹 인입 필터({@code TraceFilter})가 없으므로 배치는 작업 시작 시 추적 컨텍스트를 직접 채운다.
 */
@Component
public class BatchTraceJob {

    private final MessageResolver messages;

    public BatchTraceJob(MessageResolver messages) {
        this.messages = messages;
    }

    /** 새 추적ID를 발급해 컨텍스트에 적재하고, 로케일별 i18n 메시지를 해석해 돌려준다. */
    public String run(Locale locale) {
        TraceContext.put(HeaderNames.TRACE_ID, IdGenerator.newTraceId());
        return messages.get(locale, "error.not.found");
    }
}
