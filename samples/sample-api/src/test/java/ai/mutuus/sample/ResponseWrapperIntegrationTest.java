package ai.mutuus.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1-A(성공 응답 자동 래핑) 검증 — {@code response-wrapper.enabled=true}(application.yml)에서
 * 평범한 객체는 표준 봉투로 감싸지되, 이미 봉투/오류/프레임워크 응답은 <b>건드리지 않음</b>을 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResponseWrapperIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    /** W1: 평범한 객체(Map) 반환 → {code:OK, data:{...}} 표준 봉투로 자동 래핑. */
    @Test
    void W1_평범한객체_자동래핑() throws Exception {
        mockMvc.perform(get("/demo/wrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.wrapped").value(true))
                .andExpect(jsonPath("$.data.value").value(42))
                .andExpect(jsonPath("$.traceId").exists());
    }

    /** W2: 이미 ApiResponse 인 엔드포인트는 이중 래핑되지 않는다($.data.data 없음). */
    @Test
    void W2_이미_ApiResponse면_이중래핑_안됨() throws Exception {
        mockMvc.perform(get("/api/public/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.greeting").value("hello"))
                .andExpect(jsonPath("$.data.data").doesNotExist());
    }

    /** W3: 오류 응답(이미 ApiResponse)도 이중 래핑되지 않는다. */
    @Test
    void W3_오류응답_이중래핑_안됨() throws Exception {
        mockMvc.perform(get("/api/public/boom"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /** W4: springdoc OpenAPI 스펙은 제외 경로라 래핑되지 않는다(루트에 openapi 필드 유지). */
    @Test
    void W4_openapi_스펙은_래핑되지_않음() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    /** W5: 데모 UI 인프라(/demo/cases, raw 배열 반환)는 제외 경로라 그대로 — 래핑되면 index.html 이 깨진다. */
    @Test
    void W5_demo_cases_는_제외경로라_raw배열() throws Exception {
        mockMvc.perform(get("/demo/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$.code").doesNotExist());
    }
}
