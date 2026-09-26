package com.taskmanager.presentation.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fields) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse de(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse deValidacao(String path, List<FieldError> fields) {
        return new ErrorResponse(
                Instant.now(), 400, "VALIDATION_ERROR", "Validation failed", path, fields);
    }
}
