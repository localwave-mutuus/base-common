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
 * Phase 2-C(표준 페이징 응답) 검증 — {@code /demo/page} 가 {@code PageResponse} 로 페이지 메타를 일관 반환하고,
 * 성공 응답 자동 래핑과 결합되어 {@code {code:OK, data: PageResponse{...}}} 로 나감을 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PageResponseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 첫페이지_페이지메타_및_봉투결합() throws Exception {
        mockMvc.perform(get("/demo/page?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))              // response-wrapper 봉투
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.numberOfElements").value(2));
    }

    @Test
    void 마지막페이지는_last_true_이고_잔여건수() throws Exception {
        mockMvc.perform(get("/demo/page?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.numberOfElements").value(1));
    }
}
