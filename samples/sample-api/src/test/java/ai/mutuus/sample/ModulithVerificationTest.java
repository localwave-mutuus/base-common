package ai.mutuus.sample;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 모듈(=바운디드 컨텍스트) 경계 검증. base package({@code ai.mutuus.sample}) 직속 하위 패키지가
 * 모듈로 인식되며, 모듈 간 <b>사이클·내부 침범</b>이 없어야 통과한다. 모듈 정책은 각 모듈 {@code package-info.java}
 * 의 {@code @ApplicationModule} 로 선언한다(쇼케이스 샘플은 점진 도입을 위해 일부 OPEN).
 */
class ModulithVerificationTest {

    @Test
    void 모듈_경계를_검증한다() {
        ApplicationModules modules = ApplicationModules.of(SampleApiApplication.class);
        modules.forEach(m -> System.out.println("[module] " + m.getDisplayName()));
        modules.verify();
    }
}
