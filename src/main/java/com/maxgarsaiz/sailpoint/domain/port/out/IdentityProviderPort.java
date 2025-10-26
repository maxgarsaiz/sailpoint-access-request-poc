package com.maxgarsaiz.sailpoint.domain.port.out;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;

import java.util.Optional;

public interface IdentityProviderPort {
    
    /**
     * Creates an access request in Sailpoint and returns the updated domain model.
     * Handles all Feign exceptions internally.
     * 
     * @param accessRequest the access request to create
     * @return the access request with Sailpoint request ID populated
     * @throws IdentityClientException if the request fails after retries
     */
    AccessRequest createAccessRequest(AccessRequest accessRequest);
    
    /**
     * Checks the status of an access request in Sailpoint.
     * 
     * @param sailpointRequestId the Sailpoint request ID
     * @return Optional containing the status if found, empty if not found or error
     */
    Optional<RequestStatus> checkRequestStatus(String sailpointRequestId);
    
    /**
     * Represents the status of a request in Sailpoint
     */
    record RequestStatus(
        String requestId,
        Status status,
        String message
    ) {}
    
    enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    
    /**
     * Exception thrown when Identity client operations fail
     */
    class IdentityClientException extends RuntimeException {
        public IdentityClientException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public IdentityClientException(String message) {
            super(message);
        }
    }
}