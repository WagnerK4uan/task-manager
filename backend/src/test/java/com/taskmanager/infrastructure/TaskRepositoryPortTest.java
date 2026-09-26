package com.taskmanager.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmanager.PostgresIntegrationTest;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskPriority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prova as operações pela porta do domínio. A dependência é declarada pelo tipo da interface, nunca
 * pelo adaptador: se alguém inverter a direção da dependência, esta classe deixa de compilar.
 */
class TaskRepositoryPortTest extends PostgresIntegrationTest {

    @Autowired private TaskRepository repositorio;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparTabela() {
        jdbc.update("delete from tasks");
    }

    @Test
    @DisplayName("@spec:AC-011 as quatro operações funcionam através da porta")
    void quatroOperacoesFuncionamAtravesDaPorta() {
        Task inserida =
                repositorio.gravar(
                        new Task(
                                "Publicar a release",
                                "Tag e changelog",
                                TaskStatus.PENDENTE,
                                TaskPriority.ALTA,
                                LocalDate.of(2026, 11, 1)));

        assertThat(inserida.getId()).as("gravar sem id insere").isNotNull();
        assertThat(repositorio.buscarPorId(inserida.getId())).isPresent();

        inserida.setTitle("Publicar a release 1.0");
        Task atualizada = repositorio.gravar(inserida);

        assertThat(atualizada.getId()).as("gravar com id atualiza, não duplica").isEqualTo(inserida.getId());
        assertThat(contarLinhas()).isEqualTo(1);
        assertThat(repositorio.buscarPorId(inserida.getId()).orElseThrow().getTitle())
                .isEqualTo("Publicar a release 1.0");

        assertThat(repositorio.buscar(null, null)).hasSize(1);

        repositorio.excluirPorId(inserida.getId());

        assertThat(repositorio.buscarPorId(inserida.getId())).isEmpty();
        assertThat(contarLinhas()).isZero();
    }

    @Test
    @DisplayName("@spec:AC-012 a busca por título é parcial e indiferente a maiúsculas")
    void buscaPorTituloEParcialEIndiferenteAMaiusculas() {
        gravar("Escrever a spec", TaskStatus.CONCLUIDA);
        gravar("escrever o teste", TaskStatus.PENDENTE);
        gravar("Revisar o PR", TaskStatus.PENDENTE);

        assertThat(repositorio.buscar("escrever", null))
                .as("trecho em minúsculas encontra os dois títulos, do mais recente ao mais antigo")
                .extracting(Task::getTitle)
                .containsExactly("escrever o teste", "Escrever a spec");

        assertThat(repositorio.buscar("escrever", TaskStatus.PENDENTE))
                .extracting(Task::getTitle)
                .containsExactly("escrever o teste");

        assertThat(repositorio.buscar(null, null))
                .as("sem filtro nenhum, devolve todas")
                .hasSize(3);
    }

    @Test
    @DisplayName("@spec:AC-013 buscar um id inexistente é resultado vazio, não erro")
    void buscarIdInexistenteEResultadoVazio() {
        assertThat(repositorio.buscarPorId(999L)).isEmpty();
    }

    private void gravar(String titulo, TaskStatus status) {
        repositorio.gravar(new Task(titulo, null, status, TaskPriority.MEDIA, null));
    }

    private int contarLinhas() {
        return jdbc.queryForObject("select count(*) from tasks", Integer.class);
    }
}
