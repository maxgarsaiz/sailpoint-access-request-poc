package com.maxgarsaiz.sailpoint.domain.exception;

/**
 * Exception thrown when a request cannot be processed due to business rules violation.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class UnprocessableException extends RuntimeException {
    
    public UnprocessableException(String message) {
        super(message);
    }
    
    public UnprocessableException(String message, Throwable cause) {
        super(message, cause);
    }
}
