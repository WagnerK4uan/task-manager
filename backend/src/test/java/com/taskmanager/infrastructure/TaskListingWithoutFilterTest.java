package com.taskmanager.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmanager.PostgresIntegrationTest;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskPriority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class TaskListingWithoutFilterTest extends PostgresIntegrationTest {

    @Autowired private TaskRepository repositorio;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparTabela() {
        jdbc.update("delete from tasks");
    }

    @Test
    @DisplayName("@spec:AC-047 a listagem sem filtro de título responde na primeira execução da consulta")
    void listagemSemFiltroDeTituloRespondeNaPrimeiraExecucao() {
        gravar("Escrever a spec da correção", TaskStatus.PENDENTE);
        gravar("Revisar o proxy do nginx", TaskStatus.CONCLUIDA);

        assertThat(repositorio.buscar(null, null))
                .as("sem filtro nenhum, da mais recente para a mais antiga")
                .extracting(Task::getTitle)
                .containsExactly("Revisar o proxy do nginx", "Escrever a spec da correção");

        assertThat(repositorio.buscar(null, TaskStatus.PENDENTE))
                .as("só por situação, ainda sem título")
                .extracting(Task::getTitle)
                .containsExactly("Escrever a spec da correção");
    }

    private void gravar(String titulo, TaskStatus status) {
        repositorio.gravar(new Task(titulo, null, status, TaskPriority.MEDIA, null));
    }
}
