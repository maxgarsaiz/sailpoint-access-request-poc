package com.maxgarsaiz.sailpoint.domain.port.out;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;

import java.time.LocalDateTime;

public interface IdentityProviderPort {
    
    AccessRequest createAccessRequest(AccessRequest accessRequest) throws IdentityClientException;
    
    /**
     * Gets the full access request status from Sailpoint.
     * 
     * @param sailpointRequestId the Sailpoint request ID
     * @return the access request response with current status
     */
    AccessRequestResponse getAccessRequest(String sailpointRequestId);
    
    record AccessRequestResponse(
        String requestId,
        RequestStatus status,
        String message,
        LocalDateTime lastUpdated
    ) {}
    
    enum RequestStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    
    class IdentityClientException extends RuntimeException {
        public IdentityClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
