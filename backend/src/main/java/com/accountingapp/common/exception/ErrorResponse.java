package com.accountingapp.common.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String correlationId,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String errorCode, String message, String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, correlationId, List.of());
    }

    public static ErrorResponse withFieldErrors(
            int status, String errorCode, String message, String path, String correlationId,
            Map<String, String> fieldErrors) {
        List<FieldError> errors = fieldErrors.entrySet().stream()
                .map(e -> new FieldError(e.getKey(), e.getValue()))
                .toList();
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, correlationId, errors);
    }
}
