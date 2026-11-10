package com.ecomm.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI ecommUserOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("E-Commerce User Service API")
                .version("1.0.0")
                .description("Persona-aware User Profile, Customer Address Book, Seller Storefront, and Admin User Management Microservice. "
                    + "Obtain a RS256 JWT access token from ecomm-auth-service (POST /api/v1/auth/login) and click 'Authorize' above.")
                .contact(new Contact()
                    .name("E-Commerce Platform Engineering")
                    .email("support@ecomm.com")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Paste the RS256 JWT accessToken obtained from ecomm-auth-service")));
    }
}

