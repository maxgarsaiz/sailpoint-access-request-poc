package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.in.CreateAccessRequestUseCase;
import com.maxgarsaiz.sailpoint.domain.port.in.GetAccessRequestUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestService implements CreateAccessRequestUseCase, GetAccessRequestUseCase {
    
    private final AccessRequestRepositoryPort repositoryPort;
    private final JobSchedulerPort jobSchedulerPort;
    private final IdentityProviderPort identityProviderPort;
    
    @Override
    @Transactional
    public AccessRequest createAccessRequest(CreateAccessRequestCommand command) {
        log.info("Creating access request for user: {}", command.userId());
        
        // 1. Create domain model
        AccessRequest accessRequest = AccessRequest.builder()
            .id(UUID.randomUUID())
            .userId(command.userId())
            .justification(command.justification())
            .status(AccessRequestStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        // 2. Save to database
        AccessRequest saved = repositoryPort.save(accessRequest);
        log.info("Access request saved to database with id: {}", saved.getId());
        
        try {
            // 3. Create in Sailpoint (adapter handles all Feign exceptions)
            AccessRequest withSailpointId = identityProviderPort.createAccessRequest(saved);
            
            // 4. Update with Sailpoint request ID
            AccessRequest updated = repositoryPort.update(withSailpointId);
            log.info("Access request updated with Sailpoint ID: {}", updated.getSailpointRequestId());
            
            // 5. Schedule pooling job to check status
            jobSchedulerPort.schedulePoolingJob(updated.getId());
            log.info("Pooling job scheduled for access request: {}", updated.getId());
            
            return updated;
            
        } catch (IdentityProviderPort.IdentityClientException e) {
            // Sailpoint creation failed, but we keep the request in PENDING status
            // The pooling job will retry later or admin can manually handle
            log.error("Failed to create access request in Sailpoint, keeping in PENDING status. ID: {}", 
                saved.getId(), e);
            
            // Still schedule pooling job - it will attempt to create if sailpointRequestId is null
            jobSchedulerPort.schedulePoolingJob(saved.getId());
            
            return saved;
        }
    }
    
    @Override
    public AccessRequest getById(UUID id) {
        log.debug("Getting access request by id: {}", id);
        // Now it throws AccessRequestNotFoundException automatically
        return repositoryPort.findByIdOrThrow(id);
    }
    
    @Override
    public List<AccessRequest> getAll() {
        log.debug("Getting all access requests");
        return repositoryPort.findByStatusIn(
            List.of(AccessRequestStatus.PENDING, AccessRequestStatus.PROCESSING_IN_PROGRESS, 
                    AccessRequestStatus.PROCESSING_REQUIRES_ATTENTION, AccessRequestStatus.PROCESSING_COMPLETED)
        );
    }
}