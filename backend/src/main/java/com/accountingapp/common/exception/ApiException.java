package com.accountingapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for all exceptions that should be translated into a specific HTTP
 * response by {@link GlobalExceptionHandler}, rather than surfacing as a 500.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
