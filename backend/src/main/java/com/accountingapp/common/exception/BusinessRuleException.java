package com.accountingapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a request is well-formed but violates an accounting business
 * rule (e.g. an unbalanced journal entry, posting to a locked period).
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, message);
    }
}
