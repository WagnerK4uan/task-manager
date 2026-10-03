package com.taskmanager.infrastructure.persistence;

import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.repository.TaskRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Único ponto onde a porta do domínio e o Spring Data se tocam. */
@Component
public class TaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository jpaRepository;

    public TaskRepositoryAdapter(TaskJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Task gravar(Task task) {
        return jpaRepository.save(task);
    }

    @Override
    public Optional<Task> buscarPorId(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Task> buscar(String titulo, TaskStatus status) {
        return jpaRepository.buscar(titulo, status);
    }

    @Override
    public void excluirPorId(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long contarExistentes(Collection<Long> ids) {
        return jpaRepository.countByIdIn(ids);
    }

    @Override
    public void excluirPorIds(Collection<Long> ids) {
        jpaRepository.deleteAllByIdInBatch(ids);
    }
}
