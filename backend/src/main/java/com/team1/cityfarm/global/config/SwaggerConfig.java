package com.team1.cityfarm.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // 1. SecurityScheme 이름 설정
        String securitySchemeName = "BearerAuth";

        // 2. SecurityRequirement 정의 (모든 API 요청 시 해당 헤더 적용)
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(securitySchemeName);

        // 3. Components에 SecurityScheme 등록 (Authorization: Bearer <JWT> 형태)
        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력해 주세요. (Bearer 접두사는 자동으로 붙습니다.)"));

        return new OpenAPI()
                .info(new Info()
                        .title("CityFarm API Documentation")
                        .description("도심 속 텃밭, 시티팜의 백엔드 API 명세서입니다.")
                        .version("v1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}