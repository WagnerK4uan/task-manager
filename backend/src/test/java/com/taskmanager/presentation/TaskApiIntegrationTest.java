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

    @Test
    @DisplayName("@spec:AC-034 a exclusão responde 204 sem corpo e a tarefa deixa de existir")
    void exclusaoResponde204EATarefaDeixaDeExistir() throws Exception {
        ObjectMapper json = new ObjectMapper();

        long excluida = criarTarefa("Tarefa a excluir AC-034");
        long mantida = criarTarefa("Tarefa a manter AC-034");

        HttpResponse<String> exclusao = requisicao("DELETE", "/api/tasks/" + excluida);
        assertThat(exclusao.statusCode()).isEqualTo(204);
        assertThat(exclusao.body()).isEmpty();

        HttpResponse<String> consulta = requisicao("GET", "/api/tasks/" + excluida);
        assertThat(consulta.statusCode()).isEqualTo(404);
        assertThat(json.readTree(consulta.body()).path("error").asString())
                .isEqualTo("TASK_NOT_FOUND");

        assertThat(requisicao("GET", "/api/tasks/" + mantida).statusCode()).isEqualTo(200);

        HttpResponse<String> listagem = requisicao("GET", "/api/tasks?title=AC-034");
        assertThat(listagem.statusCode()).isEqualTo(200);
        assertThat(listagem.body())
                .contains("Tarefa a manter AC-034")
                .doesNotContain("Tarefa a excluir AC-034");
    }

    @Test
    @DisplayName("@spec:AC-036 a segunda exclusão do mesmo id é 404 e deixa o servidor no mesmo estado")
    void segundaExclusaoDoMesmoIdResponde404() throws Exception {
        long id = criarTarefa("Tarefa excluída duas vezes AC-036");

        assertThat(requisicao("DELETE", "/api/tasks/" + id).statusCode()).isEqualTo(204);

        HttpResponse<String> segunda = requisicao("DELETE", "/api/tasks/" + id);
        assertThat(segunda.statusCode()).isEqualTo(404);

        JsonNode erro = new ObjectMapper().readTree(segunda.body());
        assertThat(erro.path("status").asInt()).isEqualTo(404);
        assertThat(erro.path("error").asString()).isEqualTo("TASK_NOT_FOUND");
        assertThat(erro.path("path").asString()).isEqualTo("/api/tasks/" + id);

        assertThat(requisicao("GET", "/api/tasks/" + id).statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("@spec:AC-037 o Allow da tarefa passa a incluir a exclusão e a coleção continua sem ela")
    void exclusaoEDaTarefaNaoDaColecao() throws Exception {
        long id = criarTarefa("Tarefa do Allow AC-037");

        HttpResponse<String> item = requisicao("POST", "/api/tasks/" + id, "{}");
        assertThat(item.statusCode()).isEqualTo(405);
        assertThat(item.headers().firstValue("Allow").orElseThrow())
                .contains("GET", "PUT", "DELETE");
        assertThat(new ObjectMapper().readTree(item.body()).path("error").asString())
                .isEqualTo("METHOD_NOT_ALLOWED");

        HttpResponse<String> colecao = requisicao("DELETE", "/api/tasks");
        assertThat(colecao.statusCode()).isEqualTo(405);
        assertThat(colecao.headers().firstValue("Allow").orElseThrow()).doesNotContain("DELETE");
    }

    private long criarTarefa(String titulo) throws Exception {
        String corpo =
                """
                {
                  "title": "%s",
                  "status": "PENDENTE",
                  "priority": "MEDIA"
                }
                """
                        .formatted(titulo);

        HttpResponse<String> criada = requisicao("POST", "/api/tasks", corpo);
        assertThat(criada.statusCode()).isEqualTo(201);
        return new ObjectMapper().readTree(criada.body()).path("id").asLong();
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
