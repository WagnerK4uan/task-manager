package com.taskmanager;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ProjectSetupTest extends PostgresIntegrationTest {

    private static final Map<String, String> CONTRATO_DE_DADOS = Map.of(
            "id", "bigint NOT NULL",
            "title", "character varying(120) NOT NULL",
            "description", "character varying(2000) NULL",
            "status", "character varying(20) NOT NULL",
            "priority", "character varying(20) NOT NULL",
            "due_date", "date NULL",
            "created_at", "timestamp with time zone NOT NULL",
            "updated_at", "timestamp with time zone NOT NULL");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("@spec:AC-001 contexto sobe e a conexão com o PostgreSQL responde")
    void contextoSobeEAConexaoComOBancoResponde() throws Exception {
        try (Connection conexao = dataSource.getConnection()) {
            assertThat(conexao.isValid(5)).isTrue();
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");

            try (Statement consulta = conexao.createStatement();
                    ResultSet resultado = consulta.executeQuery("select 1")) {
                assertThat(resultado.next()).isTrue();
                assertThat(resultado.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    @DisplayName("@spec:AC-002 migrations criam a tabela tasks com o contrato de dados da spec")
    void migrationsCriamATabelaTasks() throws Exception {
        Map<String, String> colunas = new LinkedHashMap<>();
        String sql =
                """
                select column_name, data_type, character_maximum_length, is_nullable
                  from information_schema.columns
                 where table_schema = 'public' and table_name = 'tasks'
                 order by ordinal_position
                """;

        try (Connection conexao = dataSource.getConnection();
                Statement consulta = conexao.createStatement();
                ResultSet resultado = consulta.executeQuery(sql)) {
            while (resultado.next()) {
                colunas.put(resultado.getString("column_name"), descrever(resultado));
            }
        }

        assertThat(colunas).containsExactlyInAnyOrderEntriesOf(CONTRATO_DE_DADOS);
    }

    @Test
    @DisplayName("@spec:AC-003 schema vem das migrations e o Hibernate fica em validate")
    void schemaVemDasMigrationsComHibernateEmValidate() throws Exception {
        assertThat(entityManagerFactory.getProperties())
                .containsEntry("hibernate.hbm2ddl.auto", "validate");

        try (Connection conexao = dataSource.getConnection();
                Statement consulta = conexao.createStatement();
                ResultSet resultado =
                        consulta.executeQuery(
                                "select count(*) from flyway_schema_history where success")) {
            assertThat(resultado.next()).isTrue();
            assertThat(resultado.getInt(1))
                    .as("o histórico do Flyway prova quem construiu o schema")
                    .isPositive();
        }
    }

    @Test
    @DisplayName("@spec:AC-004 /v3/api-docs publica o documento OpenAPI que identifica a API")
    void apiDocsPublicaODocumentoOpenApi() throws Exception {
        HttpResponse<String> resposta =
                HttpClient.newHttpClient()
                        .send(
                                HttpRequest.newBuilder(URI.create(urlDaApi("/v3/api-docs")))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());

        assertThat(resposta.statusCode()).isEqualTo(200);

        JsonNode documento = new ObjectMapper().readTree(resposta.body());
        assertThat(documento.path("openapi").asString()).startsWith("3.");
        assertThat(documento.path("info").path("title").asString())
                .as("o título identifica a API, não o padrão do springdoc")
                .contains("Task Manager");
        assertThat(documento.path("info").path("version").asString())
                .as("a versão é a do projeto, não o padrão do springdoc")
                .isNotBlank()
                .isNotEqualTo("v0");
    }

    private String urlDaApi(String caminho) {
        return "http://localhost:" + environment.getRequiredProperty("local.server.port") + caminho;
    }

    /** Monta "tipo(tamanho) NULL|NOT NULL" para comparar com o contrato da spec. */
    private static String descrever(ResultSet coluna) throws Exception {
        String tipo = coluna.getString("data_type");
        int tamanho = coluna.getInt("character_maximum_length");
        if (!coluna.wasNull()) {
            tipo = tipo + "(" + tamanho + ")";
        }
        return tipo + ("YES".equals(coluna.getString("is_nullable")) ? " NULL" : " NOT NULL");
    }
}
