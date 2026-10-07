package ai.mutuus.common.logging;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 바인딩 시점의 실제 TX afterCompletion. 중단된 외부 TX나 savepoint 결과를 추정하지 않는다. */
public final class TransactionActionCompletionHandler implements ActionCompletionHandler {
    @Override public void complete(Consumer<Status> completion) {
        try {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            notifySafely(completion, Status.NO_TRANSACTION); return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifySafely(completion, Status.UNKNOWN); return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            private final AtomicBoolean emitted = new AtomicBoolean();
            @Override public void afterCompletion(int status) {
                if (!emitted.compareAndSet(false, true)) return;
                if (status == STATUS_ROLLED_BACK) notifySafely(completion, Status.ROLLED_BACK);
                else if (status == STATUS_COMMITTED) notifySafely(completion, Status.COMMITTED);
                else notifySafely(completion, Status.UNKNOWN);
            }
        });
        } catch (Throwable ignored) {
            notifySafely(completion, Status.UNKNOWN);
            ActionLogger.completionFailed();
        }
    }

    private static void notifySafely(Consumer<Status> completion, Status status) {
        try { completion.accept(status); }
        catch (Throwable ignored) { ActionLogger.completionFailed(); }
    }
}
