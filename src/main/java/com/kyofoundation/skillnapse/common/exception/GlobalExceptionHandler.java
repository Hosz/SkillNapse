package com.kyofoundation.skillnapse.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        log.warn("Requisição inválida (400): {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição ilegível ou malformado (400): {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "O corpo da requisição é inválido ou está malformado."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso não encontrado (404): {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        log.warn("Conflito de dados (409): {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        log.warn("Acesso proibido (403): {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Acesso não autorizado (401): {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Erro de validação de argumentos (400): {}", details);
        ErrorResponse error = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation Error", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AiQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleAiQuotaExceeded(AiQuotaExceededException ex) {
        log.warn("Cota de requisições da IA atingida: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "COTA_IA_ATINGIDA",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String msg = extrairMensagemProfunda(ex);

        if (isQuotaExhaustedError(msg)) {
            log.warn("Cota de requisições ou taxa limite da IA atingida (429): {}", msg);
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "COTA_IA_ATINGIDA",
                    "A cota diária de requisições de Inteligência Artificial foi atingida no provedor (429). A renovação ocorre automaticamente à meia-noite (UTC)."
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        log.error("Erro interno inesperado no servidor (500): ", ex);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocorreu um erro interno no servidor. Por favor, tente novamente mais tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String extrairMensagemProfunda(Throwable ex) {
        if (ex == null) return "";
        StringBuilder sb = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append(" ");
            }
            current = current.getCause();
        }
        return sb.toString().trim();
    }

    private boolean isQuotaExhaustedError(String msg) {
        if (msg == null || msg.isBlank()) return false;
        String m = msg.toLowerCase();
        return m.contains("429")
                || m.contains("quota")
                || m.contains("rate-limit")
                || m.contains("rate limit")
                || m.contains("resource_exhausted")
                || m.contains("exhausted")
                || m.contains("too many requests")
                || m.contains("exceeded your current quota");
    }
}
