package com.maxgarsaiz.sailpoint.domain.port.out;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;

public interface IdentityProviderPort {
    
    /**
     * Creates an access request attempt in Sailpoint.
     * Uses accessRequest.getLastAttempt() to get the attempt to create.
     * Updates the attempt with sailpointAccessRequestId.
     * 
     * @param accessRequest the access request with lastAttempt set
     * @return the accessRequest with lastAttempt updated with sailpointAccessRequestId
     * @throws IdentityClientException if communication fails
     */
    AccessRequest createAccessRequest(AccessRequest accessRequest) 
            throws IdentityClientException;
    
    /**
     * Gets the current status of an access request attempt from Sailpoint.
     * 
     * @param attempt the attempt to check (must have sailpointAccessRequestId)
     * @return status response with the current status
     * @throws IdentityClientException if communication fails
     */
    SailpointStatusResponse getRequestStatus(AccessRequestAttempt attempt) throws IdentityClientException;
    
    /**
     * Response from Sailpoint with status information.
     */
    record SailpointStatusResponse(String status, String message) {}
    
    class IdentityClientException extends RuntimeException {
        public IdentityClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
