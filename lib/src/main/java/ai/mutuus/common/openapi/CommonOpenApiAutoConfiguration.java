package ai.mutuus.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 공통 OpenAPI(springdoc) 설정 자동 구성 — <b>소비자가 springdoc 를 classpath 에 추가할 때만</b> 활성화된다
 * ({@code @ConditionalOnClass(OpenAPI)}). 공통 info(제목/설명/버전)와 <b>Bearer JWT 보안 스킴</b>을 담은
 * {@link OpenAPI} 빈을 제공하면 springdoc 이 이를 베이스로 각 컨트롤러 경로/스키마를 채운다.
 * 보안 스킴은 자원 서버(JWT)와 정합되어 Swagger UI 의 <i>Authorize</i> 버튼으로 토큰을 넣을 수 있다.
 * <p>소비자가 자체 {@code OpenAPI} 빈을 정의하면 {@code @ConditionalOnMissingBean} 으로 비켜선다.
 * 등록: {@code META-INF/spring/...AutoConfiguration.imports}.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(CommonOpenApiProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonOpenApiAutoConfiguration {

    /** 공통 OpenAPI 기본 문서(info + 선택적 Bearer JWT 보안 스킴). 소비자가 재정의하면 비활성. */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI commonOpenApi(CommonOpenApiProperties props,
                                 @Value("${mutuus.common.service-name:${spring.application.name:api}}") String serviceName) {
        String title = (props.getTitle() != null && !props.getTitle().isBlank())
                ? props.getTitle() : serviceName + " API";
        OpenAPI api = new OpenAPI().info(new Info()
                .title(title)
                .description(props.getDescription())
                .version(props.getVersion()));
        if (props.isBearerJwt()) {
            String scheme = "bearerAuth";
            api.components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("인증 MSA 가 발급한 JWT (Authorization: Bearer <token>)")))
                    .addSecurityItem(new SecurityRequirement().addList(scheme));
        }
        return api;
    }
}
