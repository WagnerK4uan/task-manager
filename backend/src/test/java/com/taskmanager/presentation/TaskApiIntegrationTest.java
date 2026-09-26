package com.taskmanager.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmanager.PostgresIntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TaskApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired private Environment environment;

    @Test
    @DisplayName("@spec:AC-019 o Location devolvido na criação responde 200 com a mesma tarefa")
    void locationDevolvidoNaCriacaoResponde200() throws Exception {
        HttpClient cliente = HttpClient.newHttpClient();
        String corpo =
                """
                {
                  "title": "Seguir o Location",
                  "description": "Criação e consulta concordam sobre onde a tarefa mora",
                  "status": "PENDENTE",
                  "priority": "MEDIA",
                  "dueDate": "2026-12-01"
                }
                """;

        HttpResponse<String> criacao =
                cliente.send(
                        HttpRequest.newBuilder(URI.create(urlDaApi("/api/tasks")))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(criacao.statusCode()).isEqualTo(201);

        String location = criacao.headers().firstValue("Location").orElseThrow();
        assertThat(location).startsWith("/api/tasks/");

        ObjectMapper json = new ObjectMapper();
        long idCriado = json.readTree(criacao.body()).path("id").asLong();

        HttpResponse<String> consulta =
                cliente.send(
                        HttpRequest.newBuilder(URI.create(urlDaApi(location))).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(consulta.statusCode()).isEqualTo(200);

        JsonNode tarefa = json.readTree(consulta.body());
        assertThat(tarefa.path("id").asLong()).isEqualTo(idCriado);
        assertThat(tarefa.path("title").asString()).isEqualTo("Seguir o Location");
        assertThat(tarefa.path("status").asString()).isEqualTo("PENDENTE");
    }

    @Test
    @DisplayName("@spec:AC-025 endereço que a API não publica responde 404 RESOURCE_NOT_FOUND")
    void enderecoQueApiNaoPublicaResponde404() throws Exception {
        HttpResponse<String> resposta = requisicao("GET", "/api/rota-que-nao-existe");

        assertThat(resposta.statusCode()).isEqualTo(404);

        JsonNode erro = new ObjectMapper().readTree(resposta.body());
        assertThat(erro.path("status").asInt()).isEqualTo(404);
        assertThat(erro.path("error").asString()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(erro.path("path").asString()).isEqualTo("/api/rota-que-nao-existe");
        assertThat(erro.path("timestamp").asString()).endsWith("Z");
        assertThat(resposta.body()).doesNotContain("com.taskmanager");
    }

    @Test
    @DisplayName("@spec:AC-026 método não suportado responde 405 e diz o que a rota aceita")
    void metodoNaoSuportadoResponde405ComAllow() throws Exception {
        HttpResponse<String> resposta = requisicao("DELETE", "/api/tasks");

        assertThat(resposta.statusCode()).isEqualTo(405);
        assertThat(resposta.headers().firstValue("Allow").orElseThrow()).contains("GET", "POST");

        JsonNode erro = new ObjectMapper().readTree(resposta.body());
        assertThat(erro.path("status").asInt()).isEqualTo(405);
        assertThat(erro.path("error").asString()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(erro.path("path").asString()).isEqualTo("/api/tasks");
    }

    private HttpResponse<String> requisicao(String metodo, String caminho) throws Exception {
        return HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create(urlDaApi(caminho)))
                                .method(metodo, HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    private String urlDaApi(String caminho) {
        return "http://localhost:" + environment.getRequiredProperty("local.server.port") + caminho;
    }
}
