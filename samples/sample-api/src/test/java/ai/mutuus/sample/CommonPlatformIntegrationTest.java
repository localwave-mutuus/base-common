package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * common-platform 을 의존성으로만 추가한 소비 서비스에서
 * 라이브러리 자동구성이 실제로 활성화되는지 end-to-end 검증한다.
 * <p>인증은 실제 IdP 없이 {@link TestConfiguration}의 mock {@link JwtDecoder}로 대체한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CommonPlatformIntegrationTest.TestSecurityConfig.class)
class CommonPlatformIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    // --- 1) 추적: TraceFilter가 인입 X-Trace-Id를 보존하고 컨텍스트/응답헤더에 싣는다 ---
    @Test
    void 인입_traceId가_응답헤더와_컨텍스트에_전파된다() throws Exception {
        mockMvc.perform(get("/api/public/hello").header("X-Trace-Id", "trace-abc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-abc"))
                .andExpect(jsonPath("$.traceId").value("trace-abc"));
    }

    @Test
    void traceId가_없으면_새로_생성되어_응답헤더에_담긴다() throws Exception {
        mockMvc.perform(get("/api/public/hello"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"));
    }

    // --- 2) 예외: 전역 핸들러가 ApiResponse 오류 봉투 + traceId 를 반환 ---
    // --- 3) i18n: X-Locale 헤더에 따라 메시지가 로케일별로 해석됨 ---
    @Test
    void 비즈니스예외는_ApiResponse오류봉투로_변환되고_X_Locale로_다국어된다() throws Exception {
        mockMvc.perform(get("/api/public/boom").header("X-Locale", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found."))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.traceId").exists());

        mockMvc.perform(get("/api/public/boom").header("X-Locale", "ko"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다."));
    }

    // --- 검증 실패: VALIDATION_ERROR + fieldErrors ---
    @Test
    void 본문_검증_실패는_VALIDATION_ERROR와_fieldErrors를_반환한다() throws Exception {
        mockMvc.perform(post("/api/public/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("message"));
    }

    // --- 4) 보안: OAuth2 자원 서버 자동구성 + permit-all 설정 동작 ---
    @Test
    void 인증_없이_secure_엔드포인트_접근시_401() throws Exception {
        mockMvc.perform(get("/api/secure/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_JWT로_secure_엔드포인트_접근시_200() throws Exception {
        mockMvc.perform(get("/api/secure/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sub").value("user-1"));
    }

    @Test
    void permit_all_경로는_인증_없이_접근_가능() throws Exception {
        mockMvc.perform(get("/api/public/hello"))
                .andExpect(status().isOk());
    }

    /** 실제 IdP 호출 없이 토큰을 해석하는 테스트용 JwtDecoder. */
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!"valid-token".equals(token)) {
                    throw new BadJwtException("invalid token");
                }
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject("user-1")
                        .claim("roles", List.of("USER"))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build();
            };
        }
    }
}
