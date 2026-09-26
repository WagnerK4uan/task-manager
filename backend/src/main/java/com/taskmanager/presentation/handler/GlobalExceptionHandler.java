package com.taskmanager.presentation.handler;

import com.taskmanager.domain.exception.TaskNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> tarefaNaoEncontrada(
            TaskNotFoundException excecao, HttpServletRequest requisicao) {
        return resposta(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacaoRecusada(
            MethodArgumentNotValidException excecao, HttpServletRequest requisicao) {
        List<ErrorResponse.FieldError> campos =
                excecao.getFieldErrors().stream()
                        .map(
                                erro ->
                                        new ErrorResponse.FieldError(
                                                erro.getField(), erro.getDefaultMessage()))
                        .toList();

        return ResponseEntity.badRequest()
                .body(ErrorResponse.deValidacao(requisicao.getRequestURI(), campos));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> requisicaoMalformada(HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request", requisicao);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rotaNaoEncontrada(HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", requisicao);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNaoSuportado(
            HttpRequestMethodNotSupportedException excecao, HttpServletRequest requisicao) {
        Set<HttpMethod> suportados =
                Objects.requireNonNullElse(excecao.getSupportedHttpMethods(), Set.of());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(suportados.toArray(HttpMethod[]::new))
                .body(
                        ErrorResponse.de(
                                HttpStatus.METHOD_NOT_ALLOWED.value(),
                                "METHOD_NOT_ALLOWED",
                                "Method not allowed",
                                requisicao.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> erroInterno(HttpServletRequest requisicao) {
        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal error", requisicao);
    }

    private static ResponseEntity<ErrorResponse> resposta(
            HttpStatus status, String erro, String mensagem, HttpServletRequest requisicao) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.de(status.value(), erro, mensagem, requisicao.getRequestURI()));
    }
}
