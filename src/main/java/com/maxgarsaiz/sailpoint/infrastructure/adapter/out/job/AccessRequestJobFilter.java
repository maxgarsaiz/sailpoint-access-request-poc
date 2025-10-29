package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import com.maxgarsaiz.sailpoint.domain.port.in.ManageAccessRequestAttemptStateUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JobRunr filter to handle pooling job lifecycle events.
 * 
 * When a job exhausts all retries (onFailedAfterRetries):
 * - Deletes the recurring job
 * - Keeps attempt status as IN_PROGRESS for manual retry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessRequestJobFilter implements JobServerFilter {
    
    private final ManageAccessRequestAttemptStateUseCase stateService;

    @Override
    public void onProcessing(Job job) {
        if (isPoolingJob(job)) {
            UUID attemptId = extractAttemptId(job);
            log.debug("Processing pooling job for attempt: {}", attemptId);
        }
    }

    @Override
    public void onProcessingSucceeded(Job job) {
        if (isPoolingJob(job)) {
            UUID attemptId = extractAttemptId(job);
            log.info("✅ Pooling job succeeded for attempt: {}", attemptId);
        }
    }

    @Override
    public void onProcessingFailed(Job job, Exception e) {
        if (isPoolingJob(job)) {
            UUID attemptId = extractAttemptId(job);
            log.warn("⚠️ Pooling job failed (will retry) for attempt: {}. Error: {}", 
                attemptId, e.getMessage());
        }
    }

    @Override
    public void onFailedAfterRetries(Job job) {
        if (isPoolingJob(job)) {
            UUID attemptId = extractAttemptId(job);
            
            if (attemptId != null) {
                log.error("🔥 Job exhausted all retries for attempt: {}. " +
                    "Deleting job but keeping status IN_PROGRESS for manual retry.", attemptId);
                
                // Delegate to state service
                stateService.handleJobExhaustedRetries(attemptId);
            }
        }
    }

    private boolean isPoolingJob(Job job) {
        return job.getJobName() != null && 
               job.getJobName().startsWith("Pooling Access Request Attempt");
    }

    private UUID extractAttemptId(Job job) {
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
            log.error("Failed to extract attemptId from job: {}", job.getId(), e);
            return null;
        }
    }
}
