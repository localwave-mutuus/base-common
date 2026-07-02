package ai.mutuus.sample.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DB 계층 가이드(260702.002.DB_LAYER_GUIDE.md) 3~4장 세 구조의 실증 데모.
 * <ul>
 *   <li>{@code /demo/db/routing} — ① 읽기/쓰기 라우팅(<b>라이브러리</b> 메커니즘)</li>
 *   <li>{@code /demo/db/multitx} — ② 데이터소스별 개별 TxManager(서비스 레벨)</li>
 *   <li>{@code /demo/db/xa} — ③ JTA/XA 원자적 커밋(서비스 레벨, Atomikos)</li>
 *   <li>{@code /demo/db/guide} — 구조 판별(결정 규칙) 요약</li>
 * </ul>
 * 실제 시연은 {@link DbDemoSupport}(앱 기본 DataSource 와 분리된 데모 전용 자원)가 담당한다.
 */
@RestController
@RequestMapping("/demo/db")
public class DbDemoController {

    private final DbDemoSupport support;

    public DbDemoController(DbDemoSupport support) {
        this.support = support;
    }

    /** ① 읽기/쓰기 라우팅 — 라이브러리 ReadWriteRoutingDataSource. */
    @GetMapping("/routing")
    public ApiResponse<Map<String, Object>> routing() {
        return ApiResponse.ok(support.routingDemo());
    }

    /** ② 데이터소스별 개별 TxManager — 독립 커밋/롤백(원자성 없음). */
    @GetMapping("/multitx")
    public ApiResponse<Map<String, Object>> multiTx() {
        return ApiResponse.ok(support.multiTxDemo());
    }

    /** ③ JTA/XA — 여러 DB 원자적 커밋/롤백(2PC, Atomikos). */
    @GetMapping("/xa")
    public ApiResponse<Map<String, Object>> xa() {
        return ApiResponse.ok(support.xaDemo());
    }

    /** 구조 판별(결정 규칙) — "두 DB를 동시에 커밋/롤백해야 하는가?" */
    @GetMapping("/guide")
    public ApiResponse<Map<String, Object>> guide() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("decisionRule", "두 DB를 동시에 커밋/롤백해야 하는가? → 아니오면 ①/②, 예면 ③");
        res.put("structures", List.of(
                Map.of("id", "① RoutingDataSource", "용도", "읽기/쓰기 분리·멀티테넌트",
                        "원자성", "단일", "수용", "라이브러리(opt-in)"),
                Map.of("id", "② 개별 TxManager", "용도", "독립 DB 각각",
                        "원자성", "DB별 개별", "수용", "서비스 레벨"),
                Map.of("id", "③ JTA/XA", "용도", "여러 DB 원자적 커밋",
                        "원자성", "전역(2PC)", "수용", "서비스 레벨(원자성 필수 소수만)")));
        res.put("note", "common-platform 은 ①의 라우팅 메커니즘만 optional 수용. ②③은 도메인·정책 종속이라 서비스 몫. "
                + "감사(BaseEntity)까지가 라이브러리의 올바른 DB 관여 수준.");
        return ApiResponse.ok(res);
    }
}
