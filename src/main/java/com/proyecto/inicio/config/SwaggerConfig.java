package com.proyecto.inicio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("API Proyecto Onomatopeya")
                                .version("1.0.0")
                                .description(
                                        "Documentación interactiva de las APIs REST para el"
                                            + " proyecto Onomatopeya."));
    }
}
