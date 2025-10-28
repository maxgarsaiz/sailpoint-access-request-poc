package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.exception.EntityAlreadyLockedException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.port.in.ExecuteAccessRequestPoolingUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;
import com.maxgarsaiz.sailpoint.infrastructure.config.PollingConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.BackgroundJob;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for polling the status of access requests in the identity provider.
 * 
 * This service implements a background job that periodically checks if an access request
 * has been completed in the external identity provider system. It handles:
 * - Concurrent job execution prevention (via database locks)
 * - Status synchronization between local DB and identity provider
 * - Automatic retries for incomplete requests
 * - Error handling and state transitions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestPoolingService implements ExecuteAccessRequestPoolingUseCase {
    
    private final AccessRequestRepositoryPort repositoryPort;
    private final IdentityProviderPort identityProviderPort;
    private final PollingConfigurationProperties pollingConfig;
    
    /**
     * Main entry point for the pooling job.
     * Orchestrates the entire pooling workflow for a single access request.
     */
    @Override
    @Job(name = "Pooling Access Request - %0")
    @Transactional
    public void executePooling(UUID accessRequestId) {
        log.info("🔄 Starting pooling job for access request: {}", accessRequestId);
        
        // Step 1: Validate and get access request
        var accessRequestOpt = validateAndGetAccessRequest(accessRequestId);
        if (accessRequestOpt.isEmpty()) {
            return; // Validation failed, already logged
        }
        
        // Step 2: Ensure we can process this request (acquire lock if needed)
        AccessRequest accessRequest = ensureProcessingLock(accessRequestOpt.get());
        if (accessRequest == null) {
            return; // Couldn't acquire lock or already being processed
        }
        
        // Step 3: Check status in identity provider and update accordingly
        checkAndUpdateStatus(accessRequest);
    }
    
    /**
     * Validates and retrieves an access request by ID.
     * 
     * This method performs two critical validations:
     * 1. Checks if the access request exists in the database
     * 2. Validates that the request has a provider request ID (data integrity check)
     * 
     * If either validation fails, the method logs the appropriate error and marks
     * the request as FAILED if applicable.
     * 
     * @param accessRequestId the unique identifier of the access request
     * @return Optional containing the access request if all validations pass, empty otherwise
     */
    private Optional<AccessRequest> validateAndGetAccessRequest(UUID accessRequestId) {
        // Validation 1: Check if access request exists and lock processing
        var accessRequestOpt = repositoryPort.findById(accessRequestId);
        if (accessRequestOpt.isEmpty()) {
            log.error("❌ Access request not found: {}", accessRequestId);
            return Optional.empty(); // Don't retry - doesn't exist
        }

        var accessRequest = accessRequestOpt.get();
        log.debug("📋 Loaded access request with status: {}", accessRequest.getStatus());
        
        // Validation 2: Check if access request has provider request ID
        if (!accessRequest.hasProviderRequestId()) {
            handleMissingProviderRequestId(accessRequest);
            return Optional.empty(); // Don't retry - data integrity error
        }
        
        return Optional.of(accessRequest);
    }
    
    /**
     * Ensures we have the necessary lock to process this request.
     * Handles different status scenarios and acquires lock when needed.
     * Returns null if the request shouldn't be processed.
     */
    private AccessRequest ensureProcessingLock(AccessRequest accessRequest) {
        UUID requestId = accessRequest.getId();
        
        switch (accessRequest.getStatus()) {
            case PENDING, FAILED -> {
                // First execution or retry: need to acquire lock and transition to POOLING
                return acquireLock(requestId);
            }
            case POOLING -> {
                // Retry execution: already locked and in POOLING state
                log.debug("🔄 Access request {} already in POOLING status, continuing", requestId);
                return accessRequest;
            }
            case COMPLETED -> {
                log.info("✅ Access request {} already COMPLETED, skipping", requestId);
                return null; // Already done, no need to process
            }
            default -> {
                log.warn("⚠️  Access request {} has unexpected status: {}, skipping", 
                    requestId, accessRequest.getStatus());
                return null; // Unexpected state, skip processing
            }
        }
    }
    
    /**
     * Attempts to acquire a lock on the access request by transitioning it to POOLING.
     * Returns null if the lock couldn't be acquired (another job is processing it).
     */
    private AccessRequest acquireLock(UUID accessRequestId) {
        try {
            var lockedRequest = repositoryPort.findByIdAndTransitionToPooling(accessRequestId);
            log.info("🔒 Successfully acquired lock and transitioned request {} to POOLING", accessRequestId);
            return lockedRequest;
            
        } catch (EntityAlreadyLockedException e) {
            log.info("⏭️  Access request {} already being processed by another job, skipping", 
                accessRequestId);
            return null; // Another job is handling it
        }
    }
    
    /**
     * Handles the case where an access request doesn't have a provider request ID.
     * This is a data integrity error - marks the request as FAILED.
     */
    private void handleMissingProviderRequestId(AccessRequest accessRequest) {
        UUID requestId = accessRequest.getId();
        log.error("❌ Access request {} has no provider request ID. Data integrity error. Marking as FAILED.", 
            requestId);
        
        try {
            accessRequest.markAsFailed();
            repositoryPort.update(accessRequest);
            log.info("✅ Access request {} marked as FAILED due to missing provider ID", requestId);
        } catch (Exception e) {
            log.error("💥 Failed to mark request {} as FAILED", requestId, e);
        }
    }
    
    /**
     * Main business logic: checks the status in the identity provider and updates
     * the local access request accordingly.
     * 
     * Throws AccessRequestIncompleteException if the request is not yet completed,
     * signaling JobRunr to retry.
     */
    private void checkAndUpdateStatus(AccessRequest accessRequest) {
        UUID requestId = accessRequest.getId();
        String providerRequestId = accessRequest.getSailpointRequestId();
        
        log.info("📊 Checking status in identity provider for request: {} (Provider ID: {})", 
            requestId, providerRequestId);
        
        try {
            // Query identity provider for current status
            var requestStatusOpt = identityProviderPort.checkRequestStatus(providerRequestId);
            
            if (requestStatusOpt.isEmpty()) {
                handleStatusNotAvailable(requestId, providerRequestId);
                return; // Will retry via exception
            }
            
            // Process the received status
            var status = requestStatusOpt.get();
            log.info("📬 Received status from identity provider: {} for request: {}", 
                status.status(), requestId);
            
            processProviderStatus(accessRequest, status);
            
        } catch (AccessRequestIncompleteException e) {
            // Expected case: request not complete yet, will retry
            log.debug("⏳ Access request {} is incomplete, JobRunr will retry", requestId);
            throw e;
            
        } catch (Exception e) {
            // Unexpected error: log but let JobRunr retry
            // AccessRequestJobFilter will mark as FAILED after max retries
            log.error("💥 Unexpected error during pooling for request: {}", requestId, e);
            throw e;
        }
    }
    
    /**
     * Handles the case where the identity provider doesn't return a status.
     * Throws AccessRequestIncompleteException to trigger a retry.
     */
    private void handleStatusNotAvailable(UUID requestId, String providerRequestId) {
        log.warn("⏳ Identity provider status check returned empty for request: {}. Will retry in {}s", 
            providerRequestId, pollingConfig.getRetryIntervalSeconds());
        
        throw new AccessRequestIncompleteException(
            String.format("Identity provider status not available yet for request: %s", 
                providerRequestId));
    }
    
    /**
     * Processes the status received from the identity provider and updates
     * the local access request accordingly.
     */
    private void processProviderStatus(
        AccessRequest accessRequest, 
        IdentityProviderPort.RequestStatus status) {
        
        UUID requestId = accessRequest.getId();
        
        switch (status.status()) {
            case COMPLETED -> handleCompletedStatus(accessRequest, status);
            case FAILED -> handleFailedStatus(accessRequest, status);
            case PENDING, IN_PROGRESS -> handleInProgressStatus(requestId, status);
        }
    }
    
    /**
     * Handles a COMPLETED status from the identity provider.
     */
    private void handleCompletedStatus(
            AccessRequest accessRequest, 
            IdentityProviderPort.RequestStatus status) {
        
        accessRequest.markAsCompleted(status.requestId());
        repositoryPort.update(accessRequest);
        BackgroundJob.deleteRecurringJob(accessRequest.getId().toString());
        
        log.info("✅ Access request COMPLETED successfully. ID: {}, Provider ID: {}", 
            accessRequest.getId(), status.requestId());
    }
    
    /**
     * Handles a FAILED status from the identity provider.
     */
    private void handleFailedStatus(
            AccessRequest accessRequest, 
            IdentityProviderPort.RequestStatus status) {
        
        accessRequest.markAsFailed();
        repositoryPort.update(accessRequest);
        BackgroundJob.deleteRecurringJob(accessRequest.getId().toString());
        
        log.error("❌ Access request FAILED in identity provider. ID: {}, Reason: {}", 
            accessRequest.getId(), status.message());
    }
    
    /**
     * Handles PENDING or IN_PROGRESS status from the identity provider.
     * Throws AccessRequestIncompleteException to trigger a retry.
     */
    private void handleInProgressStatus(
            UUID requestId, 
            IdentityProviderPort.RequestStatus status) {
        
        log.info("⏳ Access request still processing in identity provider (status: {}). " +
            "Will retry in {}s. ID: {}", 
            status.status(), 
            pollingConfig.getRetryIntervalSeconds(), 
            requestId);
        
        throw new AccessRequestIncompleteException(
            String.format("Access request still being processed with status: %s", status.status()));
    }
    
    /**
     * Exception thrown to signal that an access request is not yet completed
     * in the identity provider and should be retried.
     * 
     * This exception is thrown when:
     * - The identity provider hasn't responded yet (status check returns empty)
     * - The request is still being processed (PENDING or IN_PROGRESS status)
     * 
     * JobRunr will catch this exception and automatically retry the job
     * based on the configured RetryFilter (max retries and retry interval).
     * 
     * This is NOT an error condition - it's the expected behavior during
     * the normal pooling lifecycle until the request completes.
     */
    public static class AccessRequestIncompleteException extends RuntimeException {
        
        public AccessRequestIncompleteException(String message) {
            super(message);
        }
        
        public AccessRequestIncompleteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
