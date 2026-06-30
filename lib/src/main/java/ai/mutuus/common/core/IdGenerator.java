package ai.mutuus.common.core;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 트랜잭션 글로벌 ID 및 단일 구간 ID 생성기.
 * <p>추적ID는 하이픈 없는 32자 UUID, span ID는 16자 단축 형태를 사용한다.
 */
public final class IdGenerator {

    /** 코드 생성용 영숫자 집합(대문자+숫자). */
    private static final char[] ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private IdGenerator() {
    }

    /** 길이 {@code len} 의 무작위 영숫자 코드(대문자+숫자). 인스턴스구분코드 자동생성에 사용. */
    public static String randomAlnum(int len) {
        StringBuilder sb = new StringBuilder(len);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(ALNUM[r.nextInt(ALNUM.length)]);
        }
        return sb.toString();
    }

    /**
     * {@code seed}(예: 서비스명)에서 결정적으로 길이 {@code len} 의 영숫자 코드를 만든다.
     * 같은 seed 면 항상 같은 코드 → 어플리케이션코드 기본값 도출(미지정 시)에 사용.
     */
    public static String deriveAlnum(String seed, int len) {
        long h = 1125899906842597L;
        String s = (seed == null || seed.isBlank()) ? "default" : seed;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALNUM[(int) Math.floorMod(h, ALNUM.length)]);
            h = Math.floorDiv(h, ALNUM.length) ^ (h << 7);
        }
        return sb.toString();
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
