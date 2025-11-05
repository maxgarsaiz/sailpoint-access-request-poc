package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.in.ExecuteAccessRequestPoolingUseCase;
import com.maxgarsaiz.sailpoint.domain.port.in.ManageAccessRequestAttemptStateUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestAttemptRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;

import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that executes the pooling job for access request attempts.
 * 
 * The job is re-scheduled by JobServerFilter.onProcessingSucceeded() until AccessRequest reaches final state.
 * Uses attemptId as job ID for JobRunr idempotency - no conflicts because previous job is SUCCEEDED.
 */
@Slf4j
@Service
public class AccessRequestPoolingService implements ExecuteAccessRequestPoolingUseCase {
    
    private final AccessRequestAttemptRepositoryPort attemptRepository;
    private final AccessRequestRepositoryPort accessRequestRepository;
    private final IdentityProviderPort identityProviderPort;
    private final ManageAccessRequestAttemptStateUseCase stateService;

    public AccessRequestPoolingService(
            AccessRequestAttemptRepositoryPort attemptRepository,
            AccessRequestRepositoryPort accessRequestRepository,
            IdentityProviderPort identityProviderPort,
            ManageAccessRequestAttemptStateUseCase stateService) {
        this.attemptRepository = attemptRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.identityProviderPort = identityProviderPort;
        this.stateService = stateService;
    }
    
    @Override
    @Job(name = "Pooling Access Request Attempt - %0")
    @Transactional
    public void executePooling(UUID attemptId) {
        log.info("🔄 Starting pooling job for attempt: {}", attemptId);
        
        // Step 1: Validate and get attempt
        AccessRequestAttempt attempt = validateAndGetAccessRequestAttempt(attemptId);
        if (attempt == null) {
            return; // Invalid attempt, exit
        }
        
        // Step 2: Check status in Sailpoint
        checkAndUpdateStatus(attempt, attemptId);
    }
    
    /**
     * Validates the attempt exists and has provider request ID.
     */
    private AccessRequestAttempt validateAndGetAccessRequestAttempt(UUID attemptId) {
        var attemptOpt = attemptRepository.findById(attemptId);
        
        if (attemptOpt.isEmpty()) {
            log.error("❌ Attempt {} not found, job will complete", attemptId);
            return null;
        }
        
        var attempt = attemptOpt.get();
        if (!attempt.hasProviderRequestId()) {
            log.error("❌ Access request {} has no sailpoint_request_id, job will complete",
                attempt.getAccessRequestId());
            return null;
        }

        return attempt;
    }
    
    /**
     * Checks status in Sailpoint and updates both AccessRequest and AccessRequestAttempt.
     */
    private void checkAndUpdateStatus(AccessRequestAttempt attempt, UUID attemptId) {
        UUID accessRequestId = attempt.getAccessRequestId();

        log.info("📊 Checking status in Sailpoint for request: {} (Attempt ID: {})",
            accessRequestId, attemptId);

        try {
            // Get AccessRequest from repository
            AccessRequest accessRequest = accessRequestRepository.findByIdOrThrow(accessRequestId);
            
            // Get status response from Sailpoint
            IdentityProviderPort.SailpointStatusResponse statusResponse = 
                    identityProviderPort.getRequestStatus(attempt);
            
            log.info("📬 Received status from Sailpoint: {} for attempt: {}", 
                statusResponse.status(), attemptId);
            
            // Update both AccessRequest and AccessRequestAttempt based on status
            updateAccessRequestAndAttempt(accessRequest, attempt, statusResponse, attemptId);
            
        } catch (Exception e) {
            log.error("💥 Error checking Sailpoint status for attempt: {}", attemptId, e);
            throw e; // Let JobRunr handle retries
        }
    }
    
    /**
     * Updates both AccessRequest and AccessRequestAttempt entities based on Sailpoint status.
     * Saves both entities to database.
     * JobServerFilter will decide if job should be re-scheduled after completion.
     */
    private void updateAccessRequestAndAttempt(
            AccessRequest accessRequest, 
            AccessRequestAttempt attempt, 
            IdentityProviderPort.SailpointStatusResponse statusResponse,
            UUID attemptId) {
        
        String sailpointStatus = statusResponse.status();
        
        switch (sailpointStatus.toUpperCase()) {
            case "COMPLETED" -> {
                log.info("✅ Sailpoint returned COMPLETED for attempt: {}", attemptId);
                
                // Update AccessRequestAttempt
                stateService.markAttemptAsCompleted(attemptId);
                
                // Update AccessRequest
                accessRequest.setStatus(AccessRequestStatus.PROCESSING_COMPLETED);
                accessRequest.setUpdatedAt(java.time.LocalDateTime.now());
                accessRequestRepository.update(accessRequest);
            }
            case "FAILED" -> {
                log.error("❌ Sailpoint returned FAILED for attempt: {}. Message: {}", 
                        attemptId, statusResponse.message());
                
                // Update AccessRequestAttempt
                stateService.markAttemptAsFailed(attemptId, statusResponse.message());
                
                // Update AccessRequest
                accessRequest.setStatus(AccessRequestStatus.PROCESSING_REQUIRES_ATTENTION);
                accessRequest.setUpdatedAt(java.time.LocalDateTime.now());
                accessRequestRepository.update(accessRequest);
            }
            case "IN_PROGRESS" -> {
                log.info("⏳ Sailpoint still processing attempt: {}. " +
                        "JobServerFilter will re-schedule after job completes.", attemptId);
                
                // Update AccessRequest status (might have changed)
                accessRequest.setStatus(AccessRequestStatus.PROCESSING_IN_PROGRESS);
                accessRequest.setUpdatedAt(java.time.LocalDateTime.now());
                accessRequestRepository.update(accessRequest);
                
                // Attempt remains IN_PROGRESS, no need to update
            }
            default -> {
                log.warn("⚠️ Unexpected status {} for attempt: {}", sailpointStatus, attemptId);
            }
        }
    }
}
