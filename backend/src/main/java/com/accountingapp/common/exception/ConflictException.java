package com.accountingapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised for state conflicts: duplicate records, or an action attempted
 * against a resource whose current status doesn't allow it.
 */
public class ConflictException extends ApiException {

    public ConflictException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
