package ai.mutuus.common.core;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    @Test
    void traceId는_하이픈_없는_32자다() {
        String traceId = IdGenerator.newTraceId();
        assertThat(traceId).hasSize(32).doesNotContain("-");
    }

    @Test
    void spanId는_16자다() {
        assertThat(IdGenerator.newSpanId()).hasSize(16).doesNotContain("-");
    }

    @Test
    void 생성된_traceId는_충분히_고유하다() {
        var ids = IntStream.range(0, 1_000)
                .mapToObj(i -> IdGenerator.newTraceId())
                .collect(Collectors.toSet());
        assertThat(ids).hasSize(1_000);
    }
}
