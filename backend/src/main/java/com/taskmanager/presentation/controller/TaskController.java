package com.taskmanager.presentation.controller;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.dto.TaskStatusUpdateRequest;
import com.taskmanager.application.dto.TaskUpdateRequest;
import com.taskmanager.application.service.TaskService;
import com.taskmanager.domain.enums.TaskStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> criar(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse criada = service.criar(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + criada.id())).body(criada);
    }

    @GetMapping
    public List<TaskResponse> listar(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) TaskStatus status) {
        return service.listar(title, status);
    }

    @GetMapping("/{id}")
    public TaskResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public TaskResponse substituir(
            @PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return service.substituir(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse alterarStatus(
            @PathVariable Long id, @Valid @RequestBody TaskStatusUpdateRequest request) {
        return service.alterarStatus(id, request);
    }
}
