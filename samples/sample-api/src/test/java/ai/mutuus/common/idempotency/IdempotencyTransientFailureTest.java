package ai.mutuus.common.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 소비자 관점 실제 공통 필터/저장소 검증. 서버·DB·Redis·외부 호출 없음. */
class IdempotencyTransientFailureTest {
    private final IdempotencyProperties props = new IdempotencyProperties();

    @Test
    void transientStatusesReleaseReservationAndSameKeyCanSucceedThenReplay() throws Exception {
        for (int status : new int[]{500, 502, 503, 504, 599, 408, 429}) {
            var store = new InMemoryIdempotencyStore(); var filter = new IdempotencyFilter(store, props);
            var calls = new AtomicInteger(); var failed = new MockHttpServletResponse();
            filter.doFilter(request(), failed, (req, res) -> {
                calls.incrementAndGet(); ((jakarta.servlet.http.HttpServletResponse) res).setStatus(status);
                res.getWriter().write("temporary-failure");
            });
            assertThat(failed.getStatus()).isEqualTo(status);
            assertThat(failed.getContentAsString()).isEqualTo("temporary-failure");
            assertThat(store.find("key")).isNull();
            var success = new MockHttpServletResponse();
            filter.doFilter(request(), success, (req, res) -> { calls.incrementAndGet(); res.getWriter().write("done"); });
            var replay = new MockHttpServletResponse();
            filter.doFilter(request(), replay, (req, res) -> calls.incrementAndGet());
            assertThat(calls.get()).isEqualTo(2);
            assertThat(replay.getHeader("Idempotent-Replayed")).isEqualTo("true");
            assertThat(replay.getContentAsString()).isEqualTo("done");
        }
    }

    @Test
    void definitiveBusinessFailuresStillReplayWithoutReexecution() throws Exception {
        for (int status : new int[]{400, 401, 403, 409, 422}) {
            var store = new InMemoryIdempotencyStore(); var filter = new IdempotencyFilter(store, props);
            var calls = new AtomicInteger();
            filter.doFilter(request(), new MockHttpServletResponse(), (req, res) -> {
                calls.incrementAndGet(); ((jakarta.servlet.http.HttpServletResponse) res).setStatus(status);
                res.getWriter().write("business-rejection");
            });
            var replay = new MockHttpServletResponse();
            filter.doFilter(request(), replay, (req, res) -> calls.incrementAndGet());
            assertThat(calls.get()).isEqualTo(1); assertThat(replay.getStatus()).isEqualTo(status);
            assertThat(replay.getHeader("Idempotent-Replayed")).isEqualTo("true");
        }
    }

    @Test
    void legacyCompletedTransientIsReleasedAtomicallyBeforeSameKeyExecution() throws Exception {
        var store = new InMemoryIdempotencyStore(); var filter = new IdempotencyFilter(store, props);
        store.complete("key", IdempotencyRecord.completed(null, 503, "application/json", new byte[0]), props.getTtl());
        var calls = new AtomicInteger(); var response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (req, res) -> calls.incrementAndGet());
        assertThat(response.getStatus()).isEqualTo(200); assertThat(calls.get()).isEqualTo(1);
        assertThat(store.find("key").status()).isEqualTo(200);
    }

    @Test
    void compareAndRemoveCannotDeleteAnotherRetryReservationOrCompletedReceipt() {
        var store = new InMemoryIdempotencyStore();
        var old = IdempotencyRecord.completed(null, 503, null, new byte[0]);
        store.complete("key", old, props.getTtl());
        assertThat(store.removeIfCompleted("key", old)).isTrue();
        assertThat(store.reserve("key", props.getTtl(), "same-route")).isTrue();
        assertThat(store.removeIfCompleted("key", old)).isFalse();
        assertThat(store.find("key").completed()).isFalse();
        var committed = IdempotencyRecord.completed("same-route", 200, null, new byte[0]);
        store.complete("key", committed, props.getTtl());
        assertThat(store.removeIfCompleted("key", old)).isFalse();
        assertThat(store.find("key")).isSameAs(committed);
    }

    @Test
    void customStoreWithoutAtomicLegacyRemovalRemainsFailClosed() throws Exception {
        var legacy = IdempotencyRecord.completed(null, 503, null, new byte[0]);
        IdempotencyStore unsupported = new IdempotencyStore() {
            public boolean reserve(String key, Duration ttl) { throw new AssertionError("Must not reserve"); }
            public IdempotencyRecord find(String key) { return legacy; }
            public void complete(String key, IdempotencyRecord record, Duration ttl) { throw new AssertionError(); }
            public void remove(String key) { throw new AssertionError("Unsafe delete must not run"); }
        };
        var response = new MockHttpServletResponse(); var calls = new AtomicInteger();
        new IdempotencyFilter(unsupported, props).doFilter(request(), response, (req, res) -> calls.incrementAndGet());
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getHeader("Idempotent-Replayed")).isEqualTo("transient-release-unconfirmed");
        assertThat(calls.get()).isZero();
    }

    @Test
    void unknownResponsePersistenceAfterBusinessReturnStillRetainsStoreFailClosedProtection() throws Exception {
        var protectedStore = new InMemoryIdempotencyStore() {
            private boolean completionUnknown;
            @Override public void complete(String key, IdempotencyRecord record, Duration ttl) {
                completionUnknown = true; throw new IllegalStateException("confirmation unavailable");
            }
            @Override public void remove(String key) { if (!completionUnknown) super.remove(key); }
        };
        var filter = new IdempotencyFilter(protectedStore, props); var calls = new AtomicInteger();
        assertThatThrownBy(() -> filter.doFilter(request(), new MockHttpServletResponse(),
                (req, res) -> calls.incrementAndGet())).isInstanceOf(IllegalStateException.class);
        var retry = new MockHttpServletResponse();
        filter.doFilter(request(), retry, (req, res) -> calls.incrementAndGet());
        assertThat(retry.getStatus()).isEqualTo(409);
        assertThat(retry.getHeader("Idempotent-Replayed")).isEqualTo("in-progress");
        assertThat(calls.get()).isEqualTo(1);
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("POST", "/owner/command");
        request.addHeader("Idempotency-Key", "key"); return request;
    }
}
