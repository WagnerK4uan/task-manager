package com.taskmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmanager.PostgresIntegrationTest;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskPriority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.repository.TaskRepository;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prova o mapeamento da entidade contra o PostgreSQL das migrations. Nada aqui roda em transação
 * com rollback: a gravação commita de verdade, que é o que permite conferir a linha por SQL nativo.
 */
class TaskPersistenceTest extends PostgresIntegrationTest {

    @Autowired private TaskRepository repositorio;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparTabela() {
        jdbc.update("delete from tasks");
    }

    @Test
    @DisplayName("@spec:AC-008 a tarefa gravada volta íntegra do banco")
    void tarefaGravadaVoltaIntegraDoBanco() {
        Task gravada =
                repositorio.gravar(
                        new Task(
                                "Escrever a spec",
                                "Critérios de aceite em Dado/Quando/Então",
                                TaskStatus.PENDENTE,
                                TaskPriority.MEDIA,
                                LocalDate.of(2026, 10, 15)));

        assertThat(gravada.getId()).as("o id é gerado pelo banco, não pelo chamador").isNotNull();

        Task lida = repositorio.buscarPorId(gravada.getId()).orElseThrow();

        assertThat(lida.getId()).isEqualTo(gravada.getId());
        assertThat(lida.getTitle()).isEqualTo("Escrever a spec");
        assertThat(lida.getDescription()).isEqualTo("Critérios de aceite em Dado/Quando/Então");
        assertThat(lida.getStatus()).isEqualTo(TaskStatus.PENDENTE);
        assertThat(lida.getPriority()).isEqualTo(TaskPriority.MEDIA);
        assertThat(lida.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(lida.getCreatedAt()).isNotNull();
        assertThat(lida.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-009 status e prioridade são gravados como texto")
    void statusEPrioridadeSaoGravadosComoTexto() {
        Task gravada =
                repositorio.gravar(
                        new Task(
                                "Revisar o PR",
                                null,
                                TaskStatus.EM_ANDAMENTO,
                                TaskPriority.ALTA,
                                null));

        Map<String, Object> linha =
                jdbc.queryForMap(
                        "select status, priority from tasks where id = ?", gravada.getId());

        assertThat(linha.get("status"))
                .as("ordinal gravado no lugar do nome corromperia os dados ao reordenar o enum")
                .isEqualTo("EM_ANDAMENTO");
        assertThat(linha.get("priority")).isEqualTo("ALTA");
    }

    @Test
    @DisplayName("@spec:AC-010 as datas de auditoria são preenchidas sozinhas")
    void datasDeAuditoriaSaoPreenchidasSozinhas() {
        Task gravada =
                repositorio.gravar(
                        new Task(
                                "Rodar o verify",
                                null,
                                TaskStatus.PENDENTE,
                                TaskPriority.BAIXA,
                                null));

        Task aposCriacao = repositorio.buscarPorId(gravada.getId()).orElseThrow();
        assertThat(aposCriacao.getCreatedAt()).isNotNull();
        assertThat(aposCriacao.getUpdatedAt()).isNotNull();

        aposCriacao.setStatus(TaskStatus.CONCLUIDA);
        repositorio.gravar(aposCriacao);

        Task aposAlteracao = repositorio.buscarPorId(gravada.getId()).orElseThrow();
        assertThat(aposAlteracao.getUpdatedAt())
                .as("a alteração avança updatedAt")
                .isAfter(aposCriacao.getUpdatedAt());
        assertThat(aposAlteracao.getCreatedAt())
                .as("createdAt é escrito uma vez e nunca mais")
                .isEqualTo(aposCriacao.getCreatedAt());
    }
}
