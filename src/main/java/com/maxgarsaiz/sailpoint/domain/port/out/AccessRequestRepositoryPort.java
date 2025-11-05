package com.maxgarsaiz.sailpoint.domain.port.out;

import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.in.RetryAccessRequestsUseCase.RetryFilters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessRequestRepositoryPort {
    
    AccessRequest save(AccessRequest accessRequest);

    AccessRequest update(AccessRequest accessRequest);
    
    /**
     * Finds an access request by ID.
     * Returns Optional.empty() if not found (use this when you want to handle the absence).
     * 
     * @param id the access request ID
     * @return Optional containing the access request, or empty if not found
     */
    Optional<AccessRequest> findById(UUID id);
    
    /**
     * Finds an access request by ID and throws an exception if not found.
     * Use this method when the entity MUST exist (most common case).
     * 
     * @param id the access request ID
     * @return the access request
     * @throws AccessRequestNotFoundException if not found
     */
    default AccessRequest findByIdOrThrow(UUID id) throws EntityNotFoundException {
        return findById(id)
            .orElseThrow(() -> new EntityNotFoundException("AccessRequest", id));
    }
    
    List<AccessRequest> findAll();
    
    List<AccessRequest> findByStatus(AccessRequestStatus status);
    
    List<AccessRequest> findByStatusIn(List<AccessRequestStatus> statuses);
    
    List<AccessRequest> findByFilters(RetryFilters filters);
    
    boolean transitionToPendingForRetry(UUID id);
    
    /**
     * Finds an access request by its attempt ID.
     * Used by JobServerFilter to check if job should continue after completion.
     * 
     * @param attemptId the attempt ID
     * @return Optional containing the access request, or empty if not found
     */
    Optional<AccessRequest> findByAttemptId(UUID attemptId);
}

