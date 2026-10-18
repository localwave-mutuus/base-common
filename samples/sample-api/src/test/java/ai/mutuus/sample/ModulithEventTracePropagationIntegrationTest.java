package ai.mutuus.sample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.logging.TelemetryTestEvents;
import ai.mutuus.common.event.DomainEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>Modulith 비동기 이벤트 + TraceContext 전파</b> 회귀 가드. 모듈 간 이벤트를 {@code @ApplicationModuleListener}
 * (커밋 후·비동기·새 트랜잭션)로 수신할 때, 스레드 경계를 넘어도 common-platform 의 {@code TraceContextTaskDecorator}
 * 덕분에 {@code traceId} 가 <b>리스너 스레드에서 유지</b>되는지 검증한다(발행→리스너 로그 상관 보존).
 * <p>{@code @EnableAsync} 로 실제 비동기 실행을 강제한다. 전파가 안 되면 리스너 스레드의 {@code TraceContext.traceId()}
 * 는 null 이 된다.
 */
@SpringBootTest(properties = "mutuus.common.async.context-decorator-enabled=true") // agent 없는 소비자 실행기 전파 opt-in
@Import(ModulithEventTracePropagationIntegrationTest.TestCfg.class)
class ModulithEventTracePropagationIntegrationTest {

    static final AtomicReference<String> capturedTraceId = new AtomicReference<>();
    static final AtomicReference<String> listenerThread = new AtomicReference<>();
    static volatile CountDownLatch latch;

    @Autowired
    TxPublisher publisher;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void 모듈리스너_비동기_수신시_traceId가_전파된다() throws Exception {
        latch = new CountDownLatch(1);
        try (var span = TelemetryTestEvents.span("modulith-publish")) {

            publisher.publishInTransaction(); // 커밋 후 @ApplicationModuleListener(비동기) 실행

            assertThat(latch.await(5, TimeUnit.SECONDS)).as("async listener fired").isTrue();
            assertThat(capturedTraceId.get()).isEqualTo(span.traceId());
            assertThat(listenerThread.get()).isNotEqualTo(Thread.currentThread().getName()); // 실제 다른(비동기) 스레드
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAsync
    static class TestCfg {

        @Bean
        TxPublisher txPublisher(ApplicationEventPublisher publisher) {
            return new TxPublisher(publisher);
        }

        @Bean
        AsyncModuleListener asyncModuleListener() {
            return new AsyncModuleListener();
        }
    }

    /** 트랜잭션 내에서 도메인 이벤트 발행(커밋 후 @TransactionalEventListener 트리거). */
    static class TxPublisher {
        private final ApplicationEventPublisher publisher;

        TxPublisher(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        @Transactional
        public void publishInTransaction() {
            publisher.publishEvent(DomainEvent.of("Demo.Ping", "payload"));
        }
    }

    /** 다른 모듈 리스너 역할 — 비동기 수신 스레드에서 TraceContext 를 캡처. */
    static class AsyncModuleListener {
        @ApplicationModuleListener
        void on(DomainEvent<?> event) {
            capturedTraceId.set(TraceContext.traceId());
            listenerThread.set(Thread.currentThread().getName());
            latch.countDown();
        }
    }
}
