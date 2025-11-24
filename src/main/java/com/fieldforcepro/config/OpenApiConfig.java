package com.fieldforcepro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme sessionCookieScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.COOKIE)
            .name("SESSION");

        SecurityScheme basicAuthScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("basic");

        return new OpenAPI()
            .info(new Info().title("FieldForcePro API").version("v1"))
            .components(new Components()
                .addSecuritySchemes("sessionCookie", sessionCookieScheme)
                .addSecuritySchemes("basicAuth", basicAuthScheme))
            .addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
    }
}
