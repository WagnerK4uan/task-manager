package com.taskmanager.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String VERSAO_DA_API = "0.0.1";

    @Bean
    OpenAPI documentoDaApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Task Manager API")
                                .description(
                                        "Gerenciamento de tarefas: criação, listagem com busca por "
                                                + "título e status, consulta por id, atualização, "
                                                + "troca de status e exclusão.")
                                .version(VERSAO_DA_API));
    }
}
