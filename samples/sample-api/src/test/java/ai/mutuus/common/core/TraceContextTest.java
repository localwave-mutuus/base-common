package ai.mutuus.common.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void 임의로_설정한_traceId_userId는_무시한다() {
        TraceContext.put(HeaderNames.TRACE_ID, "t-1");
        TraceContext.put(HeaderNames.USER_ID, "u-9");

        assertThat(TraceContext.traceId()).isNull();
        assertThat(TraceContext.userId()).isNull();
        assertThat(TraceContext.get(HeaderNames.TRACE_ID)).isEmpty();
    }

    @Test
    void 미설정_키는_빈_Optional이고_단축접근자는_null이다() {
        assertThat(TraceContext.get(HeaderNames.SCREEN_ID)).isEmpty();
        assertThat(TraceContext.traceId()).isNull();
    }

    @Test
    void null_값은_저장하지_않는다() {
        TraceContext.put(HeaderNames.SPAN_ID, null);
        assertThat(TraceContext.get(HeaderNames.SPAN_ID)).isEmpty();
    }

    @Test
    void snapshot은_불변_복사본이다() {
        TraceContext.put(HeaderNames.SCREEN_ID, "screen-1");
        var snapshot = TraceContext.snapshot();

        assertThat(snapshot).containsEntry(HeaderNames.SCREEN_ID, "screen-1");
        assertThatThrownBy(() -> snapshot.put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void clear_후에는_컨텍스트가_비워진다() {
        TraceContext.put(HeaderNames.TRACE_ID, "t-1");
        TraceContext.clear();
        assertThat(TraceContext.traceId()).isNull();
    }
}
