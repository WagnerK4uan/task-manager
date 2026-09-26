package com.taskmanager.presentation.controller;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/{id}")
    public TaskResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
