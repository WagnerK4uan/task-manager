package com.taskmanager.domain.repository;

import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskStatus;
import java.util.List;
import java.util.Optional;

/**
 * A porta de persistência do domínio: as quatro operações que os casos de uso do CRUD precisam, e
 * nenhuma a mais. Quem a implementa vive em {@code infrastructure} — o domínio não conhece o
 * Spring Data.
 */
public interface TaskRepository {

    /** Insere quando não há id e atualiza quando há; devolve a tarefa com id e datas preenchidos. */
    Task gravar(Task task);

    Optional<Task> buscarPorId(Long id);

    /**
     * Busca por trecho do título (sem distinção de maiúsculas) e status exato, em {@code createdAt}
     * decrescente. Filtro nulo não restringe.
     */
    List<Task> buscar(String titulo, TaskStatus status);

    /** Remove a linha; verificar se ela existia é papel da camada de aplicação. */
    void excluirPorId(Long id);
}
