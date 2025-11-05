package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.infrastructure.config.PollingConfigurationProperties;

import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.filters.ElectStateFilter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.SucceededState;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * JobRunr ElectStateFilter that executes BEFORE state transitions.
 * 
 * Key responsibilities:
 * - Intercepts SUCCEEDED state transition
 * - If AccessRequest is not in final state, re-schedules the job using job.scheduleAt()
 * - Provides logging for job lifecycle events
 * 
 * Why ElectStateFilter instead of JobServerFilter or ApplyStateFilter?
 * - ElectStateFilter.onStateElection() executes BEFORE the state change
 * - We can modify the job's next state directly
 * - Using job.scheduleAt() re-uses the SAME job ID (true idempotency)
 * - No need to delete and create new jobs
 */
@Slf4j
@Component
public class AccessRequestJobFilter implements ElectStateFilter {
    
    private final AccessRequestRepositoryPort accessRequestRepository;
    private final PollingConfigurationProperties pollingConfig;

    public AccessRequestJobFilter(
            AccessRequestRepositoryPort accessRequestRepository,
            PollingConfigurationProperties pollingConfig) {
        this.accessRequestRepository = accessRequestRepository;
        this.pollingConfig = pollingConfig;
    }

    /**
     * Called BEFORE a state transition is applied.
     * This is the perfect place to intercept state changes and re-schedule jobs.
     * 
     * @param job The job that is about to change state
     * @param newState The new state that will be applied
     */
    @Override
    public void onStateElection(Job job, JobState newState) {
        // Only act when job is about to succeed
        if (!(newState instanceof SucceededState)) {
            return;
        }
        
        if (!isPoolingJob(job)) {
            return;
        }
        
        UUID attemptId = extractAttemptId(job);
        if (attemptId == null) {
            return;
        }
        
        log.info("✅ Pooling job about to succeed for attempt: {}", attemptId);
        
        try {
            // Check if AccessRequest is in final state
            AccessRequest accessRequest = accessRequestRepository.findByAttemptId(attemptId)
                    .orElse(null);
            
            if (accessRequest == null) {
                log.warn("AccessRequest not found for attempt: {}. Job will complete normally.", attemptId);
                return;
            }
            
            AccessRequestStatus status = accessRequest.getStatus();
            
            // If not in final state, re-schedule the SAME job
            if (!isFinalState(status)) {
                log.info("AccessRequest {} is not in final state ({}). Re-scheduling same job.",
                        accessRequest.getId(), status);
                
                // Use job.scheduleAt() to re-use the SAME job with SAME ID
                Instant nextRun = Instant.now().plus(pollingConfig.getPoolingInterval());
                job.scheduleAt(nextRun, "Access request not in final state: " + status);
                
                log.info("✅ Same job re-scheduled for attempt: {} at {}", 
                        attemptId, nextRun);
            } else {
                log.info("🎉 AccessRequest {} reached final state ({}). Pooling complete.",
                        accessRequest.getId(), status);
            }
            
        } catch (Exception e) {
            log.error("❌ Error checking AccessRequest status for attempt: {}. " +
                    "Job will complete normally.", attemptId, e);
        }
    }
    
    private boolean isFinalState(AccessRequestStatus status) {
        return status == AccessRequestStatus.PROCESSING_COMPLETED ||
               status == AccessRequestStatus.PROCESSING_REQUIRES_ATTENTION;
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
