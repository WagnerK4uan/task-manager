package com.taskmanager.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TaskBatchDeleteRequest(
        @NotEmpty(message = "Informe ao menos uma tarefa")
                List<@NotNull(message = "O identificador é obrigatório") Long> ids) {}
