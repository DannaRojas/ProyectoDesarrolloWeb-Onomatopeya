package com.proyecto.inicio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenApiCustomizer autenticacionBearer() {
        return api -> {
            if (api.getComponents() == null) api.setComponents(new Components());
            api.getComponents().addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
            api.getPaths().forEach((ruta, path) ->
                    path.readOperationsMap().forEach((metodo, operacion) -> {
                        boolean publica = metodo == PathItem.HttpMethod.POST
                                && (ruta.equals("/auth/login") || ruta.equals("/empresas")
                                        || ruta.equals("/usuarios/invitaciones/aceptar"));
                        if (!publica) operacion.addSecurityItem(
                                new SecurityRequirement().addList("bearerAuth"));
                    }));
        };
    }

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
