package com.maxgarsaiz.sailpoint.domain.port.out;

import com.maxgarsaiz.sailpoint.domain.exception.EntityAlreadyLockedException;
import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.in.RetryAccessRequestsUseCase.RetryFilters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessRequestRepositoryPort {
    
    AccessRequest save(AccessRequest accessRequest);
    
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
    
    AccessRequest update(AccessRequest accessRequest);
    
    /**
     * Attempts to find an access request and acquire an exclusive lock on it,
     * then transitions it to POOLING status.
     * 
     * This method uses pessimistic locking (FOR UPDATE SKIP LOCKED) to ensure
     * only one job can process the request at a time.
     * 
     * @param id the access request ID
     * @return the locked access request with POOLING status
     * @throws EntityNotFoundException if the access request doesn't exist
     * @throws EntityAlreadyLockedException if the request is already locked by another process
     *         or not in a valid status (PENDING/FAILED) for pooling
     */
    AccessRequest findByIdAndTransitionToPooling(UUID id) throws EntityNotFoundException, EntityAlreadyLockedException;
    
    List<AccessRequest> findByStatus(AccessRequestStatus status);
    
    List<AccessRequest> findByStatusIn(List<AccessRequestStatus> statuses);
    
    List<AccessRequest> findByFilters(RetryFilters filters);
    
    boolean transitionToPendingForRetry(UUID id);
    
    void releaseExpiredPoolingLocks(int timeoutMinutes);
}
