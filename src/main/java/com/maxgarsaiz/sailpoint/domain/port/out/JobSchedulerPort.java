package com.maxgarsaiz.sailpoint.domain.port.out;

import java.util.UUID;

public interface JobSchedulerPort {
    
    /**
     * Schedules a recurring pooling job for an access request attempt.
     * The job will execute periodically until explicitly deleted.
     * 
     * Job ID format: attemptId (no prefix)
     * 
     * @param attemptId the ID of the attempt to poll
     */
    void schedulePoolingJob(UUID attemptId);
    
    /**
     * Deletes a recurring pooling job for an attempt.
     * Should be called when:
     * - Attempt reaches final state (COMPLETED or FAILED)
     * - Job exhausts all retries
     * 
     * @param attemptId the ID of the attempt
     */
    void deleteRecurringJob(UUID attemptId);
}
