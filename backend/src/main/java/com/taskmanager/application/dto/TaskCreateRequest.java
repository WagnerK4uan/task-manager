package com.taskmanager.application.dto;

import com.taskmanager.domain.enums.TaskPriority;
import com.taskmanager.domain.enums.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank(message = "O título é obrigatório")
                @Size(max = 120, message = "O título deve ter no máximo 120 caracteres")
                String title,
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
                String description,
        @NotNull(message = "A situação é obrigatória") TaskStatus status,
        @NotNull(message = "A prioridade é obrigatória") TaskPriority priority,
        @FutureOrPresent(message = "O prazo não pode estar no passado") LocalDate dueDate) {}
