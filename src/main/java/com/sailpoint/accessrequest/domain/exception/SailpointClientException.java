package com.sailpoint.accessrequest.domain.exception;

public class SailpointClientException extends RuntimeException {
    public SailpointClientException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SailpointClientException(String message) {
        super(message);
    }
}
