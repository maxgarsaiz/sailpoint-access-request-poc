package com.maxgarsaiz.sailpoint.infrastructure.config;

import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.filters.JobClientFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.FailedState;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JobRunr filter to handle Access Request state transitions based on job lifecycle events.
 * Automatically marks AccessRequest as FAILED when JobRunr exhausts all retries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessRequestJobFilter implements JobClientFilter, JobServerFilter {
    
    private final AccessRequestRepositoryPort accessRequestRepository;

    public void onCreating(Job job) {
        // Optional: log when pooling job is created
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.debug("Creating pooling job for access request: {}", accessRequestId);
        }
    }

    public void onCreated(Job job) {
        // Optional: additional logic when job is created
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.info("Pooling job created for access request: {}", accessRequestId);
        }
    }

    @Override
    public void onProcessing(Job job) {
        // Optional: log when job starts processing
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.debug("Processing pooling job for access request: {}", accessRequestId);
        }
    }

    @Override
    public void onProcessingSucceeded(Job job) {
        // Job succeeded - AccessRequest status is already updated in PoolingService
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.info("✅ Pooling job succeeded for access request: {}", accessRequestId);
        }
    }

    @Override
    public void onProcessingFailed(Job job, Exception e) {
        // Job failed but will still be retried - just log, don't update AccessRequest yet
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.warn("⚠️ Pooling job failed (will retry) for access request: {}. Error: {}", 
                accessRequestId, e.getMessage());
        }
    }

    @Override
    public void onFailedAfterRetries(Job job) {
        // THIS IS THE KEY METHOD - Job exhausted all retries
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            
            if (accessRequestId != null) {
                markAccessRequestAsFailed(accessRequestId, job);
            }
        }
    }

    private boolean isPoolingJob(Job job) {
        return job.getJobName() != null && 
               job.getJobName().startsWith("Pooling Access Request");
    }

    private UUID extractAccessRequestId(Job job) {
        try {
            // The first parameter of executePooling is the accessRequestId (UUID)
            JobParameter jobParameter = job.getJobDetails().getJobParameters().get(0);
            var object = jobParameter.getObject();
            if (object instanceof UUID uuid) {
                return uuid;
            } else if (object instanceof String str) {
                return UUID.fromString(str);
            } else {
                log.error("Unexpected job parameter type: {}", 
                    jobParameter.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to extract accessRequestId from job: {}", job.getId(), e);
            return null;
        }
    }

    private void markAccessRequestAsFailed(UUID accessRequestId, Job job) {
        try {
            Optional<FailedState> lastFailedState = job.getLastJobStateOfType(FailedState.class);
            String errorMessage = lastFailedState
                .map(s -> s.getException().getMessage())
                .orElse("Unknown error - JobRunr exhausted all retries");
            
            log.error("❌ JobRunr exhausted all retries for access request: {}. " +
                "Error: {}. Job ID: {}", accessRequestId, errorMessage, job.getId());
            
            AccessRequest accessRequest = accessRequestRepository.findByIdOrThrow(accessRequestId);
            
            // Only mark as failed if still in POOLING state
            if (accessRequest.getStatus() == AccessRequestStatus.POOLING) {
                accessRequest.markAsFailed();
                accessRequestRepository.update(accessRequest);
                
                log.error("🔴 Access request {} marked as FAILED after exhausting all retries. " +
                    "Last error: {}", accessRequestId, errorMessage);
            } else {
                log.info("Access request {} is already in status: {}, not marking as FAILED", 
                    accessRequestId, accessRequest.getStatus());
            }
            
        } catch (EntityNotFoundException e) {
            log.error("Access request {} not found when trying to mark as FAILED", 
                accessRequestId, e);
        } catch (Exception e) {
            log.error("Failed to mark access request {} as FAILED", accessRequestId, e);
        }
    }
}
