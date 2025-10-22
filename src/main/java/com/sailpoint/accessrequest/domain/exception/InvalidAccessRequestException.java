package com.sailpoint.accessrequest.domain.exception;

public class InvalidAccessRequestException extends RuntimeException {
    public InvalidAccessRequestException(String message) {
        super(message);
    }
}
