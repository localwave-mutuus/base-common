package ai.mutuus.common.logging;

import java.util.function.Consumer;

/** optional TX 통합 경계. 기본 writer는 Spring TX/web 타입에 의존하지 않는다. */
@FunctionalInterface
public interface ActionCompletionHandler {
    enum Status { COMMITTED, ROLLED_BACK, NO_TRANSACTION, UNKNOWN }
    void complete(Consumer<Status> completion);
}
