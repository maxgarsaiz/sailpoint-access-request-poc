package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.port.in.CreateAccessRequestUseCase;
import com.maxgarsaiz.sailpoint.domain.port.in.GetAccessRequestUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestAttemptRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestService implements CreateAccessRequestUseCase, GetAccessRequestUseCase {
    
    private final AccessRequestRepositoryPort repositoryPort;
    private final AccessRequestAttemptRepositoryPort attemptRepositoryPort;
    private final JobSchedulerPort jobSchedulerPort;
    private final IdentityProviderPort identityProviderPort;
    
    @Override
    @Transactional
    public AccessRequest createAccessRequest(CreateAccessRequestCommand command) {
        log.info("Creating access request for user: {}", command.userId());
        
        // 1. Create and save AccessRequest entity
        AccessRequest accessRequest = command.toDomain();
        AccessRequest savedAccessRequest = repositoryPort.save(accessRequest);
        log.info("Access request saved to database with id: {}", savedAccessRequest.getId());
        
        // 2. Create and save first AccessRequestAttempt
        AccessRequestAttempt attempt = command.toFirstAttempt(savedAccessRequest.getId());
        AccessRequestAttempt savedAttempt = attemptRepositoryPort.save(attempt);
        log.info("Access request attempt created with id: {}", savedAttempt.getId());
        
        // 3. Set lastAttempt in AccessRequest (transient field)
        savedAccessRequest.setLastAttempt(savedAttempt);
        
        try {
            // 4. Create in Sailpoint (uses lastAttempt from accessRequest)
            // Returns AccessRequest with lastAttempt updated with sailpointAccessRequestId
            AccessRequest accessRequestWithAttempt = 
                    identityProviderPort.createAccessRequest(savedAccessRequest);
            
            // 5. Update attempt with Sailpoint access request ID in database
            AccessRequestAttempt updatedAttempt = accessRequestWithAttempt.getLastAttempt();
            attemptRepositoryPort.update(updatedAttempt);
            log.info("Attempt updated with Sailpoint access request ID: {} (attemptId: {})", 
                    updatedAttempt.getSailpointAccessRequestId(), updatedAttempt.getId());
            
            // 6. Schedule pooling job for this attempt
            jobSchedulerPort.schedulePoolingJob(updatedAttempt.getId());
            log.info("Pooling job scheduled for attempt: {}", updatedAttempt.getId());
            
            return accessRequestWithAttempt;
            
        } catch (IdentityProviderPort.IdentityClientException e) {
            // Sailpoint creation failed, but we keep the attempt in IN_PROGRESS status
            // The pooling job will retry later or admin can manually handle
            log.error("Failed to create access request in Sailpoint (attemptId: {})", 
                    savedAttempt.getId(), e);
            
            return savedAccessRequest;
        }
    }
    
    @Override
    public AccessRequest getById(UUID id) {
        log.debug("Getting access request by id: {}", id);
        
        // Get AccessRequest
        AccessRequest accessRequest = repositoryPort.findByIdOrThrow(id);
        
        // Load and set lastAttempt (transient field)
        AccessRequestAttempt lastAttempt = accessRequest.getLastAttempt();
        if (lastAttempt != null) {
            accessRequest.setLastAttempt(lastAttempt);
        }
        
        return accessRequest;
    }
    
    @Override
    public List<AccessRequest> getAll() {
        log.debug("Getting all access requests");
        return repositoryPort.findAll();
    }
}