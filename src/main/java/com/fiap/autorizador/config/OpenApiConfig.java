package com.fiap.cfontes0estapar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI estaparOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FIAP - Gestao de Estacionamento API")
                .description("Recebe eventos do simulador da garagem, aplica preco dinamico e apura faturamento.")
                .version("v1"));
    }
}
