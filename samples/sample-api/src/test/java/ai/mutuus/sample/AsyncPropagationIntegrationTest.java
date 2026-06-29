package ai.mutuus.sample;

import java.util.concurrent.CompletableFuture;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 라이브러리가 등록한 추적 전파 {@code TaskDecorator} 가 Spring Boot 의 실제
 * {@code applicationTaskExecutor} 에 자동 적용되어, {@code @Async} 경계 너머로도
 * 추적ID가 유지되는지(소비 서비스 무설정) 검증한다.
 */
@SpringBootTest
@Import(AsyncPropagationIntegrationTest.AsyncTestConfig.class)
class AsyncPropagationIntegrationTest {

    @Autowired
    AsyncWorker worker;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void Async_경계를_넘어도_traceId가_전파된다() throws Exception {
        TraceContext.put(HeaderNames.TRACE_ID, "t-e2e");

        CompletableFuture<String> future = worker.traceIdInAsyncThread();

        assertThat(future.get()).isEqualTo("t-e2e");
    }

    @EnableAsync
    @TestConfiguration
    static class AsyncTestConfig {
        @Bean
        AsyncWorker asyncWorker() {
            return new AsyncWorker();
        }
    }

    /** @Async 기본 실행기(applicationTaskExecutor)에서 추적ID를 읽어 돌려준다. */
    static class AsyncWorker {
        @Async
        CompletableFuture<String> traceIdInAsyncThread() {
            // 메서드 전체가 비동기 실행기 스레드에서 실행되므로, 여기서 읽는 traceId 는 그 스레드의 값이다.
            return CompletableFuture.completedFuture(TraceContext.traceId());
        }
    }
}
