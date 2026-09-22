package com.sim.backend.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
// 1. Swagger UI의 기본 정보를 설정합니다.
@OpenAPIDefinition(
        info = @Info(title = "Sim project", version = "v1", description = "心봤다"),
        // 2. 모든 API에 전역적으로 보안 요구사항(자물쇠 아이콘)을 추가합니다.
        security = @SecurityRequirement(name = "bearerAuth")
)
// 3. "bearerAuth"라는 이름의 보안 스킴(SecurityScheme)을 정의합니다.
@SecurityScheme(
        name = "bearerAuth", // SecurityRequirement에서 사용할 이름
        type = SecuritySchemeType.HTTP, // 인증 타입은 HTTP
        scheme = "bearer", // 스킴은 bearer 방식을 사용
        bearerFormat = "JWT" // 토큰 형식은 JWT
)
public class SwaggerConfig {
}