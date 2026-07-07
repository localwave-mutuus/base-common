package ai.mutuus.common.idempotency;

/**
 * 멱등 키에 대해 저장하는 레코드. 처리 중(in-progress) 마커이거나, 완료된 응답 스냅샷(상태·타입·본문)이다.
 *
 * @param completed   완료 여부(false = 처리 중 마커)
 * @param status      완료 시 HTTP 상태코드
 * @param contentType 완료 시 응답 Content-Type
 * @param body        완료 시 응답 본문(바이트)
 */
public record IdempotencyRecord(boolean completed, String fingerprint, int status, String contentType, byte[] body) {

    /** 처리 시작을 표시하는 in-progress 마커. */
    public static IdempotencyRecord inProgress() {
        return inProgress(null);
    }

    public static IdempotencyRecord inProgress(String fingerprint) {
        return new IdempotencyRecord(false, fingerprint, 0, null, null);
    }

    /** 완료된 응답 스냅샷. */
    public static IdempotencyRecord completed(int status, String contentType, byte[] body) {
        return completed(null, status, contentType, body);
    }

    public static IdempotencyRecord completed(String fingerprint, int status, String contentType, byte[] body) {
        return new IdempotencyRecord(true, fingerprint, status, contentType, body);
    }
}
