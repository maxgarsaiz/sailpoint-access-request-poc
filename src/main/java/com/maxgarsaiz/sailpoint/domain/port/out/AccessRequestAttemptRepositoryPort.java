package com.maxgarsaiz.sailpoint.domain.port.out;

import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.model.AttemptStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessRequestAttemptRepositoryPort {
    
    AccessRequestAttempt save(AccessRequestAttempt attempt);
    
    AccessRequestAttempt update(AccessRequestAttempt attempt);
    
    Optional<AccessRequestAttempt> findById(UUID id);
    
    AccessRequestAttempt findByIdOrThrow(UUID id) throws EntityNotFoundException;
    
    List<AccessRequestAttempt> findByAccessRequestId(UUID accessRequestId);
    
    Optional<AccessRequestAttempt> findLastByAccessRequestId(UUID accessRequestId);
    
    List<AccessRequestAttempt> findByStatus(AttemptStatus status);
    
    void deleteById(UUID id);
}
