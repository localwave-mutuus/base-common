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
 * Phase 2-B(OpenAPI 공통 설정 라이브러리 승격) 검증 — 소비자가 springdoc 를 추가하면 라이브러리의
 * {@code CommonOpenApiAutoConfiguration} 이 공통 info 와 Bearer JWT 보안 스킴을 주입하고, springdoc 이
 * 이를 베이스로 스펙을 생성함을 {@code /v3/api-docs} 응답으로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiCommonConfigIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 공통_info와_BearerJWT_보안스킴이_OpenAPI_스펙에_주입된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // service-name(sample-api) 기반 공통 제목
                .andExpect(jsonPath("$.info.title").value("sample-api API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                // Bearer JWT 보안 스킴(자원 서버와 정합) — Swagger UI Authorize
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                // 전역 보안 요구사항
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }
}
