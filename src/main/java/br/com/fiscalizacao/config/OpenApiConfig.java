package br.com.fiscalizacao.config;

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
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Report Service API")
                        .description("""
                                Serviço de registro e gestão de ocorrências urbanas.

                                Permite que **cidadãos** registrem problemas de infraestrutura pública e que \
                                **agentes da prefeitura** acompanhem, priorizem e atualizem o status das ocorrências.

                                ### Autenticação
                                Endpoints protegidos exigem um token JWT no header `Authorization: Bearer <token>`.

                                ### Perfis de acesso
                                - `ROLE_CIDADAO` — registro de ocorrências e consulta das próprias ocorrências
                                - `ROLE_AGENTE_PREFEITURA` — listagem completa e atualização de status
                                """)
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
