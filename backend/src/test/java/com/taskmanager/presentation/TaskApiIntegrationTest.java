package com.taskmanager.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmanager.PostgresIntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
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

    @Test
    @DisplayName("@spec:AC-027 a substituição troca os cinco campos, mantém o createdAt e move o updatedAt")
    void substituicaoTrocaOsCincoCamposEPreservaAOrigem() throws Exception {
        ObjectMapper json = new ObjectMapper();

        String criacao =
                """
                {
                  "title": "Tarefa original",
                  "description": "Descrição original",
                  "status": "PENDENTE",
                  "priority": "BAIXA",
                  "dueDate": "2026-12-01"
                }
                """;

        HttpResponse<String> criada = requisicao("POST", "/api/tasks", criacao);
        assertThat(criada.statusCode()).isEqualTo(201);

        JsonNode antes = json.readTree(criada.body());
        long id = antes.path("id").asLong();
        Instant criadaEm = Instant.parse(antes.path("createdAt").asString());
        Instant alteradaEm = Instant.parse(antes.path("updatedAt").asString());

        String substituicao =
                """
                {
                  "title": "Tarefa substituída",
                  "description": "Descrição nova",
                  "status": "CONCLUIDA",
                  "priority": "ALTA",
                  "dueDate": "2027-03-10"
                }
                """;

        HttpResponse<String> resposta = requisicao("PUT", "/api/tasks/" + id, substituicao);
        assertThat(resposta.statusCode()).isEqualTo(200);

        JsonNode depois = json.readTree(resposta.body());
        assertThat(depois.path("id").asLong()).isEqualTo(id);
        assertThat(depois.path("title").asString()).isEqualTo("Tarefa substituída");
        assertThat(depois.path("description").asString()).isEqualTo("Descrição nova");
        assertThat(depois.path("status").asString()).isEqualTo("CONCLUIDA");
        assertThat(depois.path("priority").asString()).isEqualTo("ALTA");
        assertThat(depois.path("dueDate").asString()).isEqualTo("2027-03-10");
        assertThat(Instant.parse(depois.path("createdAt").asString())).isEqualTo(criadaEm);
        assertThat(Instant.parse(depois.path("updatedAt").asString())).isAfter(alteradaEm);

        HttpResponse<String> consulta = requisicao("GET", "/api/tasks/" + id);
        assertThat(consulta.statusCode()).isEqualTo(200);

        JsonNode gravada = json.readTree(consulta.body());
        assertThat(gravada.path("title").asString()).isEqualTo("Tarefa substituída");
        assertThat(gravada.path("description").asString()).isEqualTo("Descrição nova");
        assertThat(gravada.path("status").asString()).isEqualTo("CONCLUIDA");
        assertThat(gravada.path("priority").asString()).isEqualTo("ALTA");
        assertThat(gravada.path("dueDate").asString()).isEqualTo("2027-03-10");
        assertThat(Instant.parse(gravada.path("createdAt").asString())).isEqualTo(criadaEm);
        assertThat(Instant.parse(gravada.path("updatedAt").asString())).isAfter(alteradaEm);
    }

    private HttpResponse<String> requisicao(String metodo, String caminho) throws Exception {
        return HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create(urlDaApi(caminho)))
                                .method(metodo, HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> requisicao(String metodo, String caminho, String corpo)
            throws Exception {
        return HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create(urlDaApi(caminho)))
                                .header("Content-Type", "application/json")
                                .method(metodo, HttpRequest.BodyPublishers.ofString(corpo))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    private String urlDaApi(String caminho) {
        return "http://localhost:" + environment.getRequiredProperty("local.server.port") + caminho;
    }
}
