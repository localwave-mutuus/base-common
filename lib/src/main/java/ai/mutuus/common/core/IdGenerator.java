package ai.mutuus.common.core;

import java.util.UUID;

/**
 * 트랜잭션 글로벌 ID 및 단일 구간 ID 생성기.
 * <p>추적ID는 하이픈 없는 32자 UUID, span ID는 16자 단축 형태를 사용한다.
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    /** 트랜잭션 글로벌 추적 ID (UUID, 하이픈 제거). */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 구간(span) ID — 추적ID 하위. */
    public static String newSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 일반 엔터티 식별자. */
    public static UUID newId() {
        return UUID.randomUUID();
    }
}
