package com.taskmanager.application.service;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.mapper.TaskMapper;
import com.taskmanager.domain.exception.TaskNotFoundException;
import com.taskmanager.domain.repository.TaskRepository;
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

    /** Traduz o resultado vazio da porta em exceção de domínio — o 404 nasce aqui, não lá. */
    public TaskResponse buscarPorId(Long id) {
        return repositorio
                .buscarPorId(id)
                .map(TaskMapper::paraResposta)
                .orElseThrow(TaskNotFoundException::new);
    }
}
