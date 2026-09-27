package com.taskmanager.application.service;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.dto.TaskStatusUpdateRequest;
import com.taskmanager.application.dto.TaskUpdateRequest;
import com.taskmanager.application.mapper.TaskMapper;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.exception.TaskNotFoundException;
import com.taskmanager.domain.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository repositorio;

    public TaskService(TaskRepository repositorio) {
        this.repositorio = repositorio;
    }

    public TaskResponse criar(TaskCreateRequest request) {
        return TaskMapper.paraResposta(repositorio.gravar(TaskMapper.paraEntidade(request)));
    }

    public TaskResponse buscarPorId(Long id) {
        return TaskMapper.paraResposta(carregar(id));
    }

    public List<TaskResponse> listar(String titulo, TaskStatus status) {
        String trecho = titulo == null || titulo.isBlank() ? null : titulo;
        return repositorio.buscar(trecho, status).stream().map(TaskMapper::paraResposta).toList();
    }

    public TaskResponse substituir(Long id, TaskUpdateRequest request) {
        Task tarefa = carregar(id);
        TaskMapper.aplicar(tarefa, request);
        return TaskMapper.paraResposta(repositorio.gravar(tarefa));
    }

    public TaskResponse alterarStatus(Long id, TaskStatusUpdateRequest request) {
        Task tarefa = carregar(id);
        tarefa.setStatus(request.status());
        return TaskMapper.paraResposta(repositorio.gravar(tarefa));
    }

    public void excluir(Long id) {
        carregar(id);
        repositorio.excluirPorId(id);
    }

    private Task carregar(Long id) {
        return repositorio.buscarPorId(id).orElseThrow(TaskNotFoundException::new);
    }
}
