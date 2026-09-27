package com.taskmanager.application.mapper;

import com.taskmanager.application.dto.TaskCreateRequest;
import com.taskmanager.application.dto.TaskResponse;
import com.taskmanager.application.dto.TaskUpdateRequest;
import com.taskmanager.domain.entity.Task;

public final class TaskMapper {

    private TaskMapper() {}

    public static Task paraEntidade(TaskCreateRequest request) {
        return new Task(
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate());
    }

    public static void aplicar(Task task, TaskUpdateRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
    }

    public static TaskResponse paraResposta(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
