package com.taskmanager.application.dto;

import com.taskmanager.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(
        @NotNull(message = "A situação é obrigatória") TaskStatus status) {}
