package com.fiap.autorizador.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI autorizadorOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FIAP - Mini Autorizador API")
                .description("Criacao de cartoes de beneficio, consulta de saldo e autorizacao de transacoes.")
                .version("v1"));
    }
}
