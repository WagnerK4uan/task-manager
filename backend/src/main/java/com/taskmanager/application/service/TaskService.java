package com.taskmanager.application.service;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.mapper.TaskMapper;
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
        return repositorio
                .buscarPorId(id)
                .map(TaskMapper::paraResposta)
                .orElseThrow(TaskNotFoundException::new);
    }

    public List<TaskResponse> listar(String titulo, TaskStatus status) {
        String trecho = titulo == null || titulo.isBlank() ? null : titulo;
        return repositorio.buscar(trecho, status).stream().map(TaskMapper::paraResposta).toList();
    }
}
