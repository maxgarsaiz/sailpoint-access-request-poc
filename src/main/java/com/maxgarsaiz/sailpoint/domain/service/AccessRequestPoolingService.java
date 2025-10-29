package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.port.in.ExecuteAccessRequestPoolingUseCase;
import com.maxgarsaiz.sailpoint.domain.port.in.ManageAccessRequestAttemptStateUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestAttemptRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that executes the recurring pooling job for access request attempts.
 * 
 * This job runs indefinitely until:
 * 1. Sailpoint returns COMPLETED or FAILED
 * 2. Job exhausts all retries (handled by filter)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestPoolingService implements ExecuteAccessRequestPoolingUseCase {
    
    private final AccessRequestAttemptRepositoryPort attemptRepository;
    private final IdentityProviderPort identityProviderPort;
    private final ManageAccessRequestAttemptStateUseCase stateService;
    private final JobSchedulerPort jobSchedulerPort;
    
    @Override
    @Job(name = "Pooling Access Request Attempt - %0")
    @Transactional
    public void executePooling(UUID attemptId) {
        log.info("🔄 Starting pooling job for attempt: {}", attemptId);
        
        // Step 1: Validate and get attempt
        AccessRequestAttempt attempt = validateAndGetAccessRequestAttempt(attemptId);
        if (attempt == null) {
            return; // Job deleted, exit
        }
        
        // Step 2: Check status in Sailpoint
        checkAndUpdateStatus(attempt);
    }
    
    /**
     * Validates the attempt exists, if not deletes the job.
     */
    private AccessRequestAttempt validateAndGetAccessRequestAttempt(UUID attemptId) {
        var attemptOpt = attemptRepository.findById(attemptId);
        
        if (attemptOpt.isEmpty()) {
            log.error("❌ Attempt {} not found, deleting job", attemptId);
            jobSchedulerPort.deleteRecurringJob(attemptId);
            return null;
        }
        var attempt = attemptOpt.get();
        if (!attempt.hasProviderRequestId()) {
            log.error("❌ Access request {} has no sailpoint_request_id, deleting job",
                attempt.getAccessRequestId());
            jobSchedulerPort.deleteRecurringJob(attemptId);
            return null;
        }

        return attempt;
    }
    
    /**
     * Checks status in Sailpoint and updates attempt accordingly.
     */
    private void checkAndUpdateStatus(AccessRequestAttempt attempt) {
        UUID attemptId = attempt.getId();
        String sailpointRequestId = attempt.getSailpointRequestId();

        log.info("📊 Checking status in Sailpoint for request: {} (Sailpoint ID: {}, Attempt ID: {})",
            attempt.getAccessRequestId(), sailpointRequestId, attemptId);

        try {
            IdentityProviderPort.AccessRequestResponse response = 
                identityProviderPort.getRequestStatus(sailpointRequestId);
            
            log.info("📬 Received status from Sailpoint: {} for attempt: {}", 
                response.status(), attemptId);
            
            processProviderStatus(attemptId, response);
            
        } catch (Exception e) {
            log.error("💥 Error checking Sailpoint status for attempt: {}", attemptId, e);
            throw e; // Let JobRunr handle retries
        }
    }
    
    /**
     * Processes the status from Sailpoint and updates attempt state.
     */
    private void processProviderStatus(
        UUID attemptId,
        IdentityProviderPort.AccessRequestResponse response) {
        
        switch (response.status()) {
            case COMPLETED -> {
                log.info("✅ Sailpoint returned COMPLETED for attempt: {}", attemptId);
                stateService.markAttemptAsCompleted(attemptId);
            }
            case FAILED -> {
                log.error("❌ Sailpoint returned FAILED for attempt: {}. Message: {}", 
                    attemptId, response.message());
                stateService.markAttemptAsFailed(attemptId, response.message());
            }
            case IN_PROGRESS -> {
                log.info("⏳ Sailpoint still processing attempt: {}. Job will continue.", attemptId);
                // Job continues running
            }
        }
    }
}
