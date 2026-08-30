package com.lapwise.lapwise_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lapwiseOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Lapwise API")
                .version("0.0.1")
                .description("Hand-written REST. Swagger UI is for exploring the API while it is still being built."));
    }
}
