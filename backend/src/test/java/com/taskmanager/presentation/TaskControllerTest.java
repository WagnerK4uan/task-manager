package com.taskmanager.presentation;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.dto.TaskStatusUpdateRequest;
import com.taskmanager.application.dto.TaskUpdateRequest;
import com.taskmanager.application.service.TaskService;
import com.taskmanager.domain.enums.TaskPriority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.exception.TaskNotFoundException;
import com.taskmanager.presentation.controller.TaskController;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final TaskResponse TAREFA =
            new TaskResponse(
                    7L,
                    "Escrever a spec",
                    "Critérios em Dado/Quando/Então",
                    TaskStatus.PENDENTE,
                    TaskPriority.ALTA,
                    LocalDate.of(2026, 10, 15),
                    Instant.parse("2026-09-26T12:00:00Z"),
                    Instant.parse("2026-09-26T12:00:00Z"));

    private static final String CORPO_VALIDO =
            """
            {
              "title": "Escrever a spec",
              "description": "Critérios em Dado/Quando/Então",
              "status": "PENDENTE",
              "priority": "ALTA",
              "dueDate": "2026-10-15"
            }
            """;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TaskService service;

    @Test
    @DisplayName("@spec:AC-014 a criação responde 201 com Location e o recurso criado")
    void criacaoRespondeCriadoComLocationERecurso() throws Exception {
        given(service.criar(any(TaskCreateRequest.class))).willReturn(TAREFA);

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Escrever a spec"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.priority").value("ALTA"))
                .andExpect(jsonPath("$.dueDate").value("2026-10-15"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("@spec:AC-015 cada campo recusado aparece com o motivo e nada é gravado")
    void cadaCampoRecusadoApareceComOMotivo() throws Exception {
        String corpoInvalido =
                """
                {
                  "title": "   ",
                  "status": null,
                  "priority": null,
                  "dueDate": "2020-01-01"
                }
                """;

        mockMvc.perform(
                        post("/api/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/tasks"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(
                        jsonPath("$.fields[*].field")
                                .value(containsInAnyOrder("title", "status", "priority", "dueDate")))
                .andExpect(jsonPath("$.fields[?(@.field == 'title')].message")
                        .value("O título é obrigatório"));

        verify(service, never()).criar(any(TaskCreateRequest.class));
    }

    @Test
    @DisplayName("@spec:AC-016 corpo ilegível responde 400 sem vazar detalhe interno")
    void corpoIlegivelResponde400SemVazarDetalhe() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fields").doesNotExist())
                .andExpect(content().string(not(containsString("com.taskmanager"))));

        String situacaoInexistente =
                """
                {
                  "title": "Escrever a spec",
                  "status": "URGENTE",
                  "priority": "ALTA"
                }
                """;

        mockMvc.perform(
                        post("/api/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(situacaoInexistente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        verify(service, never()).criar(any(TaskCreateRequest.class));
    }

    @Test
    @DisplayName("@spec:AC-017 a consulta responde 200 com a tarefa")
    void consultaResponde200ComATarefa() throws Exception {
        given(service.buscarPorId(7L)).willReturn(TAREFA);

        mockMvc.perform(get("/api/tasks/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Escrever a spec"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("@spec:AC-018 tarefa inexistente responde 404 no contrato único de erro")
    void tarefaInexistenteResponde404NoContratoDeErro() throws Exception {
        willThrow(new TaskNotFoundException()).given(service).buscarPorId(999L);

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/999"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    @DisplayName("@spec:AC-020 identificador não numérico responde 400, não 500")
    void identificadorNaoNumericoResponde400() throws Exception {
        mockMvc.perform(get("/api/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/tasks/abc"));
    }

    @Test
    @DisplayName("@spec:AC-021 a listagem responde 200 com o array na ordem que o service devolveu")
    void listagemResponde200ComOArrayNaOrdemDoService() throws Exception {
        TaskResponse intermediaria = comIdEData(8L, "Revisar o PR", "2026-09-26T11:00:00Z");
        TaskResponse antiga = comIdEData(9L, "Subir o ambiente", "2026-09-26T10:00:00Z");

        given(service.listar(null, null)).willReturn(List.of(TAREFA, intermediaria, antiga));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[1].id").value(8))
                .andExpect(jsonPath("$[2].id").value(9))
                .andExpect(jsonPath("$[0].title").value("Escrever a spec"))
                .andExpect(jsonPath("$[0].description").value("Critérios em Dado/Quando/Então"))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[0].priority").value("ALTA"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-10-15"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    @Test
    @DisplayName("@spec:AC-022 a query string chega ao service como trecho e situação")
    void queryStringChegaAoServiceComoTrechoESituacao() throws Exception {
        given(service.listar("escrever", TaskStatus.PENDENTE)).willReturn(List.of(TAREFA));

        mockMvc.perform(get("/api/tasks").param("title", "escrever").param("status", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7));

        verify(service).listar("escrever", TaskStatus.PENDENTE);
    }

    @Test
    @DisplayName("@spec:AC-024 situação fora do enum na consulta responde 400, não 500")
    void situacaoForaDoEnumNaConsultaResponde400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "URGENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/tasks"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("com.taskmanager"))));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("@spec:AC-028 a substituição recusa cada campo obrigatório inválido e não chama a aplicação")
    void substituicaoRecusaCadaCampoObrigatorioInvalido() throws Exception {
        String corpoInvalido =
                """
                {
                  "title": "   ",
                  "status": null,
                  "priority": null
                }
                """;

        mockMvc.perform(
                        put("/api/tasks/7")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/tasks/7"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(
                        jsonPath("$.fields[*].field")
                                .value(containsInAnyOrder("title", "status", "priority")))
                .andExpect(
                        jsonPath("$.fields[?(@.field == 'title')].message")
                                .value("O título é obrigatório"));

        verify(service, never()).substituir(any(), any(TaskUpdateRequest.class));
    }

    @Test
    @DisplayName("@spec:AC-029 a substituição aceita prazo no passado e responde 200")
    void substituicaoAceitaPrazoNoPassado() throws Exception {
        TaskResponse atrasada =
                new TaskResponse(
                        7L,
                        "Título corrigido",
                        null,
                        TaskStatus.PENDENTE,
                        TaskPriority.ALTA,
                        LocalDate.of(2020, 1, 1),
                        Instant.parse("2026-09-26T12:00:00Z"),
                        Instant.parse("2026-09-27T12:00:00Z"));
        given(service.substituir(eq(7L), any(TaskUpdateRequest.class))).willReturn(atrasada);

        String corpoComPrazoVencido =
                """
                {
                  "title": "Título corrigido",
                  "status": "PENDENTE",
                  "priority": "ALTA",
                  "dueDate": "2020-01-01"
                }
                """;

        mockMvc.perform(
                        put("/api/tasks/7")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoComPrazoVencido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Título corrigido"))
                .andExpect(jsonPath("$.dueDate").value("2020-01-01"));

        verify(service).substituir(eq(7L), any(TaskUpdateRequest.class));
    }

    @Test
    @DisplayName("@spec:AC-032 situação ausente é VALIDATION_ERROR e fora do enum é MALFORMED_REQUEST")
    void situacaoAusenteOuForaDoEnumNaTrocaERecusada() throws Exception {
        mockMvc.perform(
                        patch("/api/tasks/7/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/tasks/7/status"))
                .andExpect(jsonPath("$.fields[*].field").value(containsInAnyOrder("status")))
                .andExpect(
                        jsonPath("$.fields[?(@.field == 'status')].message")
                                .value("A situação é obrigatória"));

        mockMvc.perform(
                        patch("/api/tasks/7/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"URGENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fields").doesNotExist())
                .andExpect(content().string(not(containsString("com.taskmanager"))));

        verify(service, never()).alterarStatus(any(), any(TaskStatusUpdateRequest.class));
    }

    @Test
    @DisplayName("@spec:AC-033 trocar a situação de tarefa inexistente responde 404 no contrato único")
    void trocarSituacaoDeTarefaInexistenteResponde404() throws Exception {
        willThrow(new TaskNotFoundException())
                .given(service)
                .alterarStatus(eq(999L), any(TaskStatusUpdateRequest.class));

        mockMvc.perform(
                        patch("/api/tasks/999/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"CONCLUIDA\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/999/status"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    @DisplayName("@spec:AC-034 a exclusão responde 204 sem corpo e delega ao service")
    void exclusaoResponde204SemCorpo() throws Exception {
        mockMvc.perform(delete("/api/tasks/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).excluir(7L);
    }

    @Test
    @DisplayName("@spec:AC-035 excluir tarefa inexistente responde 404 no contrato único")
    void excluirTarefaInexistenteResponde404() throws Exception {
        willThrow(new TaskNotFoundException()).given(service).excluir(999L);

        mockMvc.perform(delete("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/999"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    private static TaskResponse comIdEData(Long id, String titulo, String criadaEm) {
        return new TaskResponse(
                id,
                titulo,
                null,
                TaskStatus.PENDENTE,
                TaskPriority.MEDIA,
                null,
                Instant.parse(criadaEm),
                Instant.parse(criadaEm));
    }
}
