package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;

import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.StorageProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JobRunr filter to handle Access Request state transitions based on job lifecycle events.
 * Automatically marks AccessRequest as FAILED when JobRunr exhausts all retries and
 * deletes the job to allow new retry attempts.
 */
@Slf4j
@Component
public class AccessRequestJobFilter implements JobServerFilter {
    
    private final AccessRequestRepositoryPort accessRequestRepository;
    // ✅ Use constructor injection with @Lazy to break circular dependency
    public AccessRequestJobFilter(
            AccessRequestRepositoryPort accessRequestRepository,
            @Lazy StorageProvider storageProvider) {
        this.accessRequestRepository = accessRequestRepository;
    }

    @Override
    public void onProcessing(Job job) {
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.debug("Processing pooling job for access request: {}", accessRequestId);
        }
    }

    @Override
    public void onProcessingSucceeded(Job job) {
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.info("✅ Pooling job succeeded for access request: {}", accessRequestId);
        }
    }

    @Override
    public void onProcessingFailed(Job job, Exception e) {
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            log.warn("⚠️ Pooling job failed (will retry) for access request: {}. Error: {}", 
                accessRequestId, e.getMessage());
        }
    }

    @Override
    public void onFailedAfterRetries(Job job) {
        if (isPoolingJob(job)) {
            UUID accessRequestId = extractAccessRequestId(job);
            
            if (accessRequestId != null) {
                markAccessRequestAsFailedAndDeleteJob(accessRequestId, job);
            }
        }
    }

    private boolean isPoolingJob(Job job) {
        return job.getJobName() != null && 
               job.getJobName().startsWith("Pooling Access Request");
    }

    private UUID extractAccessRequestId(Job job) {
        try {
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

    private void markAccessRequestAsFailedAndDeleteJob(UUID accessRequestId, Job job) {
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
                
                // ✅ DELETE the job from JobRunr to allow new retries
                deleteJobFromStorage(accessRequestId.toString());
                
            } else {
                log.info("Access request {} is already in status: {}, not marking as FAILED", 
                    accessRequestId, accessRequest.getStatus());
                
                // Still delete the job even if status changed
                deleteJobFromStorage(accessRequestId.toString());
            }
            
        } catch (EntityNotFoundException e) {
            log.error("Access request {} not found when trying to mark as FAILED", 
                accessRequestId, e);
            // Delete the job anyway
            deleteJobFromStorage(accessRequestId.toString());
            
        } catch (Exception e) {
            log.error("Failed to mark access request {} as FAILED", accessRequestId, e);
            // Try to delete the job anyway to avoid blocking future retries
            deleteJobFromStorage(accessRequestId.toString());
        }
    }
    
    /**
     * Deletes a job from JobRunr storage permanently.
     * This allows new jobs with the same name to be enqueued.
     */
    private void deleteJobFromStorage(String accessRequestId) {
        try {
            BackgroundJob.deleteRecurringJob(accessRequestId);
            log.info("🗑️  Deleted job {} from JobRunr storage to allow new retry attempts", accessRequestId);
        } catch (Exception e) {
            log.error("💥 Failed to delete job {} from JobRunr storage. " +
                "This may prevent future retries for the same access request.", accessRequestId, e);
        }
    }

}
