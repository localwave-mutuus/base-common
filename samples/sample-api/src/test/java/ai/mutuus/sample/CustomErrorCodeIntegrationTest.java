package ai.mutuus.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1-B(ErrorCode 인터페이스화) 검증 — 소비 서비스가 {@code ErrorCode} 인터페이스를 구현한
 * 자체 코드({@code SampleErrorCode})를 던져도 라이브러리 기본 코드({@code CommonErrorCode})와
 * <b>동일한 표준 봉투/흐름</b>으로 처리됨을 end-to-end 로 확인한다(상태·code·i18n·traceId).
 * <p>모두 permit-all 경로(/api/public, /demo)라 인증 없이 호출된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomErrorCodeIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    /** S1: 라이브러리 기본 코드 회귀 — 인터페이스화 후에도 CommonErrorCode 가 그대로 동작. */
    @Test
    void S1_라이브러리_기본코드_NOT_FOUND_회귀() throws Exception {
        mockMvc.perform(get("/api/public/boom").header("X-Locale", "ko"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.success").value(false));
    }

    /** S2+S4: 소비자 커스텀 코드 → 커스텀 status(409)/code + 표준 봉투(traceId/success/error). */
    @Test
    void S2_소비자_커스텀코드_409_표준봉투() throws Exception {
        mockMvc.perform(get("/demo/error/custom").header("X-Locale", "ko"))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-Trace-Id", org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.code").value("ORDER_ALREADY_SHIPPED"))
                .andExpect(jsonPath("$.error.code").value("ORDER_ALREADY_SHIPPED"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                // S3(ko): 커스텀 코드의 메시지가 소비자 번들(sample-messages_ko)에서 해석됨
                .andExpect(jsonPath("$.message").value("이미 배송이 시작된 주문입니다."));
    }

    /** S3(en): 소비자 커스텀 코드의 i18n 이 로케일별로 해석됨. */
    @Test
    void S3_소비자_커스텀코드_i18n_en() throws Exception {
        mockMvc.perform(get("/demo/error/custom").header("X-Locale", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_ALREADY_SHIPPED"))
                .andExpect(jsonPath("$.message").value("The order has already been shipped."));
    }
}
