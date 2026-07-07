package ai.mutuus.common.core;

/**
 * 로그 필드 포맷 전환 모드.
 * <p>ECS(Elastic Common Schema) 전환을 위해 로거가 어떤 필드 이름 규약으로 구조화 필드를 남길지 결정한다.
 * <ul>
 *   <li>{@link #LEGACY} — 기존 필드({@code event}/{@code httpPath}/{@code durationMs} 등)만 남긴다(하위호환).</li>
 *   <li>{@link #ECS} — ECS 필드({@code event.action}/{@code url.path}/{@code event.duration} 등)만 남긴다.</li>
 *   <li>{@link #DUAL} — 둘 다 남긴다(전환기 기본값). 기존 대시보드를 보호하면서 ECS 로 이관한다.</li>
 * </ul>
 * MDC 스칼라 alias 는 필터에서 추가하고, 배열/숫자/ip 등 구조 필드는 로거 호출부가 이 모드에 따라 채운다.
 */
public enum LogFormat {

    LEGACY,
    ECS,
    DUAL;

    /** ECS 필드를 남길지(=ECS 또는 DUAL). */
    public boolean ecs() {
        return this != LEGACY;
    }

    /** 기존(legacy) 필드를 남길지(=LEGACY 또는 DUAL). */
    public boolean legacy() {
        return this != ECS;
    }
}
