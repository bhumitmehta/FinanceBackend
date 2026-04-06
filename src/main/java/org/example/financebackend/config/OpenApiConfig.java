package org.example.financebackend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        description = "Paste the JWT token obtained from POST /api/auth/login — no 'Bearer ' prefix needed here"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Backend API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for managing financial records, users, and analytics dashboards.
                                
                                **Roles:** ADMIN · ANALYST · VIEWER
                                
                                Most endpoints require a JWT Bearer token. Obtain one via `POST /api/auth/login`.
                                Click **Authorize** (top-right), paste the token, then re-send any request.
                                """)
                        .contact(new Contact()
                                .name("Finance Backend")
                                .email("admin@finance.local")))
                // Apply bearerAuth globally so every locked endpoint shows the padlock icon
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
