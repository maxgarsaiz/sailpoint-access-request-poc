package com.sailpoint.accessrequest.domain.exception;

public class AccessRequestNotFoundException extends RuntimeException {
    public AccessRequestNotFoundException(Long id) {
        super("Access request not found with id: " + id);
    }
}
