package com.taskmanager.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.dto.TaskStatusUpdateRequest;
import com.taskmanager.application.dto.TaskUpdateRequest;
import com.taskmanager.application.service.TaskService;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskPriority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.exception.TaskNotFoundException;
import com.taskmanager.domain.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskServiceTest {

    private final RepositorioEmMemoria repositorio = new RepositorioEmMemoria();
    private final TaskService service = new TaskService(repositorio);

    @Test
    @DisplayName("@spec:AC-014 a tarefa criada volta com identificador e datas do servidor")
    void tarefaCriadaVoltaComIdentificadorEDatas() {
        TaskResponse criada =
                service.criar(
                        new TaskCreateRequest(
                                "Escrever a spec",
                                "Critérios em Dado/Quando/Então",
                                TaskStatus.PENDENTE,
                                TaskPriority.ALTA,
                                LocalDate.of(2026, 10, 15)));

        assertThat(criada.id()).isNotNull();
        assertThat(criada.title()).isEqualTo("Escrever a spec");
        assertThat(criada.description()).isEqualTo("Critérios em Dado/Quando/Então");
        assertThat(criada.status()).isEqualTo(TaskStatus.PENDENTE);
        assertThat(criada.priority()).isEqualTo(TaskPriority.ALTA);
        assertThat(criada.dueDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(criada.createdAt()).isNotNull();
        assertThat(criada.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-017 a consulta devolve a tarefa que a porta guardou")
    void consultaDevolveATarefaQueAPortaGuardou() {
        Long id =
                service.criar(
                                new TaskCreateRequest(
                                        "Revisar o PR", null, TaskStatus.EM_ANDAMENTO,
                                        TaskPriority.MEDIA, null))
                        .id();

        TaskResponse encontrada = service.buscarPorId(id);

        assertThat(encontrada.id()).isEqualTo(id);
        assertThat(encontrada.title()).isEqualTo("Revisar o PR");
        assertThat(encontrada.status()).isEqualTo(TaskStatus.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("@spec:AC-018 identificador inexistente vira exceção de domínio, não vazio")
    void identificadorInexistenteViraExcecaoDeDominio() {
        assertThatThrownBy(() -> service.buscarPorId(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task not found");
    }

    @Test
    @DisplayName("@spec:AC-022 os filtros de título e de situação valem juntos, separados e em branco")
    void filtrosDeTituloESituacaoValemJuntosESeparados() {
        criar("Escrever a spec", TaskStatus.CONCLUIDA);
        criar("escrever o teste", TaskStatus.PENDENTE);
        criar("Revisar o PR", TaskStatus.PENDENTE);

        assertThat(service.listar("escrever", null))
                .extracting(TaskResponse::title)
                .containsExactlyInAnyOrder("Escrever a spec", "escrever o teste");

        assertThat(service.listar(null, TaskStatus.PENDENTE))
                .extracting(TaskResponse::title)
                .containsExactlyInAnyOrder("escrever o teste", "Revisar o PR");

        assertThat(service.listar("escrever", TaskStatus.PENDENTE))
                .extracting(TaskResponse::title)
                .containsExactly("escrever o teste");

        // Espaços só: sem a normalização do service (ASM-017) este filtro não casaria nada.
        assertThat(service.listar("   ", null))
                .extracting(TaskResponse::title)
                .containsExactlyInAnyOrder("Escrever a spec", "escrever o teste", "Revisar o PR");
    }

    @Test
    @DisplayName("@spec:AC-023 filtro que não casa nada devolve lista vazia, nunca exceção")
    void filtroQueNaoCasaNadaDevolveListaVazia() {
        criar("Revisar o PR", TaskStatus.PENDENTE);

        assertThat(service.listar("inexistente", null)).isEmpty();
        assertThat(service.listar(null, TaskStatus.EM_ANDAMENTO)).isEmpty();
    }

    @Test
    @DisplayName("@spec:AC-030 substituir tarefa inexistente vira exceção e não grava nada")
    void substituirTarefaInexistenteViraExcecao() {
        assertThatThrownBy(
                        () ->
                                service.substituir(
                                        999L,
                                        new TaskUpdateRequest(
                                                "Tarefa que não existe",
                                                null,
                                                TaskStatus.PENDENTE,
                                                TaskPriority.MEDIA,
                                                null)))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task not found");

        assertThat(service.listar(null, null)).isEmpty();
    }

    @Test
    @DisplayName("@spec:AC-031 a troca de situação muda a situação e preserva os outros campos")
    void trocaDeSituacaoMudaSomenteASituacao() {
        TaskResponse antes =
                service.criar(
                        new TaskCreateRequest(
                                "Revisar o PR",
                                "Conferir os testes da edição",
                                TaskStatus.PENDENTE,
                                TaskPriority.ALTA,
                                LocalDate.of(2026, 11, 20)));

        TaskResponse depois =
                service.alterarStatus(
                        antes.id(), new TaskStatusUpdateRequest(TaskStatus.CONCLUIDA));

        assertThat(depois.status()).isEqualTo(TaskStatus.CONCLUIDA);
        assertThat(depois.id()).isEqualTo(antes.id());
        assertThat(depois.title()).isEqualTo(antes.title());
        assertThat(depois.description()).isEqualTo(antes.description());
        assertThat(depois.priority()).isEqualTo(antes.priority());
        assertThat(depois.dueDate()).isEqualTo(antes.dueDate());
        assertThat(depois.createdAt()).isEqualTo(antes.createdAt());
    }

    private TaskResponse criar(String titulo, TaskStatus status) {
        return service.criar(
                new TaskCreateRequest(titulo, null, status, TaskPriority.MEDIA, null));
    }

    private static final class RepositorioEmMemoria implements TaskRepository {

        private final Map<Long, Task> tarefas = new HashMap<>();
        private long proximoId = 1;

        @Override
        public Task gravar(Task task) {
            Long id = task.getId() == null ? proximoId++ : task.getId();
            Task gravada = new TarefaGravada(task, id, Instant.now());
            tarefas.put(id, gravada);
            return gravada;
        }

        @Override
        public Optional<Task> buscarPorId(Long id) {
            return Optional.ofNullable(tarefas.get(id));
        }

        @Override
        public List<Task> buscar(String titulo, TaskStatus status) {
            return tarefas.values().stream()
                    .filter(tarefa -> titulo == null || contemTrecho(tarefa.getTitle(), titulo))
                    .filter(tarefa -> status == null || tarefa.getStatus() == status)
                    .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                    .toList();
        }

        private static boolean contemTrecho(String titulo, String trecho) {
            return titulo.toLowerCase().contains(trecho.toLowerCase());
        }

        @Override
        public void excluirPorId(Long id) {
            tarefas.remove(id);
        }
    }

    private static final class TarefaGravada extends Task {

        private final Long id;
        private final Instant criadaEm;
        private final Instant alteradaEm;

        private TarefaGravada(Task original, Long id, Instant agora) {
            super(
                    original.getTitle(),
                    original.getDescription(),
                    original.getStatus(),
                    original.getPriority(),
                    original.getDueDate());
            this.id = id;
            this.criadaEm = original.getCreatedAt() == null ? agora : original.getCreatedAt();
            this.alteradaEm = agora;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public Instant getCreatedAt() {
            return criadaEm;
        }

        @Override
        public Instant getUpdatedAt() {
            return alteradaEm;
        }
    }
}
